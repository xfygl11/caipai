// NewCloud 页面模块：页面注册/路由、分类页推荐兜底、TV UI渲染

  // =============================================
  // 1. 页面注册系统
  // =============================================
  window.NCPages = window.NCPages || {};

  window.NCPage = {
    register: function(name, module) {
      window.NCPages[name] = module;
    },
    call: function(name, method) {
      var page = window.NCPages[name];
      if (page && page[method]) return page[method].apply(page, Array.prototype.slice.call(arguments, 2));
      console.warn('NCPages: ' + name + '.' + method + ' not found');
    }
  };

  // =============================================
  // 2. 分类页推荐兜底逻辑
  // =============================================
  // 当用户切换到"电影"、"连续剧"、"综艺"等分类时，
  // 如果该分类没有数据，先显示推荐内容，然后显示"暂无影片"提示。
  // 通过 monkey-patch renderMovieHome 实现。

  var _ncPageFallbackInited = false;
  var _ncPageFallbackPollTimer = null;

  function installCategoryFallback() {
    if (_ncPageFallbackInited) return;

    var _orig = window.renderMovieHome;
    if (!_orig) {
      // 等待模块加载完成 - 添加超时保护，最多等待5秒
      if (_ncPageFallbackPollTimer) clearTimeout(_ncPageFallbackPollTimer);
      _ncPageFallbackPollTimer = setTimeout(function(){
        console.warn('nc-page: renderMovieHome 加载超时，跳过兜底逻辑');
      }, 5000);
      _ncPageFallbackPollTimer = setTimeout(function(){
        if(!_ncPageFallbackInited) installCategoryFallback();
      }, 50);
      return;
    }
    
    if (_ncPageFallbackPollTimer) clearTimeout(_ncPageFallbackPollTimer);

    _ncPageFallbackInited = true;

    // 始终基于最新的 renderMovieHome（可能已经被 app.js 中的 TV UI override 替换）
    window.renderMovieHome = function() {
      // 先调用原始渲染
      _orig.call(this);

      // 分类页推荐兜底：当当前分类不是"推荐"且使用远程源时
      if (movieState.cat && movieState.cat !== '推荐' && movieState.usingRemote) {
        var kw = (movieState.keyword || '').trim().toLowerCase();
        var list = MOVIE_DATA.filter(function(v) {
          return (movieState.cat === '推荐' || v.cat === movieState.cat) &&
                 (!kw || String(v.title).toLowerCase().indexOf(kw) >= 0 || String(v.type).toLowerCase().indexOf(kw) >= 0);
        });

        if (!list.length) {
          // 当前分类没有数据，尝试用推荐数据作为兜底
          var recommendCache = movieState.listCache && movieState.listCache['推荐_1'];
          if (recommendCache && recommendCache.length) {
            var grid = document.getElementById('tvGrid');
            var secName = document.getElementById('tvSectionName');
            if (grid) {
              if (secName) secName.textContent = movieState.cat + '（推荐）';
              var fav = lsGet('movie_favs');
              window.MOVIE_INDEX = window.MOVIE_INDEX || {};
              grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;color:#8899aa;padding:8px;font-size:13px">' +
                movieState.cat + ' 暂无影片，以下为推荐内容</div>' +
                recommendCache.slice(0, 20).map(function(v) {
                  window.MOVIE_INDEX[String(v.id)] = v;
                  var on = fav.indexOf(v.id) >= 0;
                  var img = v.pic ? "background-image:url('" + v.pic + "')" : '';
                  var year = v.year || (v.title && v.title.match(/\d{4}/) ? v.title.match(/\d{4}/)[0] : '');
                  var hd = v.quality || v.tag || 'HD';
                  return '<div class="tv-card" onclick="moviePlay(\'' + v.id + '\')">' +
                    '<div class="tv-poster" style="' + img + '">' +
                    '<span class="tv-year-tag">' + year + '</span>' +
                    '<span class="tv-hd-tag">' + hd + '</span>' +
                    '<div class="tv-card-title">' + v.title + '</div>' +
                    '</div>' +
                    '<div class="tv-card-info">' + (v.type || v.cat || '影视') + (on ? ' · 已收藏' : '') + '</div>' +
                    '</div>';
                }).join('');
            }
          }
        }
      }
    };
  }

  // =============================================
  // 3. 初始化入口
  // =============================================
  // 等待 app.js 中定义了 renderMovieHome 后再安装兜底逻辑
  // 使用双重保险：DOMContentLoaded + 立即执行
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setTimeout(installCategoryFallback, 100);
    });
  } else {
    setTimeout(installCategoryFallback, 100);
  }
