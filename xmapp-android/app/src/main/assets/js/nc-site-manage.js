// NewCloud 站点管理模块
// 管理仓库站点和本地站点，支持点击站点获取分类和影片

// Inline CSP_API_MAP for T4 site resolution (also defined in nc-movie-engine.js)
var INLINE_CSP_API_MAP = {
  'csp_NewCzGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_NewCz':'https://jszyapi.com/api.php/provide/vod',
  'csp_NmyswvGuard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_JpysGuard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_T4Guard':'https://subocaiji.com/api.php/provide/vod',
  'csp_AppTTGuard':'https://www.hongniuzy2.com/api.php/provide/vod',
  'csp_AppSxGuard':'https://apiyutu.com/api.php/provide/vod',
  'csp_AppgzGuard':'https://suoniapi.com/api.php/provide/vod',
  'csp_AueteGuard':'https://api.wujinapi.com/api.php/provide/vod',
  'csp_SixVGuard':'https://sdzyapi.com/api.php/provide/vod',
  'csp_BttwooGuard':'https://api.apibdzy.com/api.php/provide/vod',
  'csp_WoGGGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_Dm84Guard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_Anime1Guard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_YCyzGuard':'https://subocaiji.com/api.php/provide/vod',
  'csp_YGPGuard':'https://www.hongniuzy2.com/api.php/provide/vod',
  'csp_MusicGuard':'https://apiyutu.com/api.php/provide/vod',
  'csp_BiliGuard':'https://suoniapi.com/api.php/provide/vod',
  'csp_KanqiuGuard':'https://api.wujinapi.com/api.php/provide/vod',
  'csp_DoubaoGuard':'https://sdzyapi.com/api.php/provide/vod',
  'csp_LiveGzGuard':'https://api.apibdzy.com/api.php/provide/vod',
  'csp_AllliveGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_JPJGuard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_MyDriveGuard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_DouDouGuard':'https://subocaiji.com/api.php/provide/vod',
  'csp_SeedhubGuard':'https://www.hongniuzy2.com/api.php/provide/vod',
  'csp_S_zpsGuard':'https://apiyutu.com/api.php/provide/vod',
  'csp_KkSsGuard':'https://suoniapi.com/api.php/provide/vod',
  'csp_UuSsGuard':'https://api.wujinapi.com/api.php/provide/vod',
  'csp_YpanSoGuard':'https://sdzyapi.com/api.php/provide/vod',
  'csp_BpanSoGuard':'https://api.apibdzy.com/api.php/provide/vod',
  'csp_PushGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_FirstAidGuard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_Tingshu275Guard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_XPathGuard':'https://subocaiji.com/api.php/provide/vod',
  'csp_LibvioGuard':'https://www.hongniuzy2.com/api.php/provide/vod'
};

