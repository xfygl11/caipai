// NewCloud 仓库管理模块 - 重构版
// 双栏布局：左=仓库分类列表 | 右=仓库名称和地址

(function(){
  var currentCategoryId = 1;
  var warehouses = [];
  var categories = [];

  // ===== 默认直播源地址（从饭太硬导航页获取）=====
  var DEFAULT_LIVE_SOURCES = [
    {name: 'develop202', url: 'https://gh.927223.xyz/https://raw.githubusercontent.com/develop202/migu_video/refs/heads/main/interface.txt'},
    {name: 'Kimentanm', url: 'https://gh.927223.xyz/https://raw.githubusercontent.com/Kimentanm/aptv/master/m3u/iptv.m3u'},
    {name: '范明明', url: 'https://nos.netease.com/ysf/3d75a78a0fc7ede372c03598d6d10367.m3u'},
    {name: '世界杯', url: 'http://82.156.243.185:33389/fwc.m3u'},
    {name: '虎牙一起看', url: 'https://sub.ottiptv.cc/huyayqk.m3u'},
    {name: '斗鱼一起看', url: 'https://sub.ottiptv.cc/douyuyqk.m3u'},
    {name: 'B站直播', url: 'https://sub.ottiptv.cc/bililive.m3u'},
    {name: 'YY轮播', url: 'https://sub.ottiptv.cc/yylunbo.m3u'}
  ];

  // ===== 初始化 =====
  var _initRetries = 0;
  var _initMaxRetries = 30;
  function init() {
    if (!window.NCDB) {
      _initRetries++;
      if (_initRetries >= _initMaxRetries) {
        console.error('[repo] NCDB not available after ' + _initMaxRetries + ' retries, giving up');
        if (window.NCUI && window.NCUI.toast) NCUI.toast('数据库初始化失败，请重启应用');
        return;
      }
      console.log('[repo] NCDB not available, retry ' + _initRetries + '/' + _initMaxRetries + '...');
      setTimeout(init, 300);
      return;
    }
    NCDB.getWarehouseCategories().then(function(cats) {
      categories = cats;
      if (!categories.length) {
        NCDB.saveWarehouseCategory('默认分类').then(function() {
          NCDB.getWarehouseCategories().then(function(cats2) {
            categories = cats2;
            if (categories.length) {
              currentCategoryId = categories[0].id;
              renderLeftPanel();
              loadWarehouses();
              initDefaultLiveSources();
              autoLoadWarehouses();
            }
          });
        });
      } else {
        currentCategoryId = categories[0].id;
        renderLeftPanel();
        loadWarehouses();
        initDefaultLiveSources();
        autoLoadWarehouses();
      }
    });
  }

  // ===== 自动加载仓库中的站点配置 =====
  function autoLoadWarehouses() {
    NCDB.getAllWarehouses().then(function(warehouses) {
      if (warehouses && warehouses.length > 0) {
        var wh = warehouses[0];
        var lastFetched = wh.lastFetched;
        var now = Date.now();
        // 如果仓库有缓存（lastFetched 不为空且距离现在不超过24小时），跳过自动加载
        if (lastFetched && (now - lastFetched) < 24 * 60 * 60 * 1000) {
          console.log('[repo] Warehouse', wh.name, 'cached', Math.round((now - lastFetched) / 60 / 1000), 'min ago, skipping auto-load');
          // 直接加载站点到电影引擎
          loadCachedSites(wh);
          return;
        }
        console.log('[repo] Auto-loading warehouse:', wh.name);
        loadWarehouseConfig(wh.id);
      }
    });
  }

  // ===== 使用缓存的站点数据 =====
  function loadCachedSites(warehouse) {
    console.log('[repo] Loading cached sites for warehouse:', warehouse.name);
    NCDB.getSitesByWarehouse(warehouse.id).then(function(sites) {
      if (!sites || !sites.length) {
        console.log('[repo] No cached sites found, loading from network');
        loadWarehouseConfig(warehouse.id);
        return;
      }
      console.log('[repo] Using', sites.length, 'cached sites');
      // 清除旧数据
      if (window.NCDB && window.NCDB.clearAllSiteConfigs) {
        window.NCDB.clearAllSiteConfigs();
      }
      // 保存缓存站点
      var savePromises = sites.map(function(s) {
        return NCDB.saveSiteConfig(warehouse.id, s);
      });
      Promise.all(savePromises).then(function() {
        console.log('[repo] Cached sites loaded, showing site panel');
        movieConfig.sites = sites;
        movieConfig.site = window.chooseUsableSite ? window.chooseUsableSite(sites) : sites[0];
        movieState.usingRemote = true;
        movieState.loaded = true;
        if (typeof updateSiteSelect === 'function') updateSiteSelect();
        if (typeof window.renderMovieHome === 'function') window.renderMovieHome();
        if (typeof setMovieStatus === 'function') setMovieStatus('已从 ' + warehouse.name + ' 加载缓存 (' + sites.length + ' 个站点)', true);
        // 初始化直播页面
        if (window.initLivePage) window.initLivePage();
      });
    });
  }

  // ===== 从饭太硬导航页获取直播源地址 =====
  window.fetchLiveSourcesFromFanTaiYing = function() {
    if (!window.NCDB) return;
    
    var fanTaiYingUrls = [
      'http://www.饭太硬.net/tv',
      'http://www.饭太硬.com/tv',
      'http://饭太硬.top/tv',
      'http://100km.top/0'
    ];
    
    fetchLiveSourceFromUrl(fanTaiYingUrls, 0);
  };

  function fetchLiveSourceFromUrl(urls, index) {
    if (index >= urls.length) {
      console.log('[repo] All fan taiying URLs failed');
      return;
    }
    
    var url = urls[index];
    console.log('[repo] Fetching live sources from:', url);
    
    // Use NativeHttp for Android WebView compatibility
    if (window.NativeHttp && typeof NativeHttp.httpGet === 'function') {
      try {
        var result = NativeHttp.httpGet(url);
        if (result) {
          parseLiveSourcesFromResponse(result, url);
        } else {
          fetchLiveSourceFromUrl(urls, index + 1);
        }
      } catch(e) {
        console.error('[repo] NativeHttp error:', e);
        fetchLiveSourceFromUrl(urls, index + 1);
      }
    } else {
      fetch(url).then(function(r) { return r.arrayBuffer(); }).then(function(buffer) {
        var text = new TextDecoder('utf-8').decode(buffer);
        parseLiveSourcesFromResponse(text, url);
      }).catch(function(e) {
        console.error('[repo] Fetch error:', e);
        fetchLiveSourceFromUrl(urls, index + 1);
      });
    }
  }

  function parseLiveSourcesFromResponse(text, sourceUrl) {
    console.log('[repo] Parsing response, length:', text.length);
    
    // Check if response is JPEG (饭太硬导航页返回JPEG图片)
    var uint8 = new Uint8Array(text.length);
    for (var i = 0; i < text.length; i++) {
      uint8[i] = text.charCodeAt(i) & 0xFF;
    }
    
    // Search for JPEG end marker (ffd9)
    var jpegEndIdx = -1;
    for (var i = 1; i < uint8.length - 1; i++) {
      if (uint8[i-1] === 0xFF && uint8[i] === 0xD9) {
        jpegEndIdx = i + 1;
        break;
      }
    }
    
    if (jpegEndIdx > 0 && jpegEndIdx < uint8.length - 10) {
      // Search for '**' after jpeg end marker
      var starStarIdx = -1;
      for (var j = jpegEndIdx; j < Math.min(uint8.length, jpegEndIdx + 500); j++) {
        if (uint8[j] === 0x2A && uint8[j+1] === 0x2A) {
          starStarIdx = j + 2;
          break;
        }
      }
      
      if (starStarIdx > 0) {
        // Extract base64 data as bytes, filter only base64 chars
        var b64Bytes = [];
        for (var k = starStarIdx; k < uint8.length; k++) {
          var ch = uint8[k];
          if ((ch >= 0x41 && ch <= 0x5A) || (ch >= 0x61 && ch <= 0x7A) || (ch >= 0x30 && ch <= 0x39) || ch === 0x2B || ch === 0x2F || ch === 0x3D) {
            b64Bytes.push(ch);
          }
        }
        
        if (b64Bytes.length > 100) {
          // Convert bytes to string and decode base64
          var base64Str = String.fromCharCode.apply(null, b64Bytes);
          while (base64Str.length % 4) base64Str += '=';
          
          var decoded = atob(base64Str);
          var utf8Bytes = [];
          for (var m = 0; m < decoded.length; m++) {
            utf8Bytes.push(decoded.charCodeAt(m) & 0xFF);
          }
          var utf8String = '';
          try {
            utf8String = new TextDecoder('utf-8').decode(new Uint8Array(utf8Bytes));
          } catch(e) {
            utf8String = decodeUtf8Bytes(utf8Bytes);
          }
          
          try {
            var config = JSON.parse(utf8String);
            if (config && config.lives) {
              saveLiveSourcesToDB(config.lives);
            }
          } catch(e) {
            console.error('[repo] JSON parse error:', e);
          }
        }
      }
    } else {
      // Not JPEG, try to parse as JSON directly
      try {
        var config = JSON.parse(text);
        if (config && config.lives) {
          saveLiveSourcesToDB(config.lives);
        }
      } catch(e) {
        console.error('[repo] JSON parse error:', e);
      }
    }
  }

  function saveLiveSourcesToDB(lives) {
    if (!window.NCDB) return;
    
    // Clear existing live sources
    NCDB.clearAllLiveSources();
    
    var savePromises = lives.map(function(live) {
      var name = live.name || '直播源';
      var url = live.url || live.api || live.path;
      if (url) {
        return NCDB.saveLiveSource(name, url);
      }
      return Promise.resolve();
    }).filter(function(p) { return p; });
    
    Promise.all(savePromises).then(function() {
      console.log('[repo] Saved', lives.length, 'live sources to DB');
      // Refresh live page
      if (window.initLivePage) window.initLivePage();
    });
  }

  // ===== 初始化默认直播源到NCDB =====
  function initDefaultLiveSources() {
    if (!window.NCDB) return;
    NCDB.getLiveSourceNames().then(function(names) {
      if (!names || names.length === 0) {
        // 没有直播源，初始化默认的
        var savePromises = DEFAULT_LIVE_SOURCES.map(function(ls) {
          return NCDB.saveLiveSource(ls.name, ls.url);
        });
        Promise.all(savePromises).then(function() {
          console.log('[repo] Initialized', DEFAULT_LIVE_SOURCES.length, 'default live sources');
        });
      }
    }).catch(function(e) {
      console.error('[repo] Error checking live sources:', e);
    });
  }

  // ===== 加载仓库列表 =====
  function loadWarehouses() {
    if (!window.NCDB) return;
    NCDB.getWarehousesByCategory(currentCategoryId).then(function(list) {
      warehouses = list || [];
      renderRightPanel();
    });
  }

  // ===== 渲染右侧面板（仓库名称和地址） =====
  function renderRightPanel() {
    var content = document.getElementById('repoContent');
    if (!content) return;

    if (!warehouses.length) {
      content.innerHTML = '<div style="text-align:center;padding:40px 20px;color:#667788">' +
        '<div style="font-size:48px;margin-bottom:16px;opacity:0.3">📦</div>' +
        '<div style="font-size:14px;margin-bottom:8px">暂无仓库</div>' +
        '<div style="font-size:12px">点击右上角"添加"按钮创建仓库</div>' +
        '</div>';
      return;
    }

    var html = '';
    for (var i = 0; i < warehouses.length; i++) {
      var w = warehouses[i];
      html += '<div class="repo-card-row" data-warehouse-id="' + w.id + '">' +
        '<div class="repo-card-front" style="flex:1;display:flex;align-items:center;gap:10px;padding:12px 14px;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);border-radius:12px 0 0 12px;cursor:pointer;transition:background .15s">' +
        '<div style="flex:1;min-width:0">' +
        '<div style="color:#fff;font-size:14px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + escapeHtml(w.name) + '</div>' +
        '<div style="color:#667788;font-size:11px;margin-top:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + escapeHtml(w.url) + '</div>' +
        '</div>' +
        '</div>' +
        '<button class="repo-del-btn" style="border:none;background:#dc2626;color:#fff;font-size:12px;font-weight:600;cursor:pointer;border-radius:0 12px 12px 0;padding:0 16px;min-width:56px;transition:opacity .15s">删除</button>' +
        '</div>';
    }
    content.innerHTML = html;

    // Bind click events
    var rows = content.querySelectorAll('.repo-card-row');
    for (var j = 0; j < rows.length; j++) {
      (function(row) {
        var id = row.getAttribute('data-warehouse-id');
        var front = row.querySelector('.repo-card-front');
        var delBtn = row.querySelector('.repo-del-btn');
        
        front.addEventListener('click', function() { 
          console.log('[repo] front clicked, id=', id);
          loadWarehouseConfig(id); 
        });
        front.addEventListener('mouseenter', function() { this.style.background = 'rgba(255,255,255,.08)'; });
        front.addEventListener('mouseleave', function() { this.style.background = 'rgba(255,255,255,.04)'; });
        
        delBtn.addEventListener('click', function(e) {
          e.stopPropagation();
          console.log('[repo] delete btn clicked, id=', id);
          deleteWarehouseItem(id, e);
        });
        delBtn.addEventListener('mouseenter', function() { this.style.opacity = '0.8'; });
        delBtn.addEventListener('mouseleave', function() { this.style.opacity = '1'; });
      })(rows[j]);
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ===== 渲染左侧面板（仓库分类） =====
  function renderLeftPanel() {
    var sidebar = document.getElementById('repoSidebar');
    if (!sidebar) return;

    var html = '';
    for (var i = 0; i < categories.length; i++) {
      var c = categories[i];
      html += '<button class="repo-cat-btn' + (c.id === currentCategoryId ? ' active' : '') + '" data-cat-id="' + c.id + '">' + escapeHtml(c.name) + '</button>';
    }
    html += '<button id="addCategoryBtn" style="color:#4aa8ff;margin-top:12px;font-size:12px;padding:8px;border:1px dashed rgba(74,168,255,.3);background:transparent;border-radius:8px">+ 新建分类</button>';
    sidebar.innerHTML = html;

    sidebar.querySelectorAll('.repo-cat-btn[data-cat-id]').forEach(function(btn) {
      btn.addEventListener('click', function() {
        var id = parseInt(this.getAttribute('data-cat-id'));
        if (window.switchCategory) window.switchCategory(id);
      });
    });

    var addBtn = document.getElementById('addCategoryBtn');
    if (addBtn) {
      addBtn.addEventListener('click', function() {
        if (window.addNewCategory) window.addNewCategory();
      });
    }
  }

  // ===== 切换分类 =====
  window.switchCategory = function(id) {
    currentCategoryId = id;
    renderLeftPanel();
    loadWarehouses();
  };

  // ===== 新建分类 =====
  window.addNewCategory = function() {
    var name = prompt('请输入分类名称：');
    if (!name || !name.trim()) return;
    if (!window.NCDB) { alert('数据库未初始化'); return; }
    NCDB.saveWarehouseCategory(name.trim()).then(function() {
      NCDB.getWarehouseCategories().then(function(cats) {
        categories = cats;
        currentCategoryId = cats[cats.length - 1].id;
        renderLeftPanel();
        loadWarehouses();
      });
    });
  };

  // ===== 添加仓库弹窗 =====
  window.showAddWarehouseDialog = function() {
    var overlay = document.getElementById('addWarehouseDialog');
    if (!overlay) {
      var div = document.createElement('div');
      div.id = 'addWarehouseDialog';
      div.className = 'awd-overlay';
      div.innerHTML = '<div class="awd-panel" id="awdPanel">' +
        '<div class="awd-header"><span>添加仓库</span><span id="awdCloseBtn" style="cursor:pointer;font-size:24px;line-height:1">&times;</span></div>' +
        '<div class="awd-body">' +
        '<input id="awdName" placeholder="仓库名称" style="width:100%;padding:12px;border:1px solid rgba(255,255,255,.08);background:rgba(255,255,255,.05);border-radius:8px;color:#e8edff;font-size:14px;margin-bottom:12px;box-sizing:border-box">' +
        '<input id="awdUrl" placeholder="仓库地址，例如：http://www.饭太硬.net/tv" style="width:100%;padding:12px;border:1px solid rgba(255,255,255,.08);background:rgba(255,255,255,.05);border-radius:8px;color:#e8edff;font-size:14px;margin-bottom:16px;box-sizing:border-box">' +
        '<div style="display:flex;gap:8px">' +
        '<button id="awdSaveBtn" style="flex:1;padding:12px;border:none;border-radius:8px;background:rgba(33,150,243,.3);color:#90caf9;font-size:14px;cursor:pointer">保存</button>' +
        '<button id="awdCancelBtn" style="flex:1;padding:12px;border:1px solid rgba(255,255,255,.08);border-radius:8px;background:transparent;color:#8899aa;font-size:14px;cursor:pointer">取消</button>' +
        '</div></div></div>';
      document.body.appendChild(div);
      overlay = div;
    }
    overlay.style.display = 'flex';
    overlay.classList.add('show');

    // Re-bind events (in case overlay already existed)
    var awdPanel = document.getElementById('awdPanel');
    if (awdPanel) {
      var newPanel = awdPanel.cloneNode(true);
      awdPanel.parentNode.replaceChild(newPanel, awdPanel);
    }
    var newCloseBtn = document.getElementById('awdCloseBtn');
    if (newCloseBtn) newCloseBtn.addEventListener('click', window.closeAddWarehouseDialog);
    var newSaveBtn = document.getElementById('awdSaveBtn');
    if (newSaveBtn) newSaveBtn.addEventListener('click', window.confirmAddWarehouse);
    var newCancelBtn = document.getElementById('awdCancelBtn');
    if (newCancelBtn) newCancelBtn.addEventListener('click', window.closeAddWarehouseDialog);

    overlay.setAttribute('onclick', 'window._closeAwdOverlay(event)');
    setTimeout(function() {
      var nameInput = document.getElementById('awdName');
      if (nameInput) nameInput.focus();
    }, 100);
  };

  window._closeAwdOverlay = function(e) {
    if (e && e.target && e.target.id === 'addWarehouseDialog') {
      window.closeAddWarehouseDialog();
    }
  };

  window.closeAddWarehouseDialog = function() {
    var overlay = document.getElementById('addWarehouseDialog');
    if (overlay) {
      overlay.classList.remove('show');
      setTimeout(function() { overlay.style.display = 'none'; }, 250);
    }
  };

  window.confirmAddWarehouse = function() {
    var nameEl = document.getElementById('awdName');
    var urlEl = document.getElementById('awdUrl');
    var name = nameEl ? nameEl.value.trim() : '';
    var url = urlEl ? urlEl.value.trim().replace(/\s+/g, '') : '';
    if (!name || !url) { alert('请填写仓库名称和地址'); return; }
    if (!/^https?:\/\/.+/.test(url)) { alert('请输入有效的 URL 地址（以 http:// 或 https:// 开头）'); return; }
    if (!window.NCDB) { alert('数据库未初始化'); return; }
    NCDB.saveWarehouse(currentCategoryId, name, url).then(function() {
      closeAddWarehouseDialog();
      loadWarehouses();
    });
  };

  // ===== 刷新仓库 =====
  window.refreshWarehouses = function() {
    if (!window.NCDB) return;
    setMovieStatus('正在刷新仓库...', false);
    var promises = warehouses.map(function(w) {
      return NCDB.updateWarehouseLastFetched(w.id, Date.now()).catch(function(){});
    });
    Promise.all(promises).then(function() {
      loadWarehouses();
      setMovieStatus('刷新完成', true);
    });
  };

  // ===== 加载仓库配置 =====
  var currentWarehouseId = null;
  window.loadWarehouseConfig = function(warehouseId) {
    console.log('[repo] loadWarehouseConfig called with:', warehouseId);
    if (!window.NCDB) { alert('数据库未初始化'); return; }
    
    NCDB.getAllWarehouses().then(function(allWh) {
      console.log('[repo] getAllWarehouses result:', allWh);
      var wh = null;
      for (var i = 0; i < (allWh || []).length; i++) {
        if (allWh[i].id == warehouseId) { wh = allWh[i]; break; }
      }
      if (!wh) { console.log('[repo] warehouse not found, id=', warehouseId); alert('仓库不存在'); return; }
      console.log('[repo] warehouse found:', wh.name, wh.url);

      currentWarehouseId = warehouseId;
      if (typeof hideRepoPanel === 'function') hideRepoPanel();
      if (typeof setMovieStatus === 'function') setMovieStatus('正在加载 ' + wh.name + '...', false);

      var urls = [wh.url];
      console.log('[repo] URLs to try:', urls);
      loadConfigFromUrl(urls, 0);
    }).catch(function(e) {
      console.log('[repo] error loading warehouse:', e);
      alert('加载仓库失败：' + e);
    });
  };

  function loadConfigFromUrl(urls, index) {
    if (index >= urls.length) {
      if (typeof setMovieStatus === 'function') setMovieStatus('所有配置源均不可用', false);
      alert('无法加载仓库配置：所有URL均不可用');
      return;
    }
    var url = urls[index];
    console.log('[repo] fetching URL', index + 1, '/', urls.length, ':', url);
    if (typeof setMovieStatus === 'function') setMovieStatus('正在加载配置源 ' + (index + 1) + '/' + urls.length + '...', false);
    fetchWithNative(url).then(function(buffer) {
      console.log('[repo] fetch success, buffer length:', buffer.byteLength);
      var config = parseWarehouseConfig(buffer, url);
      if (!config) {
        console.log('[repo] parse failed');
        throw '无法解析配置';
      }
      console.log('[repo] config parsed, sites count:', config.sites ? config.sites.length : 0);
      var wh = null;
      for (var i = 0; i < warehouses.length; i++) {
        if (warehouses[i].id === currentWarehouseId) { wh = warehouses[i]; break; }
      }
      if (!wh) wh = {id: 1, name: '仓库'};
      processWarehouseConfig(config, wh);
    }).catch(function(e) {
      console.log('[repo] fetch failed:', e);
      loadConfigFromUrl(urls, index + 1);
    });
  }

  function fetchWithNative(url) {
    console.log('[repo] fetchWithNative called with:', url);
    // 优先: JS fetch (处理二进制数据, 如果 CORS 允许)
    if (typeof fetch === 'function') {
      console.log('[repo] using JS fetch');
      return fetch(url).then(function(r) {
        console.log('[repo] fetch status:', r.status, 'url:', r.url);
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.arrayBuffer();
      }).then(function(buffer) {
        console.log('[repo] fetch ArrayBuffer length:', buffer.byteLength);
        return buffer;
      }).catch(function(fetchErr) {
        console.warn('[repo] JS fetch failed, trying NativeHttp:', fetchErr);
        return tryNativeHttpFetch(url);
      });
    }
    return tryNativeHttpFetch(url);
  }

  function tryNativeHttpFetch(url) {
    if (window.NativeHttp && typeof NativeHttp.httpGetBytes === 'function') {
      console.log('[repo] using NativeHttp binary fetch');
      return new Promise(function(resolve, reject) {
        try {
          var b64 = NativeHttp.httpGetBytes(url);
          if (!b64) { reject(new Error('NativeHttp binary fetch returned empty')); return; }
          // base64 decode to ArrayBuffer
          var binaryStr = atob(b64);
          var bytes = new Uint8Array(binaryStr.length);
          for (var i = 0; i < binaryStr.length; i++) { bytes[i] = binaryStr.charCodeAt(i); }
          resolve(bytes.buffer);
        } catch(e) {
          console.error('[repo] NativeHttp binary fetch error:', e);
          reject(e);
        }
      });
    }
    if (window.NativeHttp && typeof NativeHttp.httpGet === 'function') {
      console.log('[repo] using NativeHttp text fetch (may corrupt binary)');
      return new Promise(function(resolve, reject) {
        try {
          var result = NativeHttp.httpGet(url);
          if (result === null || result === undefined) { reject(new Error('NativeHttp returned empty')); return; }
          var encoder = new TextEncoder();
          resolve(encoder.encode(result).buffer);
        } catch(e) { reject(e); }
      });
    }
    return Promise.reject(new Error('No fetch method available'));
  }

  function parseWarehouseConfig(buffer, url) {
    var config = null;
    
    // Convert buffer to uint8 for binary pattern matching
    var uint8 = new Uint8Array(buffer);
    console.log('[repo] parseWarehouseConfig: buffer length:', uint8.length);
    
    // Search for JPEG end marker (ffd9) directly in the buffer
    var jpegEndIdx = -1;
    for (var i = 1; i < uint8.length - 1; i++) {
      if (uint8[i-1] === 0xFF && uint8[i] === 0xD9) {
        jpegEndIdx = i + 1;
        break;
      }
    }
    console.log('[repo] jpegEndIdx:', jpegEndIdx);
    
    if (jpegEndIdx > 0 && jpegEndIdx < uint8.length - 10) {
      // Search for '**' after jpeg end marker
      var starStarIdx = -1;
      for (var j = jpegEndIdx; j < Math.min(uint8.length, jpegEndIdx + 500); j++) {
        if (uint8[j] === 0x2A && uint8[j+1] === 0x2A) {
          starStarIdx = j + 2;
          break;
        }
      }
      console.log('[repo] starStarIdx:', starStarIdx);
      if (starStarIdx > 0) {
        // Extract base64 data as bytes, filter only base64 chars
        var b64Bytes = [];
        for (var k = starStarIdx; k < uint8.length; k++) {
          var ch = uint8[k];
          if ((ch >= 0x41 && ch <= 0x5A) || (ch >= 0x61 && ch <= 0x7A) || (ch >= 0x30 && ch <= 0x39) || ch === 0x2B || ch === 0x2F || ch === 0x3D) {
            b64Bytes.push(ch);
          }
        }
        console.log('[repo] base64 bytes found:', b64Bytes.length);
        // Convert bytes to string and decode base64
        var base64Str = String.fromCharCode.apply(null, b64Bytes);
        console.log('[repo] base64Str preview:', base64Str.substring(0, 50));
        
        if (base64Str.length > 100) {
          // Pad base64 to multiple of 4
          while (base64Str.length % 4) base64Str += '=';
          
          // Decode base64 to bytes
          var decoded = atob(base64Str);
          console.log('[repo] base64 decoded, length:', decoded.length);
          
          // Convert to UTF-8 string
          var utf8Bytes = [];
          for (var m = 0; m < decoded.length; m++) {
            utf8Bytes.push(decoded.charCodeAt(m) & 0xFF);
          }
          var utf8String = '';
          try {
            utf8String = new TextDecoder('utf-8').decode(new Uint8Array(utf8Bytes));
          } catch(e) {
            utf8String = decodeUtf8Bytes(utf8Bytes);
          }
          console.log('[repo] utf8String preview:', utf8String.substring(0, 200));
          config = JSON.parse(utf8String);
          console.log('[repo] JSON parsed, sites count:', config ? config.sites ? config.sites.length : 'no sites' : 'null');
          console.log('[repo] first 3 sites:', config && config.sites ? config.sites.slice(0, 3).map(function(s){return s.name}).join(', ') : 'N/A');
          if (config && config.sites) return config;
        }
      }
    }

    return null;
  }

  function processWarehouseConfig(config, warehouse) {
    console.log('[repo] processWarehouseConfig called, warehouse:', warehouse.name, 'id:', warehouse.id);
    var sites = [];
    if (config.sites) {
      for (var i = 0; i < config.sites.length; i++) {
        var s = config.sites[i];
        if (s.key && s.name) {
          var site = {
            name: s.name,
            key: s.key,
            api: s.api || '',
            type: s.type || 3,
            searchable: s.searchable || 0,
            quickSearch: s.quickSearch || 0,
            ext: s.ext || {},
            timeout: s.timeout || 10,
            sourceType: 'warehouse',
            warehouseId: warehouse.id
          };
          sites.push(site);
        }
      }
    }

    // 处理仓库中的直播源
    if (config.lives || config.live) {
      var lives = (config.lives || config.live);
      for (var j = 0; j < lives.length; j++) {
        var live = lives[j];
        var liveUrl = live.url || live.api || live.path;
        if (liveUrl) {
          var liveName = live.name || ('直播源' + (j + 1));
          fetchLiveListFromWarehouse(liveUrl, liveName);
        }
      }
    }

    if (!sites.length) {
      alert('仓库中没有找到可用站点');
      return;
    }

    if (!window.NCDB) {
      alert('数据库未初始化');
      return;
    }

    // Clear ALL old warehouse sites before saving new ones
    if (window.NCDB && window.NCDB.clearAllSiteConfigs) {
      window.NCDB.clearAllSiteConfigs();
    }
    _saveNewSites(sites, warehouse);

    function _saveNewSites(sites, warehouse) {
      window._currentWarehouseId = warehouse.id;
      var savePromises = sites.map(function(s, i) {
        if (i < 3 || i === sites.length - 1) {
          console.log('[repo] saving site', (i+1)+'/'+sites.length, ':', s.name, 'key:', s.key, 'api:', s.api);
        }
        return NCDB.saveSiteConfig(warehouse.id, s);
      });

      console.log('[repo] saving', savePromises.length, 'sites...');
      console.log('[repo] NCDB exists:', !!window.NCDB, 'saveSiteConfig:', typeof window.NCDB?.saveSiteConfig);
      Promise.all(savePromises).then(function(ids) {
        console.log('[repo] ALL SAVED, ids:', ids.length, 'showing panel...');
        console.log('[repo] showSitePanel exists:', typeof showSitePanel);
        console.log('[repo] window.renderSitePanel exists:', typeof window.renderSitePanel);
        if (typeof showSitePanel === 'function') {
          console.log('[repo] calling showSitePanel...');
          showSitePanel();
          console.log('[repo] showSitePanel returned');
        } else {
          console.log('[repo] showSitePanel is NOT a function!');
        }
        // Also try window.renderSitePanel directly
        if (typeof window.renderSitePanel === 'function') {
          console.log('[repo] also calling window.renderSitePanel directly');
          window.renderSitePanel();
        }
        // Manually show the panel overlay
        try {
          var panel = document.getElementById('sitePanelOverlay');
          if (panel) {
            panel.style.display = 'flex';
            console.log('[repo] manually showed panel overlay');
          } else {
            console.log('[repo] panel overlay NOT FOUND!');
          }
        } catch(e) { console.log('[repo] manual panel show error:', e); }
        if (typeof setMovieStatus === 'function') setMovieStatus('已从 ' + warehouse.name + ' 加载 ' + sites.length + ' 个站点，请选择站点', true);
        // 更新最后加载时间
        NCDB.updateWarehouseLastFetched(warehouse.id, Date.now()).catch(function(){});
      }).catch(function(e) {
        console.log('[repo] Promise.all REJECTED:', e);
        setMovieStatus('保存站点失败: ' + e, false);
        alert('保存站点失败：' + e);
      });
    }
  }

  // ===== 工具函数 =====
  var longPressTimer = null;
  var _lpActive = false;

  function handleLongPressAction(id) {
    _lpActive = true;
    setTimeout(function() { _lpActive = false; }, 100);
    var wh = null;
    for (var i = 0; i < warehouses.length; i++) {
      if (warehouses[i].id === id) { wh = warehouses[i]; break; }
    }
    if (!wh) return;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(wh.url).then(function() {
        alert('已复制：' + wh.name + '\n' + wh.url);
      }).catch(function() {
        fallbackCopy(wh.url);
      });
    } else {
      fallbackCopy(wh.url);
    }
  }

  function fallbackCopy(text) {
    var textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.select();
    try { document.execCommand('copy'); alert('已复制：' + text); }
    catch(e) { alert('复制失败，请手动复制'); }
    document.body.removeChild(textarea);
  }

  window.deleteWarehouseItem = function(id, e) {
    console.log('[repo] deleteWarehouseItem called with id:', id);
    if (e) e.stopPropagation();
    if (!confirm('确定删除此仓库？')) return;
    if (!window.NCDB) { alert('数据库未初始化'); return; }
    NCDB.deleteWarehouse(id).then(function() { 
      console.log('[repo] warehouse deleted, reloading...');
      loadWarehouses(); 
    });
  };

  window.initSwipeToDelete = function() {
    // No longer needed - events bound in renderRightPanel
  };

  // ===== 面板渲染 =====

  // UTF-8 decoder for base64-decoded bytes (fallback when TextDecoder unavailable)
  function decodeUtf8Bytes(bytes) {
    var result = [];
    var i = 0;
    while (i < bytes.length) {
      var b = bytes[i];
      if (b < 0x80) {
        result.push(String.fromCharCode(b));
        i++;
      } else if (b < 0xE0) {
        result.push(String.fromCharCode(((b & 0x1F) << 6) | (bytes[i+1] & 0x3F)));
        i += 2;
      } else if (b < 0xF0) {
        result.push(String.fromCharCode(((b & 0x0F) << 12) | ((bytes[i+1] & 0x3F) << 6) | (bytes[i+2] & 0x3F)));
        i += 3;
      } else {
        // 4-byte UTF-8 (emoji etc)
        var cp = ((b & 0x07) << 18) | ((bytes[i+1] & 0x3F) << 12) | ((bytes[i+2] & 0x3F) << 6) | (bytes[i+3] & 0x3F);
        cp -= 0x10000;
        result.push(String.fromCharCode(0xD800 + (cp >> 10), 0xDC00 + (cp & 0x3FF)));
        i += 4;
      }
    }
    return result.join('');
  }

  window.renderRepoPanel = function() {
    renderLeftPanel();
    loadWarehouses();
    setTimeout(function() {
      window.initSwipeToDelete();
    }, 100);
  };

  function fetchLiveListFromWarehouse(url, warehouseId) {
    console.log('[repo] Fetching live source from:', url, 'warehouse:', warehouseId);
    // Use NativeHttp for Android WebView compatibility
    if (window.NativeHttp && typeof NativeHttp.httpGet === 'function') {
      try {
        var result = NativeHttp.httpGet(url);
        if (result) {
          processLiveSourceText(result, url, warehouseId);
        } else {
          console.error('[repo] NativeHttp.httpGet returned null for:', url);
        }
      } catch(e) {
        console.error('[repo] NativeHttp.httpGet error:', e);
      }
    } else {
      fetch(url).then(function(r) { return r.text(); }).then(function(text) {
        processLiveSourceText(text, url, warehouseId);
      }).catch(function(e) {
        console.error('[repo] Live source fetch failed:', e);
      });
    }
  }

  function processLiveSourceText(text, url, warehouseId) {
    console.log('[repo] processLiveSourceText: text length=' + (text||'').length + ' url=' + url);
    if (!text) {
      console.warn('[repo] Empty text received for', url);
      return;
    }
    var hasExtinf = text.indexOf('#EXTINF') >= 0;
    console.log('[repo] Has #EXTINF:', hasExtinf, 'First 200 chars:', (text||'').substring(0, 200));
    var channels = [];
    if (hasExtinf) {
      channels = parseM3ULiveText(text);
    } else {
      channels = parseTXTLiveText(text);
    }
    console.log('[repo] Parsed', channels.length, 'live channels from warehouse');
    if (channels.length && window.NCDB) {
      NCDB.saveLiveChannels(warehouseId, channels);
      console.log('[repo] Saved', channels.length, 'live channels for warehouse', warehouseId, '(total now:)', NCDB.getAllLiveChannelsSync ? NCDB.getAllLiveChannelsSync().length : 'N/A');
      if (window.initLivePage) {
        console.log('[repo] Calling initLivePage after save');
        window.initLivePage();
      }
    }
  }

  function parseM3ULiveText(text) {
    var channels = [];
    
    // Android WebView NativeHttp may strip newlines. Restore them.
    var restored = text;
    // Split before each #EXTINF: marker
    restored = restored.replace(/(#EXTINF:)/g, '\n$1');
    if (restored.charAt(0) === '\n') restored = restored.substring(1);
    
    // Process line by line - split channel name from URL in #EXTINF lines
    var lines = restored.split('\n');
    var finalLines = [];
    for (var li = 0; li < lines.length; li++) {
      var line = lines[li].trim();
      if (line.indexOf('#EXTINF:') === 0) {
        // Find: #EXTINF:...,<channelName><URL>
        // Use last comma to separate attributes from channel name
        var lastComma = line.lastIndexOf(',');
        if (lastComma >= 0) {
          var beforeName = line.substring(0, lastComma + 1); // includes comma
          var afterComma = line.substring(lastComma + 1);
          // Check if afterComma starts with http/rtmp (meaning no channel name, just URL)
          var urlMatch = afterComma.match(/^(https?:\/\/|rtmp:\/\/)/);
          if (urlMatch) {
            // No channel name, just URL after comma
            finalLines.push(beforeName.trim());
            finalLines.push(afterComma);
          } else {
            // Channel name followed immediately by URL
            var nameEnd = afterComma.search(/https?:\/\/|rtmp:\/\//);
            if (nameEnd > 0) {
              var chName = afterComma.substring(0, nameEnd).trim();
              var urlPart = afterComma.substring(nameEnd);
              finalLines.push(beforeName + chName);
              finalLines.push(urlPart);
            } else if (nameEnd === 0) {
              // Comma directly followed by URL
              finalLines.push(beforeName.trim());
              finalLines.push(afterComma);
            } else {
              // Just channel name, no URL on this line
              finalLines.push(line);
            }
          }
        } else {
          finalLines.push(line);
        }
      } else {
        finalLines.push(line);
      }
    }
    
    var currentChannel = null;
    for (var i = 0; i < finalLines.length; i++) {
      var line = finalLines[i].trim();
      if (line.startsWith('#EXTINF:')) {
        var namePart = line.substring(10);
         var commaIdx = namePart.lastIndexOf(',');
         var name = commaIdx >= 0 ? namePart.substring(commaIdx + 1).trim() : '未知频道';
         if (!name || name === ',') name = '未知频道';

         // 跳过元数据行（更新日期、温馨提示等非频道内容）
         if (/^(更新日期|温馨提示|提醒)/.test(name)) {
           currentChannel = null;
           continue;
         }

         currentChannel = {
          name: name,
          group: extractLiveGroup(line),
          logo: extractLiveLogo(line),
          url: ''
        };
      } else if (line && !line.startsWith('#') && currentChannel && !currentChannel.url) {
        if (line.indexOf('http') === 0 || line.indexOf('rtmp') === 0) {
          currentChannel.url = line;
          channels.push(currentChannel);
          currentChannel = null;
        }
      }
    }
    return channels;
  }

  function parseTXTLiveText(text) {
    var channels = [];
    var lines = text.split(/\r?\n/);
    var currentGroup = '直播';
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i].trim();
      if (!line || line.startsWith('#')) {
        if (line && line.indexOf('group-title') >= 0) {
          var g = line.match(/group-title="([^"]*)"/);
          if (g) currentGroup = g[1];
        }
        continue;
      }
      if (line.indexOf(',') > 0) {
        var parts = line.split(',');
        var name = parts[0].trim();
        var url = parts.slice(1).join(',').trim();
        if (url.indexOf('http') === 0 || url.indexOf('rtmp') === 0) {
          channels.push({ name: name, group: currentGroup, url: url, logo: '' });
        }
      }
    }
    return channels;
  }

  function extractLiveName(line) {
    var parts = line.split(',');
    return parts.length > 1 ? parts[parts.length - 1].trim() : '未知频道';
  }

  function extractLiveGroup(line) {
    var match = line.match(/group-title="([^"]*)"/);
    return match ? match[1] : '其他';
  }

  function extractLiveLogo(line) {
    var match = line.match(/tvg-logo="([^"]*)"/);
    return match ? match[1] : '';
  }

  // 初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setTimeout(init, 200);
    });
  } else {
    setTimeout(init, 200);
  }
})();
