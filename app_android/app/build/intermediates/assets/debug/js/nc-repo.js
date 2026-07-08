// NewCloud 仓库管理模块 - 重构版
// 双栏布局：左=仓库分类列表 | 右=仓库名称和地址

(function(){
  var currentCategoryId = 1;
  var warehouses = [];
  var categories = [];

  // ===== 初始化 =====
  function init() {
    if (!window.NCDB) return;
    NCDB.getWarehouseCategories().then(function(cats) {
      categories = cats;
      if (!categories.length) {
        NCDB.saveWarehouseCategory('默认分类').then(function() {
          categories = [{id: 1, name: '默认分类'}];
          currentCategoryId = 1;
          renderLeftPanel();
          loadWarehouses();
        });
      } else {
        currentCategoryId = categories[0].id;
        renderLeftPanel();
        loadWarehouses();
      }
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
  var slideCard = null;
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

    var html = '<div style="display:flex;flex-direction:column;gap:8px;padding-right:8px">';
    for (var i = 0; i < warehouses.length; i++) {
      var w = warehouses[i];
      html += '<div class="repo-warehouse-item" data-id="' + w.id + '" data-url="' + escapeHtml(w.url) + '">' +
        '<div class="repo-card-wrapper" style="display:flex;position:relative">' +
        '<div class="repo-card-front" style="display:flex;align-items:center;gap:10px;padding:10px 12px;background:rgba(255,255,255,.03);border:1px solid rgba(255,255,255,.06);border-radius:10px;cursor:pointer;transition:transform .2s ease;-webkit-transition:transform .2s ease;min-width:calc(100% - 70px);flex-shrink:0">' +
        '<div style="flex:1;min-width:0">' +
        '<div style="color:#fff;font-size:13px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + escapeHtml(w.name) + '</div>' +
        '<div style="color:#667788;font-size:11px;margin-top:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + escapeHtml(w.url) + '</div>' +
        '</div>' +
        '<div style="color:#4aa8ff;font-size:16px;flex-shrink:0">›</div>' +
        '</div>' +
        '<div class="repo-del-area" style="width:70px;display:flex;align-items:center;justify-content:center;flex-shrink:0">' +
        '<button onclick="deleteWarehouseItem(' + w.id + ',event)" style="border:none;background:#ef4444;color:#fff;font-size:15px;font-weight:700;cursor:pointer;border-radius:0 10px 10px 0;width:100%;height:100%">删</button>' +
        '</div>' +
        '</div></div>';
    }
    html += '</div>';
    content.innerHTML = html;
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
      html += '<button class="repo-cat-btn' + (c.id === currentCategoryId ? ' active' : '') + '" ' +
        'onclick="switchCategory(' + c.id + ')">' + escapeHtml(c.name) + '</button>';
    }
    html += '<button onclick="addNewCategory()" style="color:#4aa8ff;margin-top:12px;font-size:12px;padding:8px;border:1px dashed rgba(74,168,255,.3);background:transparent;border-radius:8px">+ 新建分类</button>';
    sidebar.innerHTML = html;
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
      div.setAttribute('onclick', 'closeAddWarehouseDialog()');
      div.innerHTML = '<div class="awd-panel" onclick="event.stopPropagation()">' +
        '<div class="awd-header"><span>添加仓库</span><span onclick="closeAddWarehouseDialog()" style="cursor:pointer;font-size:24px;line-height:1">&times;</span></div>' +
        '<div class="awd-body">' +
        '<input id="awdName" placeholder="仓库名称" style="width:100%;padding:12px;border:1px solid rgba(255,255,255,.08);background:rgba(255,255,255,.05);border-radius:8px;color:#e8edff;font-size:14px;margin-bottom:12px;box-sizing:border-box">' +
        '<input id="awdUrl" placeholder="仓库地址，例如：http://www.饭太硬.net/tv" style="width:100%;padding:12px;border:1px solid rgba(255,255,255,.08);background:rgba(255,255,255,.05);border-radius:8px;color:#e8edff;font-size:14px;margin-bottom:16px;box-sizing:border-box">' +
        '<div style="display:flex;gap:8px">' +
        '<button onclick="confirmAddWarehouse()" style="flex:1;padding:12px;border:none;border-radius:8px;background:rgba(33,150,243,.3);color:#90caf9;font-size:14px;cursor:pointer">保存</button>' +
        '<button onclick="closeAddWarehouseDialog()" style="flex:1;padding:12px;border:1px solid rgba(255,255,255,.08);border-radius:8px;background:transparent;color:#8899aa;font-size:14px;cursor:pointer">取消</button>' +
        '</div></div></div>';
      document.body.appendChild(div);
    }
    overlay.style.display = 'flex';
    overlay.classList.add('show');
    setTimeout(function() {
      var nameInput = document.getElementById('awdName');
      if (nameInput) nameInput.focus();
    }, 100);
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
    var url = urlEl ? urlEl.value.trim() : '';
    if (!name || !url) { alert('请填写仓库名称和地址'); return; }
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
    var wh = null;
    for (var i = 0; i < warehouses.length; i++) {
      if (warehouses[i].id === warehouseId) { wh = warehouses[i]; break; }
    }
    if (!wh) { console.log('[repo] warehouse not found'); alert('仓库不存在'); return; }
    console.log('[repo] warehouse found:', wh.name, wh.url);

    currentWarehouseId = warehouseId;
    hideRepoPanel();
    setMovieStatus('正在加载 ' + wh.name + '...', false);

    var urls = [wh.url];
    if (/饭太硬|xn--/i.test(wh.url)) {
      urls = urls.concat(['http://www.饭太硬.com/tv', 'http://饭太硬.top/tv', 'http://100km.top/0']);
    }
    console.log('[repo] URLs to try:', urls);
    loadConfigFromUrl(urls, 0);
  };

  function loadConfigFromUrl(urls, index) {
    if (index >= urls.length) {
      setMovieStatus('所有配置源均不可用', false);
      alert('无法加载仓库配置：所有URL均不可用');
      return;
    }
    var url = urls[index];
    setMovieStatus('正在加载配置源 ' + (index + 1) + '/' + urls.length + '...', false);
    fetchWithNative(url).then(function(text) {
      var config = parseWarehouseConfig(text, url);
      if (!config) throw '无法解析配置';
      var wh = null;
      for (var i = 0; i < warehouses.length; i++) {
        if (warehouses[i].id === currentWarehouseId) { wh = warehouses[i]; break; }
      }
      if (!wh) wh = {id: 1, name: '仓库'};
      processWarehouseConfig(config, wh);
    }).catch(function(e) {
      loadConfigFromUrl(urls, index + 1);
    });
  }

  function fetchWithNative(url) {
    if (window.NativeHttp && NativeHttp.httpGet) {
      return new Promise(function(resolve, reject) {
        setTimeout(function() {
          try {
            var text = NativeHttp.httpGet(url);
            if (!text) { reject('原生请求返回空内容'); return; }
            if (String(text).indexOf('__ERROR__') === 0) { reject(String(text).replace('__ERROR__', '')); return; }
            resolve(text);
          } catch(e) { reject(e.message); }
        }, 0);
      });
    }
    return fetch(url, {cache: 'no-store'}).then(function(r) {
      if (!r.ok) throw 'HTTP ' + r.status;
      return r.text();
    });
  }

  function parseWarehouseConfig(text, url) {
    var config = null;

    // 策略1: 直接JSON
    try { config = JSON.parse(text); } catch(e) {}
    if (config && config.sites) return config;

    // 策略2: 饭太硬特殊解码(JPEG头+Base64)
    try {
      var base64Str = text.replace(/^.*?(base64,)?/i, '').replace(/[^A-Za-z0-9+\/=]/g, '');
      var decoded = atob(base64Str);
      config = JSON.parse(decoded);
      if (config && config.sites) return config;
    } catch(e) {}

    // 策略3: 正则提取JSON对象
    var jsonMatch = text.match(/\{[\s\S]*"sites"[\s\S]*\}/);
    if (jsonMatch) {
      try { config = JSON.parse(jsonMatch[0]); } catch(e) {}
      if (config && config.sites) return config;
    }

    return null;
  }

  function processWarehouseConfig(config, warehouse) {
    var sites = [];
    if (config.sites) {
      for (var i = 0; i < config.sites.length; i++) {
        var s = config.sites[i];
        if (s.key && s.name) {
          sites.push({
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
          });
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

    var savePromises = sites.map(function(s) {
      return NCDB.saveSiteConfig(warehouse.id, s);
    });

    Promise.all(savePromises).then(function() {
      showSitePanel();
      setMovieStatus('已从 ' + warehouse.name + ' 加载 ' + sites.length + ' 个站点，请选择站点', true);
    }).catch(function(e) {
      setMovieStatus('保存站点失败: ' + e, false);
      alert('保存站点失败：' + e);
    });
  }

  // ===== 左滑删除 =====
  var slideItem = null;
  var startX = 0;
  var currentX = 0;
  var isSliding = false;
  var slideCard = null;
  var longPressTimer = null;

  function handleLongPressAction(id) {
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
    if (e) e.stopPropagation();
    if (!confirm('确定删除此仓库？')) return;
    if (!window.NCDB) { alert('数据库未初始化'); return; }
    NCDB.deleteWarehouse(id).then(function() { loadWarehouses(); });
  };

  window.initSwipeToDelete = function() {
    var items = document.querySelectorAll('.repo-warehouse-item');
    for (var i = 0; i < items.length; i++) {
      (function(item) {
        var wrapper = item.querySelector('.repo-card-wrapper');
        if (!wrapper) return;
        var front = wrapper.querySelector('.repo-card-front');
        if (!front) return;
        var id = parseInt(item.getAttribute('data-id'));

        front.addEventListener('touchstart', function(e) {
          startX = e.touches[0].clientX;
          currentX = startX;
          isSliding = true;
          slideItem = item;
          slideCard = wrapper;

          longPressTimer = setTimeout(function() {
            if (isSliding) {
              isSliding = false;
              handleLongPressAction(id);
            }
          }, 500);
        }, {passive: true});

        front.addEventListener('touchmove', function(e) {
          if (longPressTimer) {
            clearTimeout(longPressTimer);
            longPressTimer = null;
          }
          if (!isSliding || !slideCard) return;
          currentX = e.touches[0].clientX;
          var diff = currentX - startX;
          if (diff < 0 && diff > -100) {
            slideCard.style.transform = 'translateX(' + diff + 'px)';
          }
        }, {passive: true});

        front.addEventListener('touchend', function() {
          if (longPressTimer) {
            clearTimeout(longPressTimer);
            longPressTimer = null;
          }
          if (!slideCard) return;
          var diff = currentX - startX;
          if (diff < -60) {
            slideCard.style.transform = 'translateX(-70px)';
            slideCard.setAttribute('data-swiped', 'true');
          } else {
            slideCard.style.transform = 'translateX(0)';
            slideCard.removeAttribute('data-swiped');
          }
          slideItem = null;
          slideCard = null;
          isSliding = false;
        });

        front.addEventListener('click', function(e) {
          alert('click triggered, id: ' + item.getAttribute('data-id'));
        });
      })(items[i]);
    }
  };

  // ===== 面板渲染 =====
  window.renderRepoPanel = function() {
    renderLeftPanel();
    loadWarehouses();
    setTimeout(function() {
      window.initSwipeToDelete();
    }, 100);
  };

  // 初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setTimeout(init, 200);
    });
  } else {
    setTimeout(init, 200);
  }
})();
