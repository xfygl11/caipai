// NewCloud 多源搜索模块
// 支持精确/模糊匹配、列表/网格视图、多源并发搜索、线程数设置

(function(){
  var searchState = {
    keyword: '',
    exactMatch: false,
    listView: false,
    selectedSources: [],
    allSources: [],
    results: {},
    isLoading: false,
    currentSourceIdx: 0
  };

  // ===== 初始化搜索 =====
  window.initSearchPage = function() {
    var container = document.getElementById('searchPageContainer');
    if (!container) return;
    
    container.innerHTML = '<div class="search-container">' +
      '<div class="search-header">' +
        '<input type="text" id="searchInput" placeholder="输入影片名称..." class="search-input">' +
        '<div class="search-toolbar">' +
          '<button class="search-type-btn active" data-type="exact">精确</button>' +
          '<button class="search-type-btn" data-type="fuzzy">模糊</button>' +
          '<button class="search-select-all" onclick="selectAllResults()">全选</button>' +
          '<button class="search-select-none" onclick="deselectAllResults()">全不选</button>' +
        '</div>' +
      '</div>' +
      '<div class="search-results" id="searchResults"></div>' +
      '<div class="search-status" id="searchStatus">等待搜索...</div>' +
    '</div>';
  }

  // ===== 搜索控制 =====
  window.toggleExactMatch = function() {
    searchState.exactMatch = document.getElementById('exactMatchCheck').checked;
  };

  window.toggleListView = function() {
    searchState.listView = document.getElementById('listViewCheck').checked;
  };

  window.selectAllSources = function() {
    searchState.allSources.forEach(function(s) { s.selected = true; });
    updateSourceCheckboxes();
  };

  window.selectNoneSources = function() {
    searchState.allSources.forEach(function(s) { s.selected = false; });
    updateSourceCheckboxes();
  };

  window.toggleSource = function(id) {
    var source = searchState.allSources.find(function(s) { return s.id === id; });
    if (source) source.selected = !source.selected;
  };

  function updateSourceCheckboxes() {
    var checkboxes = document.querySelectorAll('.search-source-item input[type="checkbox"]');
    checkboxes.forEach(function(cb) {
      var id = parseInt(cb.getAttribute('data-id'));
      var source = searchState.allSources.find(function(s) { return s.id === id; });
      cb.checked = source ? source.selected : true;
    });
  }

  // ===== 执行搜索（并发线程控制） =====
  window.executeSearch = function() {
    var input = document.getElementById('searchInput');
    var keyword = input ? input.value.trim() : '';
    
    if (!keyword) {
      alert('请输入搜索关键词');
      return;
    }

    searchState.keyword = keyword;
    searchState.isLoading = true;
    searchState.results = {};

    // 获取选中的源
    var selectedSources = searchState.allSources.filter(function(s) { return s.selected; });
    if (!selectedSources.length) {
      alert('请至少选择一个搜索源');
      searchState.isLoading = false;
      return;
    }

    // 获取线程数
    var threads = window.NCDB ? NCDB.getSearchThreads() : 16;
    
    // 显示加载状态
    var resultsDiv = document.getElementById('searchResults');
    if (resultsDiv) {
      resultsDiv.style.display = 'block';
      resultsDiv.innerHTML = '<div class="search-loading">' +
        '<div class="search-loading-spinner"></div>' +
        '<p>正在搜索 ' + selectedSources.length + ' 个源（并发线程: ' + threads + '）...</p>' +
      '</div>';
    }

    // 并发搜索
    searchWithConcurrency(selectedSources, keyword, threads);
  };

  function searchWithConcurrency(sources, keyword, concurrency) {
    var results = {};
    var completed = 0;
    var total = sources.length;
    var queue = sources.slice();
    var activeCount = 0;

    function processNext() {
      if (queue.length === 0 || activeCount >= concurrency) return;
      
      var source = queue.shift();
      activeCount++;
      
      searchBySource(source, keyword).then(function(result) {
        results[source.id] = result;
      }).catch(function(e) {
        results[source.id] = { error: e.message || '搜索失败', movies: [] };
      }).finally(function() {
        activeCount--;
        completed++;
        updateSearchProgress(completed, total, results);
        processNext();
      });
    }

    // 启动初始并发
    for (var i = 0; i < Math.min(concurrency, sources.length); i++) {
      processNext();
    }
  }

  function searchBySource(source, keyword) {
    return new Promise(function(resolve, reject) {
      if (!source.api) {
        resolve({ sourceId: source.id, sourceName: source.name, movies: [], error: '无API地址' });
        return;
      }

      var params = keyword;
      if (source.type === 'json') {
        params = 'ac=detail&wd=' + encodeURIComponent(keyword);
      } else if (source.type === 'xml') {
        params = 'ac=detail&wd=' + encodeURIComponent(keyword);
      } else {
        params = 'wd=' + encodeURIComponent(keyword);
      }

      var url = source.api + (source.api.indexOf('?') >= 0 ? '&' : '?') + params;

      // 使用NativeHttp或fetch
      if (window.NativeHttp && NativeHttp.httpGet) {
        try {
          var text = NativeHttp.httpGet(url);
          if (!text) { reject('空响应'); return; }
          var data = JSON.parse(text);
          var movies = normalizeSearchResults(data, source.name);
          resolve({ sourceId: source.id, sourceName: source.name, movies: movies, error: null });
        } catch(e) {
          resolve({ sourceId: source.id, sourceName: source.name, movies: [], error: e.message });
        }
      } else {
        fetch(url, {cache: 'no-store'}).then(function(r) {
          if (!r.ok) throw new Error('HTTP ' + r.status);
          return r.json();
        }).then(function(data) {
          var movies = normalizeSearchResults(data, source.name);
          resolve({ sourceId: source.id, sourceName: source.name, movies: movies, error: null });
        }).catch(function(e) {
          resolve({ sourceId: source.id, sourceName: source.name, movies: [], error: e.message });
        });
      }
    });
  }

  function normalizeSearchResults(data, sourceName) {
    if (!data || !data.list) return [];
    
    return data.list.slice(0, 60).map(function(v) {
      return {
        id: String(v.vod_id || v.id || Math.random()),
        title: v.vod_name || v.name || '未知',
        pic: v.vod_pic || v.pic || '',
        type: v.type_name || sourceName,
        year: v.vod_year || '',
        tag: v.vod_remarks || v.note || '',
        cat: '搜索',
        source: sourceName,
        raw: v
      };
    });
  }

  function updateSearchProgress(completed, total, results) {
    var resultsDiv = document.getElementById('searchResults');
    if (!resultsDiv) return;

    // 统计有结果的源
    var sourcesWithResults = 0;
    var totalMovies = 0;
    var sourceList = [];

    for (var id in results) {
      var r = results[id];
      if (r.movies && r.movies.length > 0) {
        sourcesWithResults++;
        totalMovies += r.movies.length;
        sourceList.push({ id: id, name: r.sourceName, count: r.movies.length });
      }
    }

    if (completed < total) {
      resultsDiv.innerHTML = '<div class="search-loading">' +
        '<p>已搜索 ' + completed + '/' + total + ' 个源，' + sourcesWithResults + ' 个源有结果（共 ' + totalMovies + ' 部）</p>' +
      '</div>';
      return;
    }

    // 搜索完成，显示结果
    searchState.isLoading = false;
    searchState.results = results;
    searchState.currentSourceIdx = 0;
    
    renderSearchResults(sourceList, results);
  }

  function renderSearchResults(sourceList, results) {
    var resultsDiv = document.getElementById('searchResults');
    if (!resultsDiv) return;

    if (!sourceList.length) {
      resultsDiv.innerHTML = '<div class="search-empty">未找到任何结果</div>';
      return;
    }

    var currentSource = sourceList[0];
    var movies = results[currentSource.id] ? results[currentSource.id].movies : [];

    resultsDiv.innerHTML = 
      '<div class="search-result-page">' +
        '<div class="search-result-header">' +
          '<button onclick="goBackToSearch()" class="search-back-btn">← 返回</button>' +
          '<h3>搜索: ' + searchState.keyword + '</h3>' +
          '<span class="search-result-count">' + sourceList.length + ' 个源有结果 · ' + 
            sourceList.reduce(function(a, b) { return a + b.count; }, 0) + ' 部影片</span>' +
        '</div>' +
        '<div class="search-result-body">' +
          '<div class="search-source-list">' +
            '<div class="source-list-title">有结果的源</div>' +
            sourceList.map(function(s, i) {
              return '<div class="source-item ' + (i === 0 ? 'active' : '') + '" onclick="switchSearchSource(' + i + ')">' +
                '<span class="source-name">' + s.name + '</span>' +
                '<span class="source-count">' + s.count + '部</span>' +
              '</div>';
            }).join('') +
          '</div>' +
          '<div class="search-movie-list ' + (searchState.listView ? 'list-view' : 'grid-view') + '" id="searchMovieList">' +
            renderMovieGrid(movies) +
          '</div>' +
        '</div>' +
      '</div>';
  }

  function renderMovieGrid(movies) {
    if (!movies.length) return '<div class="search-empty">该源暂无结果</div>';
    
    if (searchState.listView) {
      return movies.map(function(m) {
        return '<div class="search-movie-item search-list-item">' +
          '<div class="search-movie-info">' +
            '<b>' + m.title + '</b>' +
            '<span>' + (m.year || '') + ' · ' + (m.tag || '') + '</span>' +
          '</div>' +
          '<button onclick="playFromSearch(\'' + m.id + '\')">播放</button>' +
        '</div>';
      }).join('');
    }

    return movies.map(function(m) {
      var img = m.pic ? 'background-image:url(\'' + m.pic + '\')' : '';
      return '<div class="search-movie-item tv-card" onclick="playFromSearch(\'' + m.id + '\')">' +
        '<div class="tv-poster" style="' + img + '">' +
          '<span class="tv-card-title">' + m.title + '</span>' +
        '</div>' +
        '<div class="tv-card-info">' + (m.year || '') + ' · ' + (m.tag || '') + '</div>' +
      '</div>';
    }).join('');
  }

  window.switchSearchSource = function(idx) {
    var sourceList = Object.keys(searchState.results).filter(function(id) {
      return searchState.results[id] && searchState.results[id].movies && searchState.results[id].movies.length > 0;
    });
    
    if (idx >= sourceList.length) idx = 0;
    searchState.currentSourceIdx = idx;
    
    var sourceId = sourceList[idx];
    var movies = searchState.results[sourceId] ? searchState.results[sourceId].movies : [];
    
    // 更新源列表高亮
    var sourceItems = document.querySelectorAll('.source-item');
    sourceItems.forEach(function(item, i) {
      item.classList.toggle('active', i === idx);
    });
    
    // 更新影片列表
    var movieList = document.getElementById('searchMovieList');
    if (movieList) {
      movieList.innerHTML = renderMovieGrid(movies);
    }
  };

  window.playFromSearch = function(movieId) {
    // 查找影片
    var sourceId = searchState.currentSourceIdx;
    var sourceKeys = Object.keys(searchState.results).filter(function(id) {
      return searchState.results[id] && searchState.results[id].movies && searchState.results[id].movies.length > 0;
    });
    
    if (sourceId >= sourceKeys.length) sourceId = 0;
    var key = sourceKeys[sourceId];
    var movies = searchState.results[key] ? searchState.results[key].movies : [];
    var movie = movies.find(function(m) { return m.id === movieId; });
    
    if (movie) {
      // 跳转到播放页面
      if (window.ExoPlayerWrapper && window.ExoPlayerWrapper.play) {
        // TODO: 获取播放URL
        alert('播放: ' + movie.title + '\n（需要获取播放地址后调用EXO播放器）');
      } else {
        alert('播放: ' + movie.title);
      }
    }
  };

  window.goBackToSearch = function() {
    var resultsDiv = document.getElementById('searchResults');
    if (resultsDiv) {
      resultsDiv.style.display = 'none';
    }
    // 显示搜索控制面板
    var controls = document.querySelector('.search-controls');
    if (controls) controls.style.display = 'block';
  };

  // ===== 搜索线程设置 =====
  window.showSearchSettings = function() {
    var currentThreads = window.NCDB ? NCDB.getSearchThreads() : 16;
    var msg = '设置搜索并发线程数：\n\n';
    msg += '当前: ' + currentThreads + '\n\n';
    msg += '选项:\n1. 16线程\n2. 24线程\n3. 32线程\n4. 64线程\n\n';
    msg += '输入序号（1-4）：';
    
    var n = prompt(msg, '1');
    if (!n) return;
    
    var threads = [16, 24, 32, 64][parseInt(n) - 1];
    if (!threads) {
      alert('无效的选项');
      return;
    }
    
    if (window.NCDB) {
      NCDB.setSearchThreads(threads);
    }
    alert('搜索线程数已设置为: ' + threads);
  };

  // 搜索类型切换
  window.toggleSearchType = function(type) {
    document.querySelectorAll('.search-type-btn').forEach(function(btn) {
      btn.classList.toggle('active', btn.getAttribute('data-type') === type);
    });
    var s = window.SEARCH_SETTINGS || (window.SEARCH_SETTINGS = {exact: false, viewMode: 'list'});
    s.exact = type === 'exact';
    if (window.NCDB) { window.SEARCH_SETTINGS = s; window.saveSearchSettings(s); }
  };

  // 全选/全不选 - 修复未声明变量
  window.selectAllResults = function() {
    var results = searchState.results;
    var sourceKeys = Object.keys(results);
    sourceKeys.forEach(function(id) {
      if (results[id] && results[id].movies) {
        results[id].movies.forEach(function(m) { m.selected = true; });
      }
    });
    var sourceList = [];
    sourceKeys.forEach(function(id) {
      if (results[id] && results[id].movies && results[id].movies.length > 0) {
        sourceList.push({ id: id, name: results[id].sourceName, count: results[id].movies.length });
      }
    });
    if (sourceList.length > 0) renderSearchResults(sourceList, results);
  };
  
  window.deselectAllResults = function() {
    var results = searchState.results;
    var sourceKeys = Object.keys(results);
    sourceKeys.forEach(function(id) {
      if (results[id] && results[id].movies) {
        results[id].movies.forEach(function(m) { m.selected = false; });
      }
    });
    var sourceList = [];
    sourceKeys.forEach(function(id) {
      if (results[id] && results[id].movies && results[id].movies.length > 0) {
        sourceList.push({ id: id, name: results[id].sourceName, count: results[id].movies.length });
      }
    });
    if (sourceList.length > 0) renderSearchResults(sourceList, results);
  };

  // 开始多源搜索 - 修复currentSite未声明问题
  window.startMultiSourceSearch = function() {
    var keyword = document.getElementById('searchInput')?.value?.trim();
    if (!keyword) { alert('请输入搜索关键词'); return; }
    // 检查是否有选中的源
    var selectedSources = searchState.allSources.filter(function(s) { return s.selected; });
    if (!selectedSources.length) { alert('请至少选择一个站点'); return; }
    searchWithConcurrency(selectedSources, keyword, window.NCDB ? NCDB.getSearchThreads() : 16);
  };

  // 初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setTimeout(initSearchPage, 500);
    });
  } else {
    setTimeout(initSearchPage, 500);
  }
})();
