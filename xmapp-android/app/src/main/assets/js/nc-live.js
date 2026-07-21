// NewCloud 直播模块 - 完整三栏布局（分类 | 频道 | 源）

(function(){
  var liveState = {
    allChannels: [],
    sourceGroups: {},
    currentSource: null,
    groups: {},
    currentGroup: null,
    currentChannel: null,
    currentChannelIdx: 0,
    decoder: localStorage.getItem('live_decoder') || 'ijk_hard',
    scale: localStorage.getItem('live_scale') || 'default',
    timeout: parseInt(localStorage.getItem('live_timeout') || '10', 10),
    netSpeed: 0,
    longPressTimer: null,
    _nextId: 0
  };

  // ===== 初始化直播页面 =====
  window.initLivePage = function() {
    var container = document.getElementById('livePageContainer');
    if (!container) return;
    
    if (window.NCDB) {
      NCDB.getAllLiveChannels().then(function(channels) {
        liveState.allChannels = channels || [];
        liveState.sourceNames = getSourceNames();
        if (!liveState.currentSource && liveState.sourceNames.length > 0) {
          liveState.currentSource = liveState.sourceNames[0];
        }
        buildSourceTreeForCurrent();
        renderLivePage();
        updateMineLiveCount();
      });
    } else {
      renderLivePage();
    }
  };

  function getSourceNames() {
    var names = [];
    liveState.allChannels.forEach(function(ch) {
      var src = ch.fromSite || '本地';
      if (names.indexOf(src) === -1) names.push(src);
    });
    names.sort(function(a, b) {
      if (a === '本地') return -1;
      if (b === '本地') return 1;
      return a.localeCompare(b);
    });
    return names;
  }

  function buildSourceTreeForCurrent() {
    if (!liveState.currentSource) return;
    liveState.groups = {};
    
    liveState.allChannels.forEach(function(ch) {
      if (ch.fromSite !== liveState.currentSource) return;
      // 跳过注意事项/温馨提示等非频道分组
      if (/^(注意事项|列表更新时间)$/.test(ch.group)) return;
      var grp = ch.group || '其他';
      if (!liveState.groups[grp]) {
        liveState.groups[grp] = [];
      }
      liveState.groups[grp].push(ch);
    });
    
    liveState.currentGroup = Object.keys(liveState.groups)[0] || '其他';
    liveState.currentChannel = null;
    liveState.currentChannelIdx = 0;
    
    console.log('[LIVE] buildSourceTreeForCurrent[' + liveState.currentSource + ']: groups=' + Object.keys(liveState.groups).length + ' channels=' + liveState.allChannels.filter(function(c){return c.fromSite===liveState.currentSource}).length);
  }

  // ===== 渲染直播页面（下拉框 + 三栏布局）=====
  function renderLivePage() {
    var container = document.getElementById('livePageContainer');
    if (!container) return;

    var groupKeys = Object.keys(liveState.groups);
    var channelsInGroup = liveState.groups[liveState.currentGroup] || [];
    var channelsInChannel = liveState.currentChannel ? getChannelsForCurrentChannel() : [];
    
    var html = '<div class="live-page-new-layout">' +
      '<div class="live-player-section">' +
        '<div class="live-player-placeholder" id="livePlayerPlaceholder">' +
          '<div class="live-player-icon">▶</div>' +
          '<div class="live-now-playing" id="liveNowPlaying">选择频道开始播放</div>' +
        '</div>' +
      '</div>' +
      '<div class="live-source-selector-new" id="liveSourceSelectorNew">' +
        '<span class="source-label-new">直播源:</span>' +
        '<select id="liveSourceDropdown">' +
          (liveState.sourceNames || []).map(function(src) {
            var isSelected = src === liveState.currentSource;
            return '<option value="' + escapeAttr(src) + '"' + (isSelected ? ' selected' : '') + '>' + escapeHtml(src) + '</option>';
          }).join('') +
        '</select>' +
      '</div>' +
      '<div class="live-three-col-layout">' +
        '<div class="live-col live-col-categories" id="liveColCategories">' +
          groupKeys.map(function(g) {
            var count = (liveState.groups[g] || []).length;
            var isActive = g === liveState.currentGroup;
            return '<div class="live-category-item' + (isActive ? ' active' : '') + '" data-group="' + escapeAttr(g) + '">' +
              '<span class="category-name">' + escapeHtml(g) + '</span>' +
              '<span class="category-count">' + count + '</span>' +
            '</div>';
          }).join('') +
        '</div>' +
        '<div class="live-col live-col-channels" id="liveColChannels">' +
          channelsInGroup.map(function(ch, idx) {
            var isActive = liveState.currentChannel && liveState.currentChannel.name === ch.name;
            return '<div class="live-channel-btn' + (isActive ? ' active' : '') + '" data-channel-idx="' + idx + '">' +
              escapeHtml(ch.name || ch.title || '未知频道') +
            '</div>';
          }).join('') +
        '</div>' +
        '<div class="live-col live-col-sources-new" id="liveColSourcesNew">' +
          (channelsInChannel.length > 0 ? channelsInChannel.map(function(ch, idx) {
            var isActive = liveState.currentChannelIdx === idx;
            return '<div class="live-source-btn-new' + (isActive ? ' active' : '') + '" data-source-idx="' + idx + '">' +
              '源' + (idx + 1) +
            '</div>';
          }).join('') : '<div style="padding:12px;color:#667788;font-size:12px;text-align:center">选择频道</div>') +
        '</div>' +
      '</div>' +
    '</div>';

    container.innerHTML = html;

    var dropdown = document.getElementById('liveSourceDropdown');
    if (dropdown) {
      dropdown.onchange = function() {
        onSourceDropdownChange(this.value);
      };
    }

    var catContainer = document.getElementById('liveColCategories');
    if (catContainer) {
      catContainer.onclick = function(e) {
        var item = e.target.closest('.live-category-item');
        if (item) {
          var group = item.getAttribute('data-group');
          liveState.currentGroup = group;
          liveState.currentChannel = null;
          liveState.currentChannelIdx = 0;
          renderLivePage();
          if (window.switchLiveGroup) window.switchLiveGroup(group);
        }
      };
    }

    var chanContainer = document.getElementById('liveColChannels');
    if (chanContainer) {
      chanContainer.onclick = function(e) {
        var btn = e.target.closest('.live-channel-btn');
        if (btn) {
          var idx = parseInt(btn.getAttribute('data-channel-idx'), 10);
          var channels = liveState.groups[liveState.currentGroup] || [];
          if (channels[idx]) {
            liveState.currentChannel = channels[idx];
            liveState.currentChannelIdx = 0;
            renderLivePage();
            playCurrentChannel();
          }
        }
      };
      bindChannelLongPress(chanContainer);
    }

    var srcContainer = document.getElementById('liveColSourcesNew');
    if (srcContainer) {
      srcContainer.onclick = function(e) {
        var btn = e.target.closest('.live-source-btn-new');
        if (btn) {
          var idx = parseInt(btn.getAttribute('data-source-idx'), 10);
          liveState.currentChannelIdx = idx;
          renderLivePage();
          playCurrentChannel();
        }
      };
    }
  }

  function playCurrentChannel() {
    if (!liveState.currentChannel) return;
    var channelSources = getChannelsForCurrentChannel();
    console.log('[LIVE] playCurrentChannel: channelSources=', channelSources.length, 'currentIdx=', liveState.currentChannelIdx);
    if (channelSources.length > 0) {
      var ch = channelSources[liveState.currentChannelIdx] || channelSources[0];
      console.log('[LIVE] Playing URL:', ch.url, 'name:', ch.name);
      if (window.playLiveChannel) {
        window.playLiveChannel(ch.url, ch.name);
      }
    } else {
      console.warn('[LIVE] No channel sources found!');
    }
  }

  function getChannelsForCurrentChannel() {
    if (!liveState.currentChannel || !liveState.currentGroup) return [];
    var allChannels = liveState.groups[liveState.currentGroup] || [];
    var channelName = liveState.currentChannel.name;
    return allChannels.filter(function(ch) { return ch.name === channelName; });
  }

  window.onSourceDropdownChange = function(sourceName) {
    if (sourceName === liveState.currentSource) return;
    liveState.currentSource = sourceName;
    buildSourceTreeForCurrent();
    renderLivePage();
    console.log('[LIVE] Switched to source:', sourceName);
  };

  function escapeAttr(s) {
    return String(s).replace(/'/g, "\\'").replace(/"/g, '&quot;');
  }

  function escapeHtmlAttr(s) {
    return String(s||'').replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  function escapeHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ===== 播放直播频道 =====
  window.playLiveChannel = function(urlOrChannel, name) {
    var url, chName;
    if (typeof urlOrChannel === 'object' && urlOrChannel !== null && urlOrChannel.url) {
      url = urlOrChannel.url;
      chName = urlOrChannel.name || urlOrChannel.title || '未知频道';
    } else {
      url = urlOrChannel;
      chName = name || '未知频道';
    }
    
    console.log('[LIVE] playLiveChannel called: url=', url, 'name=', chName);
    
    var nowPlaying = document.getElementById('liveNowPlaying');
    if (nowPlaying) {
      nowPlaying.textContent = '正在播放: ' + chName;
    }
    liveState.currentChannel = { url: url, name: chName };
    
    // 使用 ExoPlayer 内嵌播放
    var exoPlayerObj = window.exoPlayer;
    if (exoPlayerObj && typeof exoPlayerObj.playInline === 'function') {
      console.log('[LIVE] Using ExoPlayer inline');
      var isLive = url.indexOf('.m3u8') >= 0 || url.indexOf('m3u8') >= 0;
      var params = JSON.stringify({
        title: chName,
        url: url,
        isLive: isLive
      });
      console.log('[LIVE] playInline params:', params);
      var result = exoPlayerObj.playInline(params);
      console.log('[LIVE] playInline returned:', result);
      return;
    }
    console.warn('[LIVE] No ExoPlayer playInline available, falling back to HTML5 video');
  };

  // ===== 分组切换 =====
  window.switchLiveGroup = function(group) {
    liveState.currentGroup = group;
    renderLivePage();
  };

  // ===== 长按菜单 =====
  function handleLongPress(e, channelJson) {
    e.preventDefault();
    var ch = JSON.parse(channelJson);
    liveState.currentChannel = ch;
    showSettingsMenu(ch);
  }

  function showSettingsMenu(channel) {
    // 移除旧菜单
    var old = document.getElementById('liveSettingsMenu');
    if (old) old.remove();

    var menu = document.createElement('div');
    menu.id = 'liveSettingsMenu';
    menu.className = 'live-settings-menu';
    menu.innerHTML = '<div class="menu-backdrop" id="liveMenuBackdrop"></div>' +
      '<div class="menu-content">' +
        '<div class="menu-header">' +
          '<h3>' + escapeHtml(channel.name || channel.title || '频道设置') + '</h3>' +
          '<button class="menu-close" id="liveMenuCloseBtn">&#10005;</button>' +
        '</div>' +
        
        // 解码方式
        '<div class="menu-section">' +
          '<div class="section-title">解码方式</div>' +
          '<div class="menu-options">' +
            radioOption('decoder', 'ijk_hard', 'IJK 硬解', liveState.decoder) +
            radioOption('decoder', 'ijk_soft', 'IJK 软解', liveState.decoder) +
            radioOption('decoder', 'exo_hard', 'EXO 硬解', liveState.decoder) +
            radioOption('decoder', 'exo_soft', 'EXO 软解', liveState.decoder) +
          '</div>' +
        '</div>' +
        
        // 画面缩放
        '<div class="menu-section">' +
          '<div class="section-title">画面缩放</div>' +
          '<div class="menu-options">' +
            radioOption('scale', 'default', '默认', liveState.scale) +
            radioOption('scale', '16:9', '16:9', liveState.scale) +
            radioOption('scale', '4:3', '4:3', liveState.scale) +
            radioOption('scale', 'fill', '填充', liveState.scale) +
            radioOption('scale', 'original', '原始', liveState.scale) +
            radioOption('scale', 'crop', '剪裁', liveState.scale) +
          '</div>' +
        '</div>' +
        
        // 超时换源
        '<div class="menu-section">' +
          '<div class="section-title">超时换源(秒)</div>' +
          '<div class="menu-options">' +
            radioOption('timeout', '5', '5秒', liveState.timeout) +
            radioOption('timeout', '10', '10秒', liveState.timeout) +
            radioOption('timeout', '15', '15秒', liveState.timeout) +
            radioOption('timeout', '20', '20秒', liveState.timeout) +
            radioOption('timeout', '25', '25秒', liveState.timeout) +
            radioOption('timeout', '30', '30秒', liveState.timeout) +
          '</div>' +
        '</div>' +
        
        // 网速显示
        '<div class="menu-section">' +
          '<div class="section-title">网速显示</div>' +
          '<div class="speed-display" id="speedDisplay">0 KB/s</div>' +
        '</div>' +
        
        // 播放按钮
        '<div class="menu-play-btn" id="liveMenuPlayBtn">' +
          '<button class="play-btn-primary">播放 ' + escapeHtml(channel.name || channel.title || '') + '</button>' +
        '</div>' +
      '</div>';

    document.body.appendChild(menu);

    // Event delegation
    document.getElementById('liveMenuBackdrop').addEventListener('click', window.closeSettingsMenu);
    document.getElementById('liveMenuCloseBtn').addEventListener('click', window.closeSettingsMenu);
    document.getElementById('liveMenuPlayBtn').addEventListener('click', function() { if (window.playFromMenu) window.playFromMenu(); });
    document.querySelectorAll('#liveSettingsMenu input[type="radio"]').forEach(function(radio) {
      radio.addEventListener('change', function() {
        if (window.onSettingChange) window.onSettingChange(radio.name, radio.value);
      });
    });
  }

  function radioOption(name, value, label, current) {
    var checked = String(current) === String(value) ? ' checked' : '';
    return '<label class="menu-option">' +
      '<input type="radio" name="' + name + '" value="' + escapeAttr(value) + '"' + checked + ' onchange="onSettingChange(\'' + name + '\',\'' + escapeAttr(value) + '\')">' +
      '<span class="option-label">' + label + '</span>' +
    '</label>';
  }

  function escapeAttr(s) {
    return String(s).replace(/'/g, "\\'");
  }

  window.closeSettingsMenu = function() {
    var menu = document.getElementById('liveSettingsMenu');
    if (menu) menu.remove();
  };

  window.onSettingChange = function(name, value) {
    if (name === 'decoder') {
      liveState.decoder = value;
      localStorage.setItem('live_decoder', value);
    } else if (name === 'scale') {
      liveState.scale = value;
      localStorage.setItem('live_scale', value);
    } else if (name === 'timeout') {
      liveState.timeout = parseInt(value, 10);
      localStorage.setItem('live_timeout', value);
    }
  };

  window.playFromMenu = function() {
    if (!liveState.currentChannel) return;
    closeSettingsMenu();
    var ch = liveState.currentChannel;
    if (ch.url) {
      playLiveChannelInternal(ch.url, ch.name || ch.title);
    }
  };

  // ===== 触摸事件(长按检测) =====
  var touchStartTime = 0;
  var touchMoved = false;

  window.handleTouchStart = function(e) {
    touchStartTime = Date.now();
    touchMoved = false;
  };

  window.handleTouchEnd = function(e) {
    var duration = Date.now() - touchStartTime;
    if (duration >= 500 && !touchMoved) {
      // 长按事件 - 触发菜单
      var card = e.currentTarget;
      var id = card.getAttribute('data-id');
      var channel = findChannelById(id);
      if (channel) {
        liveState.currentChannel = channel;
        showSettingsMenu(channel);
      }
    }
  };

  window.handleTouchMove = function(e) {
    touchMoved = true;
  };

  // 也支持桌面端右键
  document.addEventListener('contextmenu', function(e) {
    var card = e.target.closest('.live-channel-card, .live-channel-btn');
    if (card) {
      e.preventDefault();
      var idx = parseInt(card.getAttribute('data-channel-idx'), 10);
      var channels = liveState.groups[liveState.currentGroup] || [];
      var channel = channels[idx];
      if (channel) {
        liveState.currentChannel = channel;
        showSettingsMenu(channel);
      }
    }
  });

  // 绑定长按事件到频道按钮
  function bindChannelLongPress(container) {
    if (!container) return;
    container.querySelectorAll('.live-channel-btn').forEach(function(btn) {
      var timer = null;
      var moved = false;
      btn.addEventListener('touchstart', function() {
        moved = false;
        timer = setTimeout(function() {
          if (!moved) {
            var idx = parseInt(btn.getAttribute('data-channel-idx'), 10);
            var channels = liveState.groups[liveState.currentGroup] || [];
            var channel = channels[idx];
            if (channel) {
              liveState.currentChannel = channel;
              showSettingsMenu(channel);
            }
          }
        }, 500);
      }, {passive: true});
      btn.addEventListener('touchmove', function() { moved = true; }, {passive: true});
      btn.addEventListener('touchend', function() { clearTimeout(timer); });
      btn.addEventListener('touchcancel', function() { clearTimeout(timer); });
    });
  }

  function findChannelById(id) {
    for (var g in liveState.groups) {
      var ch = liveState.groups[g].find(function(c) {
        return String(c.id || c.vodId || '') === String(id);
      });
      if (ch) return ch;
    }
    return liveState.channels.find(function(c) {
      return String(c.id || c.vodId || '') === String(id);
    });
  }


  function openLiveVideoModal(title, url, decoder, scale, timeout) {
    var modal = document.getElementById('videoModal');
    var player = document.getElementById('videoPlayer');
    var titleEl = document.getElementById('videoTitle');
    if (!modal || !player) return;

    // 销毁旧播放器
    if (window.NCPlayerPlugin) NCPlayerPlugin.destroy();
    var mount = document.getElementById('artPlayerMount');
    if (mount) mount.innerHTML = '';

    player.style.display = 'block';
    player.removeAttribute('src');
    player.src = url;
    player.poster = '';
    titleEl.textContent = title;

    // 隐藏剧集列表
    var list = document.getElementById('episodeList');
    if (list) list.innerHTML = '';

    // 设置播放器参数
    player.preload = 'auto';
    player.controls = true;

    // 超时换源逻辑
    var switchTimer = setTimeout(function() {
      console.log('直播源超时，尝试切换解码方式');
      if (decoder === 'ijk_hard') {
        liveState.decoder = 'ijk_soft';
        localStorage.setItem('live_decoder', 'ijk_soft');
      } else if (decoder === 'ijk_soft') {
        liveState.decoder = 'exo_hard';
        localStorage.setItem('live_decoder', 'exo_hard');
      } else {
        liveState.decoder = 'exo_soft';
        localStorage.setItem('live_decoder', 'exo_soft');
      }
      player.src = url;
    }, timeout * 1000);

    player.onloadeddata = function() {
      clearTimeout(switchTimer);
      player.play().catch(function(e) {
        console.log('自动播放失败:', e);
      });
    };

    player.onerror = function() {
      clearTimeout(switchTimer);
      console.log('播放出错');
    };

    modal.classList.add('show');

    // 尝试使用EXO Player播放
    if (window.EXOPlayer && EXOPlayer.isAvailable()) {
      EXOPlayer.play(title, [{name: title, url: url}]);
    }
  }

  // ===== 网速模拟显示 =====
  function updateNetSpeed() {
    var el = document.getElementById('speedDisplay');
    if (el) {
      // 模拟网速变化
      liveState.netSpeed = Math.floor(Math.random() * 5000) + 500;
      el.textContent = liveState.netSpeed + ' KB/s';
    }
  }
  setInterval(updateNetSpeed, 2000);

  // ===== 从仓库获取直播源 =====
  window.fetchLiveFromWarehouses = function() {
    if (!window.NCDB) return;
    
    NCDB.getAllWarehouses().then(function(warehouses) {
      if (!warehouses || !warehouses.length) {
        initLivePage();
        return;
      }

      var fetchPromises = warehouses.map(function(w) {
        return fetchLiveFromWarehouse(w);
      });

      Promise.all(fetchPromises).then(function() {
        initLivePage();
      });
    });
  };

  function fetchLiveFromWarehouse(warehouse) {
    if (!warehouse.url) return Promise.resolve();
    
    return new Promise(function(resolve) {
      if (window.fetchWarehouseConfig) {
        fetchWarehouseConfig(warehouse.url).then(function(config) {
          if (config && config.lives) {
            config.lives.forEach(function(live) {
              if (live.url || live.api || live.path) {
                var liveName = live.name || ('直播源');
                fetchLiveList(live.url || live.api || live.path, liveName);
              }
            });
          }
          resolve();
        }).catch(function() {
          resolve();
        });
      } else {
        resolve();
      }
    });
  }

  function fetchLiveList(url, fromSite) {
    fetch(url).then(function(r) {
      return r.text();
    }).then(function(text) {
      parseAndSaveLiveChannels(text, fromSite);
    }).catch(function() {});
  }

  function parseAndSaveLiveChannels(text, fromSite) {
    var channels = [];
    
    if (text.indexOf('#EXTINF') >= 0) {
      channels = parseM3U(text);
    } else {
      channels = parseTXT(text);
    }

    if (channels.length && window.NCDB) {
      NCDB.saveLiveChannels(fromSite, channels).then(function() {
        initLivePage();
      });
    }
  }

  function parseM3U(text) {
    var channels = [];
    var lines = text.split(/\r?\n/);
    var currentChannel = null;

    for (var i = 0; i < lines.length; i++) {
      var line = lines[i].trim();

      if (line.startsWith('#EXTINF:')) {
        currentChannel = {
          name: extractChannelName(line),
          group: extractGroup(line),
          logo: extractLogo(line),
          url: ''
        };
      } else if (line && !line.startsWith('#') && currentChannel) {
        if (line.indexOf('http') === 0 || line.indexOf('rtmp') === 0) {
          currentChannel.url = line;
          channels.push(currentChannel);
          currentChannel = null;
        }
      }
    }

    return channels;
  }

  function extractChannelName(line) {
    var parts = line.split(',');
    return parts.length > 1 ? parts[parts.length - 1].trim() : '未知频道';
  }

  function extractGroup(line) {
    var match = line.match(/group-title="([^"]*)"/);
    return match ? match[1] : '其他';
  }

  function extractLogo(line) {
    var match = line.match(/tvg-logo="([^"]*)"/);
    return match ? match[1] : '';
  }

  function parseTXT(text) {
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
          channels.push({
            name: name,
            group: currentGroup,
            url: url,
            logo: ''
          });
        }
      }
    }

    return channels;
  }

  // ===== 暴露全局函数 =====
  window.renderLiveCategories = function() {
    // 三栏布局中已经包含分类渲染，此函数保留兼容
  };

  window.renderLiveChannels = function() {
    // 三栏布局中已经包含频道渲染，此函数保留兼容
  };

  // ===== 刷新指定直播源 =====
  window.refreshLiveSource = function(sourceName) {
    if (!window.NCDB) return;
    
    // 找到该直播源的所有频道 URL
    NCDB.getLiveChannels().then(function(channels) {
      var urls = [];
      channels.forEach(function(ch) {
        if (ch.fromSite === sourceName && urls.indexOf(ch.url) === -1) {
          urls.push(ch.url);
        }
      });
      
      if (urls.length > 0) {
        // 逐个刷新
        var index = 0;
        function refreshNext() {
          if (index >= urls.length) {
            alert('直播源 "' + sourceName + '" 刷新完成');
            renderLiveSourceManager();
            return;
          }
          var url = urls[index];
          index++;
          fetch(url).then(function(r) {
            return r.text();
          }).then(function(text) {
            // 解析 M3U 或 TXT 格式
            var newChannels = [];
            if (text.indexOf('#EXTINF') >= 0) {
              newChannels = parseM3ULiveText(text);
            } else {
              newChannels = parseTXTLiveText(text);
            }
            
            if (newChannels.length > 0) {
              // 保存新频道
              newChannels.forEach(function(ch) {
                ch.fromSite = sourceName;
              });
              NCDB.saveLiveChannels(sourceName, newChannels).then(function() {
                console.log('[LIVE] Refreshed source: ' + sourceName + ' with ' + newChannels.length + ' channels');
                refreshNext();
              });
            } else {
              refreshNext();
            }
          }).catch(function() {
            refreshNext();
          });
        }
        refreshNext();
      } else {
        alert('直播源 "' + sourceName + '" 没有找到可刷新的频道');
      }
    });
  };

  // ===== 解析 M3U 格式 =====
  function parseM3ULiveText(text) {
    var channels = [];
    // 恢复换行符（Android WebView 会移除换行）
    text = text.replace(/#EXTINF:/g, '\n#EXTINF:');
    var lines = text.split('\n');
    var currentChannel = null;

    for (var i = 0; i < lines.length; i++) {
      var line = lines[i].trim();

      if (line.startsWith('#EXTINF:')) {
        currentChannel = {
          name: extractChannelName(line),
          group: extractGroup(line),
          logo: extractLogo(line),
          url: ''
        };
      } else if (line && !line.startsWith('#') && currentChannel) {
        if (line.indexOf('http') === 0 || line.indexOf('rtmp') === 0) {
          currentChannel.url = line;
          channels.push(currentChannel);
          currentChannel = null;
        }
      }
    }

    return channels;
  }

  // ===== 解析 TXT 格式 =====
  function parseTXTLiveText(text) {
    var channels = [];
    // 恢复换行符
    text = text.replace(/,/g, ',\n');
    var lines = text.split('\n');
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
          channels.push({
            name: name,
            group: currentGroup,
            url: url,
            logo: ''
          });
        }
      }
    }

    return channels;
  }

  function extractChannelName(line) {
    var parts = line.split(',');
    return parts.length > 1 ? parts[parts.length - 1].trim() : '未知频道';
  }

  function extractGroup(line) {
    var match = line.match(/group-title="([^"]*)"/);
    return match ? match[1] : '其他';
  }

  function extractLogo(line) {
    var match = line.match(/tvg-logo="([^"]*)"/);
    return match ? match[1] : '';
  }

  // 初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setTimeout(initLivePage, 500);
    });
  } else {
    setTimeout(initLivePage, 500);
  }
})();