(function(){
  var currentTab = 'warehouse'; // 'warehouse' | 'local'
  var currentSite = null;

  // ===== 渲染站点面板 =====
  // ===== 站点管理 =====
  function escapeHtml(str) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
  }

  window.renderSitePanel = function() {
    console.log('[site] renderSitePanel called');
    var grid = document.getElementById('siteGrid');
    console.log('[site] siteGrid element:', !!grid);
    if (!grid) {
      console.log('[site] siteGrid NOT FOUND!');
      return;
    }

    if (currentTab === 'warehouse') {
      console.log('[site] rendering warehouse sites, tab=', currentTab);
      renderWarehouseSites(grid);
    } else {
      console.log('[site] rendering local sites, tab=', currentTab);
      renderLocalSites(grid);
    }
  };

  // 兼容旧函数名 - 不再覆盖 renderRepoPanel，避免与 nc-repo.js 冲突
  // window.renderRepoPanel = window.renderSitePanel;

  function renderWarehouseSites(grid) {
    if (!window.NCDB) {
      grid.innerHTML = '<div class="nc-repo-empty">数据库未初始化</div>';
      return;
    }

    NCDB.getAllSites().then(function(sites) {
      var warehouseSites = (sites || []).filter(function(s) { return s.sourceType === 'warehouse' && s.warehouseId === window._currentWarehouseId; });
      
      if (!warehouseSites.length) {
        grid.innerHTML = '<div class="nc-repo-empty">暂无仓库站点，请先在仓库管理中添加仓库</div>';
        return;
      }

      grid.innerHTML = warehouseSites.map(function(s, i) {
        var typeTag = s.type === 'json' ? 'JSON' : (s.type === 'xml' ? 'XML' : (s.type === 'js' ? 'JS' : (s.type == 3 ? 'T4' : 'BILI')));
        var searchTag = (s.searchable || s.quickSearch) ? '可搜索' : '不可搜索';
        var safeName = escapeHtml(s.name || '');
        var safeApi = escapeHtml(s.api || '');
        return '<div class="tv-site-item" data-site-id="' + s.id + '">' +
          '<div class="tv-site-icon">' + (i + 1) + '</div>' +
          '<div class="tv-site-info">' +
            '<span class="tv-site-name">' + safeName + '</span>' +
            '<span class="tv-site-desc">' + typeTag + ' · ' + searchTag + '</span>' +
            '<span class="tv-site-desc" style="font-size:9px;color:#556677;max-width:90%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + safeApi + '</span>' +
          '</div>' +
        '</div>';
      }).join('');

      grid.querySelectorAll('.tv-site-item').forEach(function(item) {
        item.addEventListener('click', function() {
          var siteId = this.getAttribute('data-site-id');
          if (window._selectSiteById) window._selectSiteById(siteId);
        });
      });
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

      var delBtnsHtml = sites.map(function(s, i) {
        return '<button class="nc-source-del" data-site-id="' + s.id + '">删</button>';
      }).join('');

      grid.innerHTML = sites.map(function(s, i) {
        var safeName = escapeHtml(s.name || '');
        var safeApi = escapeHtml(s.api || '');
        return '<div class="tv-site-item" data-site-id="' + s.id + '">' +
          '<div class="tv-site-icon">' + (i + 1) + '</div>' +
          '<div class="tv-site-info">' +
            '<span class="tv-site-name">' + safeName + '</span>' +
            '<span class="tv-site-desc">' + safeApi + '</span>' +
          '</div>' +
        '</div>' + delBtnsHtml.split('>删</button>').slice(i, i + 1).join('>删</button>');
      }).join('') +
      '<div style="margin-top:16px;padding-top:12px;border-top:1px solid rgba(255,255,255,.06)">' +
        '<button id="addLocalSiteBtn" style="width:100%;padding:12px;border:1px dashed rgba(33,150,243,.3);background:rgba(33,150,243,.05);color:#4aa8ff;border-radius:10px;font-size:13px">+ 添加本地CMS站点</button>' +
      '</div>';

      grid.querySelectorAll('.tv-site-item').forEach(function(item) {
        item.addEventListener('click', function() {
          var siteId = this.getAttribute('data-site-id');
          if (window._selectSiteById) window._selectSiteById(siteId);
        });
      });

      grid.querySelectorAll('.nc-source-del').forEach(function(btn) {
        btn.addEventListener('click', function(e) {
          e.stopPropagation();
          var siteId = this.getAttribute('data-site-id');
          if (window.deleteLocalSite) window.deleteLocalSite(siteId, e);
        });
      });

      var addBtn = document.getElementById('addLocalSiteBtn');
      if (addBtn) {
        addBtn.addEventListener('click', function() {
          if (window.showAddLocalSiteForm) window.showAddLocalSiteForm();
        });
      }
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

  // Wrapper that resolves site ID to full site object before calling selectSite
  window._selectSiteById = function(siteId) {
    console.log('[SITE] _selectSiteById called with:', siteId, 'type=' + typeof siteId);
    if (!window.NCDB) return;
    NCDB.getAllSites().then(function(sites) {
      console.log('[SITE] getAllSites count:', sites ? sites.length : 0);
      console.log('[SITE] All sites:', (sites||[]).map(function(s){return s.id + ':' + s.name}));
      var site = null;
      for (var i = 0; i < (sites || []).length; i++) {
        var sid = String(sites[i].id);
        var targetId = String(siteId);
        if (sid === targetId) { 
          console.log('[SITE] Matched site at index', i, ':', sites[i].name, 'id=' + sid);
          site = sites[i]; 
          break; 
        }
      }
      if (!site) { 
        console.warn('[SITE] No site found for id:', siteId);
        alert('站点不存在: ' + siteId); 
        return; 
      }
      currentSite = site;
      if (typeof hideSitePanel === 'function') hideSitePanel();
      selectSiteDirect(site);
    });
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
      if (typeof hideSitePanel === 'function') hideSitePanel();
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

  function selectSiteDirect(site) {
    if (typeof setMovieStatus === 'function') setMovieStatus('正在获取 ' + site.name + ' 的分类...', false);
    if (window.detectApiFormat) {
      window.detectApiFormat(site).then(function(format) {
        site.apiType = format;
        fetchSiteCategories(site);
      });
    } else {
      fetchSiteCategories(site);
    }
  }

  // Exposed for CSP_API_MAP retry (deprecated, now using inline map)
  window._retryT4Map = function(site) {
    fetchSiteCategories(site);
  };

  function extractCategoriesFromList(list) {
    var typeMap = {};
    for (var i = 0; i < (list || []).length; i++) {
      var v = list[i];
      var tid = v.type_id != null ? v.type_id : v.vod_id;
      var tname = v.type_name || v.vod_name || '未知';
      if (!typeMap[tid]) {
        typeMap[tid] = { type_id: String(tid), type_name: tname };
      }
    }
    var cats = [];
    for (var k in typeMap) {
      cats.push(typeMap[k]);
    }
    console.log('[SITE] Extracted', cats.length, 'categories from list:', cats.map(function(c){return c.type_name + '(' + c.type_id + ')'}).join(', '));
    return cats;
  }

  function fetchSiteCategories(site) {
    if (!site.api) {
      if (typeof setMovieStatus === 'function') setMovieStatus('站点没有API地址', false);
      return;
    }

    var apiUrl = site.api;
    var originalApi = site.api;
    // For T4/plugin sites (type=3), map to real CMS API
    if (site.type == 3) {
      var mappedApi = '';
      // Use inline CSP_API_MAP first, then merge with window.CSP_API_MAP if available
      var apiMap = {};
      for (var k in INLINE_CSP_API_MAP) apiMap[k] = INLINE_CSP_API_MAP[k];
      if (window.CSP_API_MAP) {
        for (var k2 in window.CSP_API_MAP) apiMap[k2] = window.CSP_API_MAP[k2];
      }
      
      if (apiMap[apiUrl]) {
        mappedApi = apiMap[apiUrl];
      } else {
        var base = apiUrl.replace(/Guard$/, '');
        if (apiMap[base]) {
          mappedApi = apiMap[base];
        }
      }
      if (mappedApi) {
        site.api = mappedApi;
        apiUrl = mappedApi;
        console.log('[SITE] T4 mapped:', site.name, originalApi, '->', mappedApi);
      } else {
        console.warn('[SITE] No T4 mapping for:', site.name, 'api:', apiUrl);
      }
    }

    if (site.type === 'json' || !site.type || site.type == 3) {
      console.log('[SITE] Fetching categories from:', apiUrl + '?ac=detail');
      fetchJsonSmart(apiUrl + '?ac=detail').then(function(data) {
        console.log('[SITE] ac=detail response:', data.class ? data.class.length + ' classes' : 'no class field, total=' + data.total + ', keys=' + Object.keys(data || {}).join(','));
        if (data && data.class && data.class.length) {
          site.categories = data.class;
          NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: data.class })).then(function() {
            onCategoriesLoaded(site);
          });
        } else {
          // 直接从 ac=list 获取分类（T4 API 的 list 包含所有 type_id）
          fetchJsonSmart(apiUrl + '?ac=list').then(function(data2) {
            console.log('[SITE] ac=list response:', data2.class ? data2.class.length + ' classes' : 'no class field, total=' + (data2.total || 'N/A') + ', keys=' + Object.keys(data2 || {}).join(','));
            if (data2 && data2.class && data2.class.length) {
              console.log('[SITE] ac=list class sample:', JSON.stringify(data2.class[0]));
              // 直接用 class 字段作为分类（标准格式）
              try {
                var cats = data2.class.map(function(c) {
                  return { type_id: String(c.type_id), type_name: c.type_name, type_pid: c.type_pid };
                });
                console.log('[SITE] Extracted', cats.length, 'categories from class');
                site.categories = cats;
                console.log('[SITE] Calling saveSiteConfig with warehouseId:', site.warehouseId, 'name:', site.name);
                NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: cats })).then(function(id) {
                  console.log('[SITE] saveSiteConfig resolved with id:', id);
                  onCategoriesLoaded(site);
                }).catch(function(e) { 
                  console.error('[SITE] saveSiteConfig error:', e, 'stack:', e.stack || 'no stack'); 
                  useDefaultCategories(site); 
                });
              } catch(e) {
                console.error('[SITE] class.map error:', e);
                useDefaultCategories(site);
              }
            } else if (data2 && data2.list && data2.list.length) {
              var cats = extractCategoriesFromList(data2.list);
              if (cats.length) {
                site.categories = cats;
                NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: cats })).then(function() {
                  onCategoriesLoaded(site);
                });
              } else {
                useDefaultCategories(site);
              }
            } else {
              useDefaultCategories(site);
            }
          }).catch(function() { useDefaultCategories(site); });
        }
      }).catch(function() {
        // ac=detail 失败，尝试 ac=list
        fetchJsonSmart(apiUrl + '?ac=list').then(function(data) {
          if (data && data.class && data.class.length) {
            site.categories = data.class;
            NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: data.class })).then(function() {
              onCategoriesLoaded(site);
            });
          } else if (data && data.list && data.list.length) {
            var cats = extractCategoriesFromList(data.list);
            if (cats.length) {
              site.categories = cats;
              NCDB.saveSiteConfig(site.warehouseId, Object.assign({}, site, { categories: cats })).then(function() {
                onCategoriesLoaded(site);
              });
            } else {
              useDefaultCategories(site);
            }
          } else {
            useDefaultCategories(site);
          }
        }).catch(function() { useDefaultCategories(site); });
      });
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
    console.log('[SITE] onCategoriesLoaded called, site.categories.length=', (site.categories||[]).length, 'site.key=', site.key);
    var api = site.api;
    var mappedApi = '';
    // Check if the current API is still a T4 key (not yet mapped)
    if (window.CSP_API_MAP && window.CSP_API_MAP[api]) {
      mappedApi = window.CSP_API_MAP[api];
    }
    // Ensure movieConfig exists (may be in different JS scope)
    if (!window.movieConfig) {
      window.movieConfig = { sites: [], classes: [], site: null, parses: [], lives: [], liveChannels: [] };
    }
    // Ensure movieState exists - use the one from nc-movie-engine.js if available
    if (!window.movieState) {
      window.movieState = { cat: '', page: 1, usingRemote: false, loaded: false, results: [] };
    }
    // Sync local movieState with window.movieState
    var ms = window.movieState;
    ms.cat = '推荐';
    ms.usingRemote = true;
    ms.loaded = true;
    ms.dbCats = site.categories.filter(function(c){return !c.type_pid||String(c.type_pid)==='0'}).map(function(c){return c.type_name});
    
    window.movieConfig.site = {
      key: site.key || site.name,
      name: site.name,
      api: mappedApi || api,
      type: site.type || 'json',
      categories: site.categories || [],
      ext: site.ext || {},
      timeout: site.timeout || 10,
      _originalApi: site._originalApi || (site.type == 3 ? site.api : null)
    };
    
    // Populate movieConfig.classes for classIdByName lookup
    console.log('[SITE] About to set movieConfig.classes, site.categories.length=', (site.categories||[]).length);
    if (site.categories && site.categories.length) {
      window.movieConfig.classes = site.categories;
      console.log('[SITE] movieConfig.classes set to', window.movieConfig.classes.length, 'items');
    }
    
    // Override categories for YGP (T4) sites
    if (site.key && (String(site.key).indexOf('YGPGuard') >= 0 || String(site.key) === 'YGP')) {
      window.movieConfig.site.categories = ['推荐', '预告片'];
    }
    
    // 更新UI
    var nameEl = document.getElementById('tvSourceName');
    if (nameEl) nameEl.textContent = site.name;
    
    if (typeof setMovieStatus === 'function') setMovieStatus('已选择: ' + site.name + '，正在加载推荐数据...', true);
    
    // Sync dbCats synchronously (only parent categories)
    if (window.movieState) {
      window.movieState.dbCats = site.categories.filter(function(c){
        var pid = c.type_pid;
        return pid==0||pid===null||pid===undefined||String(pid)==='0'||String(pid)==='';
      }).map(function(c){return c.type_name});
      console.log('[SITE] dbCats after filter:', window.movieState.dbCats.length, 'from', site.categories.length, 'total');
    }
    
    // Render categories immediately
    if (window.renderTvCats) {
      window.renderTvCats();
    }
    
    // Load movie list
    if (window.loadMovieList) {
      window.loadMovieList('推荐', 1);
    }
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

  window.deleteLocalSite = function(id) {
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
      fetch(site.api + '?ac=list').then(function(r) {
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
    return fetch(url).then(function(r) {
      if (!r.ok) throw 'HTTP ' + r.status;
      return r.json();
    });
  }

  function fetchTextSmart(url) {
    return fetch(url).then(function(r) {
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
  // Don't auto-call renderSitePanel here - it will be called after sites are loaded
})();
