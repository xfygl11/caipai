// NewCloud 直播模块
// 管理直播频道，支持M3U/TXT格式解析，EXO播放器播放

(function(){
  var liveState = {
    channels: [],
    groups: {},
    currentGroup: '全部'
  };

  // ===== 初始化直播 =====
  window.initLivePage = function() {
    var container = document.getElementById('livePageContainer');
    if (!container) return;
    
    if (!container.querySelector('.live-page')) {
      container.innerHTML = '<div class="live-page">' +
        '<div class="live-header">' +
          '<h2>电视直播</h2>' +
          '<div class="live-controls">' +
            '<button class="live-add-btn" onclick="showAddLiveChannel()">添加直播源</button>' +
            '<button class="live-refresh-btn" onclick="refreshLiveChannels()">刷新</button>' +
          '</div>' +
        '</div>' +
        '<div class="live-categories" id="liveCategories"></div>' +
        '<div class="live-channels" id="liveChannels"></div>' +
      '</div>';
    }
    
    // 从数据库加载直播频道
    if (window.NCDB) {
      NCDB.getAllLiveChannels().then(function(channels) {
        liveState.channels = channels || [];
        groupChannels(liveState.channels);
        renderLiveCategories();
        renderLiveChannels();
      });
    } else {
      renderLiveCategories();
      renderLiveChannels();
    }
  };

  function groupChannels(channels) {
    liveState.groups = {};
    channels.forEach(function(ch) {
      var group = ch.group || '其他';
      if (!liveState.groups[group]) {
        liveState.groups[group] = [];
      }
      liveState.groups[group].push(ch);
    });
  }

  // ===== 从仓库获取直播源 =====
  function fetchLiveFromWarehouses() {
    if (!window.NCDB) return;
    
    NCDB.getAllWarehouses().then(function(warehouses) {
      if (!warehouses || !warehouses.length) {
        renderLivePage();
        return;
      }

      var fetchPromises = warehouses.map(function(w) {
        return fetchLiveFromWarehouse(w);
      });

      Promise.all(fetchPromises).then(function() {
        renderLivePage();
      });
    });
  }

  function fetchLiveFromWarehouse(warehouse) {
    if (!warehouse.url) return Promise.resolve();
    
    return new Promise(function(resolve) {
      // 获取仓库配置
      if (window.fetchWarehouseConfig) {
        fetchWarehouseConfig(warehouse.url).then(function(config) {
          if (config && config.lives) {
            config.lives.forEach(function(live) {
              if (live.url || live.api || live.path) {
                fetchLiveList(live.url || live.api || live.path, warehouse.id);
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

  window.fetchLiveList = function(url, fromSite) {
    if (window.NativeHttp && NativeHttp.httpGet) {
      try {
        var text = NativeHttp.httpGet(url);
        if (!text) return;
        if (String(text).indexOf('__ERROR__') === 0) return;
        parseAndSaveLiveChannels(text, fromSite);
        return;
      } catch(e) {}
    }
    if (window.fetchTextSmart) {
      fetchTextSmart(url).then(function(text) {
        parseAndSaveLiveChannels(text, fromSite);
      }).catch(function() {});
    } else {
      fetch(url, {cache: 'no-store'}).then(function(r) {
        if (!r.ok) throw 'HTTP ' + r.status;
        return r.text();
      }).then(function(text) {
        parseAndSaveLiveChannels(text, fromSite);
      }).catch(function() {});
    }
  };

  function parseAndSaveLiveChannels(text, fromSite) {
    var channels = [];
    
    // 检测格式
    if (text.indexOf('#EXTINF') >= 0) {
      // M3U格式
      channels = parseM3U(text);
    } else {
      // TXT格式
      channels = parseTXT(text);
    }

    if (channels.length && window.NCDB) {
      NCDB.saveLiveChannels(fromSite, channels).then(function() {
        // 重新加载
        NCDB.getAllLiveChannels().then(function(allChannels) {
          liveState.channels = allChannels || [];
          groupChannels(liveState.channels);
        });
      });
    }
  }

  // ===== M3U解析 =====
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

  // ===== TXT解析 =====
  function parseTXT(text) {
    var channels = [];
    var lines = text.split(/\r?\n/);
    var currentGroup = '直播';

    for (var i = 0; i < lines.length; i++) {
      var line = lines[i].trim();
      if (!line) continue;

      if (line.indexOf('#genre#') >= 0) {
        var commaIdx = line.indexOf(',');
        if (commaIdx > 0) {
          currentGroup = line.substring(0, commaIdx).trim();
        }
        continue;
      }

      if (line.startsWith('#')) {
        if (line.indexOf('group-title') >= 0) {
          var g = line.match(/group-title="([^"]*)"/);
          if (g) currentGroup = g[1];
        }
        continue;
      }

      if (line.indexOf(',') > 0) {
        var parts = line.split(',');
        var name = parts[0].trim();
        var url = parts.slice(1).join(',').trim();
        
        if (url && (url.indexOf('http') === 0 || url.indexOf('rtmp') === 0)) {
          var urlList = url.split('#');
          for (var j = 0; j < urlList.length; j++) {
            var u = urlList[j].trim();
            if (u) {
              channels.push({
                name: name + (urlList.length > 1 ? ' 线路' + (j + 1) : ''),
                group: currentGroup,
                url: u,
                logo: ''
              });
            }
          }
        }
      }
    }

    return channels;
  }

  // ===== 渲染直播页面 =====
  function renderLivePage() {
    var container = document.getElementById('livePageContainer');
    if (!container) return;

    var groups = Object.keys(liveState.groups);
    var allChannels = liveState.channels;

    container.innerHTML = 
      '<div class="live-page">' +
        '<div class="live-header">' +
          '<h3>直播</h3>' +
        '</div>' +
        '<div class="live-groups">' +
          '<div class="group-item ' + (liveState.currentGroup === '全部' ? 'active' : '') + '" onclick="switchLiveGroup(\'全部\')">全部</div>' +
          groups.map(function(g) {
            return '<div class="group-item ' + (liveState.currentGroup === g ? 'active' : '') + '" onclick="switchLiveGroup(\'' + g + '\')">' + g + '</div>';
          }).join('') +
        '</div>' +
        '<div class="live-channel-list" id="liveChannelList">' +
          renderChannelList() +
        '</div>' +
      '</div>';
  }

  function renderChannelList() {
    var channels = liveState.currentGroup === '全部' ? 
      liveState.channels : 
      (liveState.groups[liveState.currentGroup] || []);

    if (!channels.length) {
      return '<div class="live-empty">暂无直播频道<br><small>请添加直播源或从仓库获取</small></div>';
    }

    return channels.map(function(ch) {
      return '<div class="live-channel-item" onclick="playLiveChannel(\'' + ch.id + '\')">' +
        '<div class="channel-info">' +
          '<span class="channel-name">' + ch.name + '</span>' +
          '<span class="channel-group">' + ch.group + '</span>' +
        '</div>' +
        '<button class="channel-play-btn">▶</button>' +
      '</div>';
    }).join('');
  }

  window.switchLiveGroup = function(group) {
    liveState.currentGroup = group;
    var list = document.getElementById('liveChannelList');
    if (list) list.innerHTML = renderChannelList();
    
    // 更新分组高亮
    var items = document.querySelectorAll('.group-item');
    items.forEach(function(item) {
      item.classList.toggle('active', item.textContent === group);
    });
  };

  // ===== 播放直播 =====
  window.playLiveChannel = function(channelId) {
    var channel = liveState.channels.find(function(ch) { return ch.id == channelId; });
    if (!channel) {
      for (var g in liveState.groups) {
        var found = liveState.groups[g].find(function(ch) { return ch.id == channelId; });
        if (found) { channel = found; break; }
      }
    }

    if (!channel || !channel.url) {
      alert('频道地址无效');
      return;
    }

    if (window.EXOPlayer && EXOPlayer.isAvailable()) {
      EXOPlayer.playLive(channel.name, channel.url);
    } else if (window.exoPlayer && exoPlayer.play) {
      exoPlayer.play(JSON.stringify({
        title: channel.name,
        url: channel.url,
        isLive: true
      }));
    } else {
      var video = document.createElement('video');
      video.src = channel.url;
      video.controls = true;
      video.style.width = '100%';
      video.style.height = '100%';
      var modal = document.getElementById('videoModal');
      if (modal) {
        var playerWrap = document.getElementById('videoPlayerWrap');
        if (playerWrap) {
          playerWrap.appendChild(video);
          modal.style.display = 'flex';
          modal.classList.add('show');
          video.play().catch(function(e) {
            alert('播放失败: ' + e.message);
          });
        }
      }
    }
  };

  // ===== 直播源管理（在我的页面） =====
  window.showLiveSourceManager = function() {
    var msg = '直播源管理\n\n';
    msg += '当前频道数: ' + liveState.channels.length + '\n';
    msg += '分组数: ' + Object.keys(liveState.groups).length + '\n\n';
    msg += '1. 添加直播源URL\n2. 管理本地频道\n3. 从仓库重新获取\n\n输入序号：';
    
    var n = prompt(msg, '');
    if (!n) return;

    if (n === '1') {
      var url = prompt('请输入直播源URL（M3U或TXT格式）：');
      if (url) {
        fetchLiveList(url, 'manual');
        alert('正在获取直播源...');
      }
    } else if (n === '3') {
      fetchLiveFromWarehouses();
      alert('正在从仓库获取直播源...');
    }
  };

  // ===== 渲染直播分类和频道 =====
  window.renderLiveCategories = function() {
    var container = document.getElementById('liveCategories');
    if (!container) return;
    
    var groups = Object.keys(liveState.groups);
    if (groups.length === 0) {
      container.innerHTML = '<div style="color:#667788;padding:8px">暂无分组</div>';
      return;
    }
    
    container.innerHTML = '<button class="' + (liveState.currentGroup === '全部' ? 'active' : '') + '" onclick="window.switchLiveGroup&&switchLiveGroup(\'全部\')">全部</button>' +
      groups.map(function(g) {
        return '<button class="' + (liveState.currentGroup === g ? 'active' : '') + '" onclick="window.switchLiveGroup&&switchLiveGroup(\'' + g + '\')">' + g + '</button>';
      }).join('');
  };

  window.renderLiveChannels = function() {
    var container = document.getElementById('liveChannels');
    if (!container) return;
    
    var channels = liveState.currentGroup === '全部' ? 
      liveState.channels : 
      (liveState.groups[liveState.currentGroup] || []);
    
    if (!channels.length) {
      container.innerHTML = '<div style="text-align:center;color:#667788;padding:40px">暂无直播频道<br><small>请添加直播源或从仓库获取</small></div>';
      return;
    }
    
    container.innerHTML = channels.map(function(ch) {
      return '<div class="live-channel-item" onclick="playLiveChannel(\'' + ch.id + '\')">' +
        '<div class="live-channel-info"><b>' + ch.name + '</b><span>' + ch.group + '</span></div>' +
        '<button class="live-play-btn">播放</button>' +
      '</div>';
    }).join('');
  };

  // 初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setTimeout(initLivePage, 500);
    });
  } else {
    setTimeout(initLivePage, 500);
  }
})();
