// NewCloud 站点管理模块
// 管理仓库站点和本地站点，支持点击站点获取分类和影片

(function(){
  var currentTab = 'warehouse'; // 'warehouse' | 'local'
  var currentSite = null;

  // ===== 渲染站点面板 =====
  window.renderSitePanel = function() {
    var grid = document.getElementById('siteGrid');
    if (!grid) return;

    if (currentTab === 'warehouse') {
      renderWarehouseSites(grid);
    } else {
      renderLocalSites(grid);
    }
  };

  // 兼容旧函数名
  window.renderRepoPanel = window.renderSitePanel;

  function renderWarehouseSites(grid) {
    if (!window.NCDB) {
      grid.innerHTML = '<div class="nc-repo-empty">数据库未初始化</div>';
      return;
    }

    NCDB.getAllSites().then(function(sites) {
      var warehouseSites = (sites || []).filter(function(s) { return s.sourceType === 'warehouse'; });
      
      if (!warehouseSites.length) {
        grid.innerHTML = '<div class="nc-repo-empty">暂无仓库站点，请先在仓库管理中添加仓库</div>';
        return;
      }

      grid.innerHTML = warehouseSites.map(function(s, i) {
        var typeTag = s.type === 'json' ? 'JSON' : (s.type === 'xml' ? 'XML' : (s.type === 'js' ? 'JS' : 'BILI'));
        var searchTag = (s.searchable || s.quickSearch) ? '可搜索' : '不可搜索';
        return '<div class="tv-site-item" onclick="selectSite(' + s.id + ')">' +
          '<div class="tv-site-icon">' + (i + 1) + '</div>' +
          '<div class="tv-site-info">' +
            '<span class="tv-site-name">' + s.name + '</span>' +
            '<span class="tv-site-desc">' + typeTag + ' · ' + searchTag + '</span>' +
            '<span class="tv-site-desc" style="font-size:9px;color:#556677;max-width:90%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + (s.api || '') + '</span>' +
          '</div>' +
        '</div>';
      }).join('');
    });
  }

  function renderLocalSites(grid) {
    if (!window.NCDB) {
      grid.innerHTML = '<div class="nc-repo-empty">数据库未初始化</div>';
      return;
    }

    NCDB.getLocalSites().then(function(sites) {
      if (!sites || !sites.length) {
        grid.innerHTML = '<div class="nc-repo-empty">暂无本地站点</div>';
        return;
      }

      grid.innerHTML = sites.map(function(s, i) {
        return '<div class="tv-site-item" onclick="selectSite(' + s.id + ')">' +
          '<div class="tv-site-icon">' + (i + 1) + '</div>' +
          '<div class="tv-site-info">' +
            '<span class="tv-site-name">' + s.name + '</span>' +
            '<span class="tv-site-desc">' + (s.api || '') + '</span>' +
          '</div>' +
          '<button class="nc-source-del" onclick="deleteLocalSite(' + s.id + ',event)">删</button>' +
        '</div>';
      }).join('') +
      '<div style="margin-top:16px;padding-top:12px;border-top:1px solid rgba(255,255,255,.06)">' +
        '<button onclick="showAddLocalSiteForm()" style="width:100%;padding:12px;border:1px dashed rgba(33,150,243,.3);background:rgba(33,150,243,.05);color:#4aa8ff;border-radius:10px;font-size:13px">+ 添加本地CMS站点</button>' +
      '</div>';
    });
  }

  window.switchSiteTab = function(tab) {
    currentTab = tab;
    var tabs = document.querySelectorAll('.site-tab-btn');
    for (var i = 0; i < tabs.length; i++) {
      tabs[i].classList.toggle('active', tabs[i].getAttribute('data-tab') === tab);
    }
    renderSitePanel();
  };

  // ===== 选择站点 =====
  window.selectSite = function(siteId) {
    if (!window.NCDB) return;
    
    NCDB.getAllSites().then(function(sites) {
      var site = null;
      for (var i = 0; i < (sites || []).length; i++) {
        if (sites[i].id === siteId) { site = sites[i]; break; }
      }
      if (!site) { alert('站点不存在'); return; }

      currentSite = site;
      hideSitePanel();
      setMovieStatus('正在获取 ' + site.name + ' 的分类...', false);

      // 检测API格式
      if (window.detectApiFormat) {
        window.detectApiFormat(site).then(function(format) {
          site.apiType = format;
          fetchSiteCategories(site);
        });
      } else {
        fetchSiteCategories(site);
      }
    });
  };

  function fetchSiteCategories(site) {
    if (!site.api) {
      setMovieStatus('站点没有API地址', false);
      return;
    }

    var apiUrl = site.api;
    // 处理不同类型的API
    if (site.type === 'json' || !site.type) {
      // JSON API: 尝试 ac=list
      fetchJsonSmart(apiUrl + '?ac=list').then(function(data) {
        if (data && data.class && data.class.length) {
          site.categories = data.class;
          NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: data.class })).then(function() {
            onCategoriesLoaded(site);
          });
        } else {
          // 尝试 ac.detail
          fetchJsonSmart(apiUrl + '?ac.detail').then(function(data2) {
            if (data2 && data2.class && data2.class.length) {
              site.categories = data2.class;
              NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: data2.class })).then(function() {
                onCategoriesLoaded(site);
              });
            } else {
              // 使用默认分类
              useDefaultCategories(site);
            }
          }).catch(function() { useDefaultCategories(site); });
        }
      }).catch(function() { useDefaultCategories(site); });
    } else if (site.type === 'xml') {
      // XML API
      fetchTextSmart(apiUrl + '?ac=list').then(function(text) {
        var categories = parseXmlCategories(text);
        if (categories.length) {
          site.categories = categories;
          NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: categories })).then(function() {
            onCategoriesLoaded(site);
          });
        } else {
          useDefaultCategories(site);
        }
      }).catch(function() { useDefaultCategories(site); });
    } else {
      useDefaultCategories(site);
    }
  }

  function useDefaultCategories(site) {
    site.categories = [
      {type_id:'1',type_name:'电影'},
      {type_id:'2',type_name:'连续剧'},
      {type_id:'3',type_name:'综艺'},
      {type_id:'4',type_name:'动漫'}
    ];
    onCategoriesLoaded(site);
  }

  function onCategoriesLoaded(site) {
    movieConfig.site = {
      key: site.key || site.name,
      name: site.name,
      api: site.api,
      type: site.type || 'json',
      categories: site.categories || [],
      ext: site.ext || {},
      timeout: site.timeout || 10
    };
    
    // 更新UI
    var nameEl = document.getElementById('tvSourceName');
    if (nameEl) nameEl.textContent = site.name;
    
    setMovieStatus('已选择: ' + site.name, true);
    
    // 切换到推荐页并加载数据
    movieState.cat = '推荐';
    movieState.usingRemote = true;
    
    if (window.updateDbRenderCats) {
      window.updateDbRenderCats();
    }
    
    renderMovieHome();
  }

  // ===== 添加本地站点 =====
  window.showAddLocalSiteForm = function() {
    var name = prompt('请输入站点名称：', '自定义站点');
    if (!name) return;
    var url = prompt('请输入CMS接口地址：', 'http://');
    if (!url) return;

    // 智能检测
    var type = detectUrlType ? detectUrlType(url) : 'cms';
    if (type === 'tvbox') {
      alert('检测到这是TVBox配置地址，请在「仓库管理」中添加。\nCMS采集接口（如 /api.php/provide/vod）请在本站添加。');
      return;
    }

    var site = {
      name: name,
      api: url,
      type: 'json',
      sourceType: 'local'
    };

    if (window.NCDB) {
      NCDB.saveSiteConfig(null, site).then(function() {
        alert('站点添加成功');
        renderSitePanel();
      }).catch(function(e) {
        alert('添加失败: ' + e);
      });
    }
  };

  window.deleteLocalSite = function(id, e) {
    if (e) e.stopPropagation();
    if (!confirm('确定删除此站点？')) return;
    if (window.NCDB) {
      NCDB.deleteSiteConfig(id).then(function() {
        renderSitePanel();
      });
    }
  };

  // ===== API格式检测 =====
  window.detectApiFormat = function(site) {
    return new Promise(function(resolve) {
      if (!site.api) { resolve('json'); return; }
      
      var api = site.api.toLowerCase();
      if (api.indexOf('.js') >= 0) { resolve('js'); return; }
      if (api.indexOf('bilibili') >= 0 || api.indexOf('bilivd') >= 0) { resolve('bili'); return; }
      
      // 尝试请求检测
      fetch(site.api + '?ac=list', {cache: 'no-store'}).then(function(r) {
        return r.json();
      }).then(function(data) {
        if (data && data.class && data.list) {
          resolve('json');
        } else {
          resolve('json');
        }
      }).catch(function() {
        resolve('json');
      });
    });
  };

  // ===== 工具函数 =====
  function fetchJsonSmart(url) {
    return fetch(url, {cache: 'no-store'}).then(function(r) {
      if (!r.ok) throw 'HTTP ' + r.status;
      return r.json();
    });
  }

  function fetchTextSmart(url) {
    return fetch(url, {cache: 'no-store'}).then(function(r) {
      if (!r.ok) throw 'HTTP ' + r.status;
      return r.text();
    });
  }

  function parseXmlCategories(text) {
    var categories = [];
    try {
      var parser = new DOMParser();
      var xml = parser.parseFromString(text, 'text/xml');
      var types = xml.getElementsByTagName('ty');
      for (var i = 0; i < types.length; i++) {
        categories.push({
          type_id: types[i].getAttribute('id') || types[i].getAttribute('type_id') || i,
          type_name: (types[i].textContent || '').trim()
        });
      }
    } catch(e) {}
    return categories;
  }

  // 初始化
  renderSitePanel();
})();
