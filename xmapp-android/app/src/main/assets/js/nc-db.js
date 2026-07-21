// NewCloud 本地数据库模块：localStorage 封装（无 IndexedDB）

var NCDB = (function() {
  var _CAT_ALIAS = {'电影':'电影片','剧集':'连续剧','连续剧':'连续剧','电视剧':'连续剧','综艺':'综艺片','综艺片':'综艺片','动漫':'动漫片','动漫片':'动漫片','电影片':'电影片'};
  function _normalizeCatName(n) { return _CAT_ALIAS[String(n || '').trim()] || String(n || '').trim() || '推荐'; }

  function _get(key) { try { return JSON.parse(localStorage.getItem(key)); } catch(e) { return null; } }
  function _set(key, val) { try { localStorage.setItem(key, JSON.stringify(val)); } catch(e) {} }
  function _getList(key) { return _get(key) || []; }
  function _setList(key, arr) { _set(key, arr); }

  // ===== 采集源 =====
  var saveSource = function(name, url, base) {
    var sources = _getList('nc_sources');
    var existing = sources.find(function(s) { return s.base === (base || url); });
    if (existing) {
      existing.name = name || '未知源';
      existing.url = url;
      existing.lastUpdate = Date.now();
    } else {
      sources.push({ id: 'src_' + Date.now(), name: name || '未知源', url: url, base: base || url, lastUpdate: Date.now() });
    }
    _set('nc_sources', sources);
    return Promise.resolve(existing ? existing.id : sources[sources.length-1].id);
  };

  var getSources = function() { return Promise.resolve(_getList('nc_sources')); };
  var getSourceByBase = function(base) {
    var sources = _getList('nc_sources');
    return Promise.resolve(sources.find(function(s) { return s.base === base }) || null);
  };

  // ===== 分类 =====
  var saveCategories = function(sourceId, categories) {
    var all = _getList('nc_categories');
    var catList = categories.map(function(c) {
      return { sourceId: sourceId, name: _normalizeCatName(c.type_name || c.name || '未知'), rawName: c.type_name || c.name || '', typeId: String(c.type_id || c.id || ''), parentId: String(c.type_pid || c.parentId || '0'), updateTime: Date.now() };
    });
    var other = all.filter(function(c) { return c.sourceId !== sourceId; });
    _set('nc_categories', other.concat(catList));
  };

  var getCategories = function(sourceId) { return Promise.resolve(_getList('nc_categories').filter(function(c) { return c.sourceId == sourceId; })); };
  var getAllCategories = function() { return Promise.resolve(_getList('nc_categories')); };
  var getDistinctCategoryNames = function() {
    var cats = _getList('nc_categories');
    var names = [];
    cats.forEach(function(c) { if (names.indexOf(c.name) === -1) names.push(c.name); });
    return Promise.resolve(names);
  };

  // ===== 影片 =====
  var saveMovies = function(sourceId, category, movies) {
    var all = _getList('nc_movies_' + sourceId);
    var now = Date.now();
    var ids = {};
    all.forEach(function(m) { ids[m.vodId] = true; });
    movies.forEach(function(v) {
      var vid = String(v.id || v.vod_id || '');
      if (!ids[vid]) {
        all.push({ vodId: vid, category: category, title: v.title || '', pic: v.pic || '', tag: v.tag || '', type: v.type || '', year: v.year || '', area: v.area || '', actor: v.actor || '', director: v.director || '', score: v.score || '', quality: v.quality || '', play: v.play || '', desc: v.desc || '', raw: v.raw || {}, updateTime: now });
        ids[vid] = true;
      }
    });
    _set('nc_movies_' + sourceId, all);
  };

  var getMovies = function(sourceId, category, limit) {
    var all = _getList('nc_movies_' + sourceId);
    var filtered = category ? all.filter(function(m) { return m.category === category; }) : all;
    if (limit) filtered = filtered.slice(0, limit);
    return Promise.resolve(filtered);
  };

  var saveMoviesIncremental = function(sourceId, category, movies) {
    var all = _getList('nc_movies_' + sourceId);
    var now = Date.now();
    var ids = {};
    all.forEach(function(m) { ids[m.vodId] = true; });
    var added = [];
    movies.forEach(function(v) {
      var vid = String(v.id || v.vod_id || '');
      if (!ids[vid]) {
        all.push({ vodId: vid, category: category, title: v.title || '', pic: v.pic || '', tag: v.tag || '', raw: v.raw || {}, updateTime: now });
        ids[vid] = true;
        added.push(vid);
      }
    });
    _set('nc_movies_' + sourceId, all);
    return Promise.resolve(added);
  };

  var hasMovies = function(sourceId, category) {
    var all = _getList('nc_movies_' + sourceId);
    return Promise.resolve(category ? all.some(function(m) { return m.category === category; }) : all.length > 0);
  };

  var getCategoriesWithData = function(sourceId) {
    var all = _getList('nc_movies_' + sourceId);
    var cats = {};
    all.forEach(function(m) { cats[m.category] = true; });
    return Promise.resolve(Object.keys(cats));
  };

  var clearSource = function(sourceId) {
    _set('nc_movies_' + sourceId, []);
    return Promise.resolve();
  };

  var getStats = function() {
    var sources = _getList('nc_sources');
    var total = 0;
    sources.forEach(function(s) { total += (_getList('nc_movies_' + s.id) || []).length; });
    return Promise.resolve({ sources: sources.length, movies: total });
  };

  // ===== 仓库 =====
  var getWarehouseCategories = function() { return Promise.resolve(_getList('nc_warehouse_cats')); };
  var saveWarehouseCategory = function(name) {
    var cats = _getList('nc_warehouse_cats');
    cats.push({ id: 'wc_' + Date.now(), name: name || '默认分类', createdAt: Date.now() });
    _set('nc_warehouse_cats', cats);
    return Promise.resolve(cats[cats.length-1].id);
  };
  var deleteWarehouseCategory = function(id) {
    _set('nc_warehouse_cats', _getList('nc_warehouse_cats').filter(function(c) { return c.id !== id; }));
    return Promise.resolve();
  };

  var saveWarehouse = function(categoryId, name, url) {
    var list = _getList('nc_warehouses');
    var item = { id: 'wh_' + Date.now(), categoryId: categoryId || 1, name: name || '默认仓库', url: url, type: 'tvbox', lastFetched: null, createdAt: Date.now() };
    list.push(item);
    _set('nc_warehouses', list);
    return Promise.resolve(item.id);
  };

  var getWarehousesByCategory = function(categoryId) { return Promise.resolve(_getList('nc_warehouses').filter(function(w) { return w.categoryId == categoryId; })); };
  var getAllWarehouses = function() { return Promise.resolve(_getList('nc_warehouses')); };
  var deleteWarehouse = function(id) { _set('nc_warehouses', _getList('nc_warehouses').filter(function(w) { return w.id !== id; })); return Promise.resolve(); };
  var updateWarehouseLastFetched = function(id, timestamp) {
    var list = _getList('nc_warehouses');
    var w = list.find(function(x) { return x.id === id; });
    if (w) { w.lastFetched = timestamp; _set('nc_warehouses', list); }
    return Promise.resolve();
  };

  // ===== 站点配置 =====
  var _siteIdCounter = 0;
  var _getNextSiteId = function() {
    _siteIdCounter++;
    return 'sc_' + Date.now() + '_' + _siteIdCounter;
  };
  var saveSiteConfig = function(warehouseId, siteData) {
    var list = _getList('nc_site_configs');
    var item = { id: _getNextSiteId(), warehouseId: warehouseId || null, name: siteData.name || '未命名站点', key: siteData.key || '', api: siteData.api || '', type: siteData.type || 'json', timeout: siteData.timeout || 10, searchable: siteData.searchable || 0, quickSearch: siteData.quickSearch || 0, categories: siteData.categories || [], ext: siteData.ext || {}, sourceType: siteData.sourceType || 'warehouse', createdAt: Date.now() };
    list.push(item);
    _set('nc_site_configs', list);
    return Promise.resolve(item.id);
  };

  var getSitesByWarehouse = function(warehouseId) { return Promise.resolve(_getList('nc_site_configs').filter(function(s) { return s.warehouseId == warehouseId; })); };
  var getLocalSites = function() { return Promise.resolve(_getList('nc_site_configs').filter(function(s) { return s.sourceType === 'local'; })); };
  var getAllSites = function() { return Promise.resolve(_getList('nc_site_configs')); };
  var deleteSiteConfig = function(id) { _set('nc_site_configs', _getList('nc_site_configs').filter(function(s) { return s.id !== id; })); return Promise.resolve(); };
  var clearAllSiteConfigs = function() { _set('nc_site_configs', []); return Promise.resolve(); };

  // ===== 直播源管理 =====
  var getLiveSourceNames = function() {
    var sources = _getList('nc_live_sources');
    var names = [];
    sources.forEach(function(s) { if (names.indexOf(s.name) === -1) names.push(s.name); });
    return Promise.resolve(names);
  };

  var saveLiveSource = function(name, url) {
    var sources = _getList('nc_live_sources');
    var existing = sources.find(function(s) { return s.name === name; });
    if (existing) {
      existing.url = url;
      existing.lastUpdate = Date.now();
    } else {
      sources.push({ id: 'ls_' + Date.now(), name: name, url: url, lastUpdate: Date.now() });
    }
    _set('nc_live_sources', sources);
    return Promise.resolve(existing ? existing.id : sources[sources.length-1].id);
  };

  var getLiveSources = function() { return Promise.resolve(_getList('nc_live_sources')); };
  var deleteLiveSource = function(name) { _set('nc_live_sources', _getList('nc_live_sources').filter(function(s) { return s.name !== name; })); return Promise.resolve(); };
  var clearAllLiveSources = function() { _set('nc_live_sources', []); return Promise.resolve(); };

  // ===== 直播频道 =====
  var saveLiveChannels = function(fromSite, channels) {
    var all = _getList('nc_live_channels');
    all = all.filter(function(c) { return c.fromSite !== fromSite; });
    channels.forEach(function(ch) {
      all.push({ id: 'lc_' + Date.now() + '_' + Math.random().toString(36).substr(2,4), fromSite: fromSite, name: ch.name || '未知频道', url: ch.url || '', group: ch.group || '其他', logo: ch.logo || '', createdAt: Date.now() });
    });
    _set('nc_live_channels', all);
  };

  var getLiveChannelsByGroup = function() {
    var all = _getList('nc_live_channels');
    var groups = {};
    all.forEach(function(c) { if (!groups[c.group]) groups[c.group] = []; groups[c.group].push(c); });
    return Promise.resolve(groups);
  };

  var getAllLiveChannels = function() { return Promise.resolve(_getList('nc_live_channels')); };
  var getAllLiveChannelsSync = function() { return _getList('nc_live_channels'); };
  var deleteLiveChannel = function(id) { _set('nc_live_channels', _getList('nc_live_channels').filter(function(c) { return c.id !== id; })); return Promise.resolve(); };

  var getLiveChannels = function() { return Promise.resolve(_getList('nc_live_channels')); };
  var saveLiveChannel = function(channel) {
    var all = _getList('nc_live_channels');
    all.push({ id: 'live_' + Date.now() + '_' + Math.random().toString(36).substr(2,6), fromSite: null, name: channel.name || '未命名', url: channel.url || '', group: channel.group || '默认', logo: channel.logo || '', timestamp: Date.now() });
    _set('nc_live_channels', all);
    return Promise.resolve();
  };

  // ===== 搜索配置 =====
  var getSearchThreads = function() { try { return parseInt(localStorage.getItem('search_threads') || '16'); } catch(e) { return 16; } };
  var setSearchThreads = function(count) { try { localStorage.setItem('search_threads', String(count)); } catch(e) {} };

  // ===== 历史记录 =====
  var getHistoryCount = function() {
    var his = _getList('nc_history');
    var b = document.getElementById('mineHistoryCount');
    if (b) { b.textContent = his.length; }
    return Promise.resolve(his.length);
  };

  var saveHistory = function(item) {
    var his = _getList('nc_history');
    his = his.filter(function(h) { return h.vodId !== item.vodId; });
    his.unshift(item);
    if (his.length > 200) his = his.slice(0, 200);
    _set('nc_history', his);
  };

  var clearHistory = function() { _set('nc_history', []); return Promise.resolve(); };
  var clearFavorites = function() { _set('nc_favorites', []); return Promise.resolve(); };

  // ===== 收藏（独立存储）=====
  var getFavorites = function() { return Promise.resolve(_getList('nc_favorites')); };
  var saveFavorite = function(item) {
    var favs = _getList('nc_favorites');
    favs = favs.filter(function(f) { return f.vodId !== item.vodId; });
    favs.unshift(item);
    _set('nc_favorites', favs);
    return Promise.resolve();
  };
  var deleteFavorite = function(vodId) { _set('nc_favorites', _getList('nc_favorites').filter(function(f) { return f.vodId !== vodId; })); return Promise.resolve(); };

  // ===== 搜索历史 =====
  var getSearchHistory = function() { return Promise.resolve(_getList('nc_search_hist')); };
  var saveSearchHistory = function(keyword) {
    var hist = _getList('nc_search_hist');
    hist = hist.filter(function(h) { return h !== keyword; });
    hist.unshift(keyword);
    if (hist.length > 50) hist = hist.slice(0, 50);
    _set('nc_search_hist', hist);
  };
  var clearSearchHistory = function() { _set('nc_search_hist', []); return Promise.resolve(); };

  // ===== init 无操作 =====
  var init = function() { return Promise.resolve(); };

  return {
    init: init,
    saveSource: saveSource, getSources: getSources, getSourceByBase: getSourceByBase,
    saveCategories: saveCategories, getCategories: getCategories, getAllCategories: getAllCategories,
    getDistinctCategoryNames: getDistinctCategoryNames,
    saveMovies: saveMovies, getMovies: getMovies, saveMoviesIncremental: saveMoviesIncremental,
    hasMovies: hasMovies, getCategoriesWithData: getCategoriesWithData, clearSource: clearSource, getStats: getStats,
    getWarehouseCategories: getWarehouseCategories, saveWarehouseCategory: saveWarehouseCategory,
    deleteWarehouseCategory: deleteWarehouseCategory, saveWarehouse: saveWarehouse,
    getWarehousesByCategory: getWarehousesByCategory, getAllWarehouses: getAllWarehouses,
    deleteWarehouse: deleteWarehouse, updateWarehouseLastFetched: updateWarehouseLastFetched,
    saveSiteConfig: saveSiteConfig, getSitesByWarehouse: getSitesByWarehouse,
    getLocalSites: getLocalSites, getAllSites: getAllSites, deleteSiteConfig: deleteSiteConfig,
    clearAllSiteConfigs: clearAllSiteConfigs,
    saveLiveChannels: saveLiveChannels, getLiveChannelsByGroup: getLiveChannelsByGroup,
    getAllLiveChannels: getAllLiveChannels, getAllLiveChannelsSync: getAllLiveChannelsSync, deleteLiveChannel: deleteLiveChannel,
    getLiveChannels: getLiveChannels, saveLiveChannel: saveLiveChannel,
    getLiveSourceNames: getLiveSourceNames, saveLiveSource: saveLiveSource,
    getLiveSources: getLiveSources, deleteLiveSource: deleteLiveSource, clearAllLiveSources: clearAllLiveSources,
    getSearchThreads: getSearchThreads, setSearchThreads: setSearchThreads,
    getHistoryCount: getHistoryCount, saveHistory: saveHistory, clearHistory: clearHistory,
    clearFavorites: clearFavorites,
    getFavorites: getFavorites, saveFavorite: saveFavorite, deleteFavorite: deleteFavorite,
    getSearchHistory: getSearchHistory, saveSearchHistory: saveSearchHistory, clearSearchHistory: clearSearchHistory
  };
})();

// Expose to global scope for Android WebView compatibility
window.NCDB = NCDB;
