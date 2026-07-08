// NewCloud 本地数据库模块：IndexedDB 封装
// 存储采集源、分类、影片、仓库、站点、直播频道

var NCDB = (function() {
  var DB_NAME = 'NewCloudDB';
  var DB_VERSION = 2;
  var db = null;

  // 独立的分类名标准化（不依赖外部模块）
  var _CAT_ALIAS = {'电影':'电影片','剧集':'连续剧','连续剧':'连续剧','电视剧':'连续剧','综艺':'综艺片','综艺片':'综艺片','动漫':'动漫片','动漫片':'动漫片','电影片':'电影片'};
  function _normalizeCatName(n) { return _CAT_ALIAS[String(n || '').trim()] || String(n || '').trim() || '推荐'; }

  function openDB() {
    return new Promise(function(resolve, reject) {
      if (db) { resolve(db); return; }
      var req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onerror = function() { reject(req.error); };
      req.onsuccess = function() { db = req.result; resolve(db); };
      req.onupgradeneeded = function(e) {
        var d = e.target.result;
        // sources: 采集源信息
        if (!d.objectStoreNames.contains('sources')) {
          var sStore = d.createObjectStore('sources', { keyPath: 'id', autoIncrement: true });
          sStore.createIndex('url', 'url', { unique: false });
          sStore.createIndex('base', 'base', { unique: false });
        }
        // categories: 分类信息（按源分组）
        if (!d.objectStoreNames.contains('categories')) {
          var cStore = d.createObjectStore('categories', { keyPath: 'id', autoIncrement: true });
          cStore.createIndex('sourceId', 'sourceId', { unique: false });
          cStore.createIndex('sourceId_name', ['sourceId', 'name'], { unique: false });
        }
        // movies: 影片数据
        if (!d.objectStoreNames.contains('movies')) {
          var mStore = d.createObjectStore('movies', { keyPath: 'id', autoIncrement: true });
          mStore.createIndex('sourceId', 'sourceId', { unique: false });
          mStore.createIndex('category', 'category', { unique: false });
          mStore.createIndex('sourceId_category', ['sourceId', 'category'], { unique: false });
          mStore.createIndex('vodId', 'vodId', { unique: false });
          mStore.createIndex('updateTime', 'updateTime', { unique: false });
        }
        // warehouses: 仓库管理
        if (!d.objectStoreNames.contains('warehouses')) {
          var wStore = d.createObjectStore('warehouses', { keyPath: 'id', autoIncrement: true });
          wStore.createIndex('categoryId', 'categoryId', { unique: false });
          wStore.createIndex('url', 'url', { unique: false });
          wStore.createIndex('name', 'name', { unique: false });
        }
        // warehouseCategories: 仓库分类
        if (!d.objectStoreNames.contains('warehouseCategories')) {
          d.createObjectStore('warehouseCategories', { keyPath: 'id', autoIncrement: true });
        }
        // siteConfigs: 站点配置
        if (!d.objectStoreNames.contains('siteConfigs')) {
          var scStore = d.createObjectStore('siteConfigs', { keyPath: 'id', autoIncrement: true });
          scStore.createIndex('warehouseId', 'warehouseId', { unique: false });
          scStore.createIndex('api', 'api', { unique: false });
          scStore.createIndex('type', 'type', { unique: false });
          scStore.createIndex('name', 'name', { unique: false });
        }
        // liveChannels: 直播频道
        if (!d.objectStoreNames.contains('liveChannels')) {
          var lcStore = d.createObjectStore('liveChannels', { keyPath: 'id', autoIncrement: true });
          lcStore.createIndex('fromSite', 'fromSite', { unique: false });
          lcStore.createIndex('group', 'group', { unique: false });
          lcStore.createIndex('name', 'name', { unique: false });
        }
      };
    });
  }

  function withStore(storeName, mode) {
    return openDB().then(function(d) {
      return d.transaction(storeName, mode).objectStore(storeName);
    });
  }

  function promisifyRequest(req) {
    return new Promise(function(resolve, reject) {
      req.onsuccess = function() { resolve(req.result); };
      req.onerror = function() { reject(req.error); };
    });
  }

  return {
    init: function() { return openDB(); },

    // ===== 采集源操作 =====
    saveSource: function(name, url, base) {
      return openDB().then(function(d) {
        var tx = d.transaction('sources', 'readwrite');
        var store = tx.objectStore('sources');
        var idx = store.index('base');
        return promisifyRequest(idx.get(base || url)).then(function(existing) {
          var data = {
            name: name || '未知源',
            url: url,
            base: base || url,
            lastUpdate: Date.now()
          };
          if (existing) {
            data.id = existing.id;
            store.put(data);
            return existing.id;
          } else {
            return promisifyRequest(store.add(data));
          }
        });
      });
    },

    getSources: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('sources', 'readonly').objectStore('sources').getAll());
      });
    },

    getSourceByBase: function(base) {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('sources', 'readonly').objectStore('sources').index('base').get(base));
      });
    },

    // ===== 分类操作 =====
    saveCategories: function(sourceId, categories) {
      return openDB().then(function(d) {
        var tx = d.transaction('categories', 'readwrite');
        var store = tx.objectStore('categories');
        // 先删除该源旧分类 - 使用 getAllKeys + delete 替代递归游标
        var idx = store.index('sourceId');
        var range = IDBKeyRange.only(sourceId);
        return promisifyRequest(idx.getAllKeys(range)).then(function(keys) {
          keys.forEach(function(key) { store.delete(key) });
          return Promise.resolve();
        }).then(function() {
          categories.forEach(function(c) {
            store.add({
              sourceId: sourceId,
              name: _normalizeCatName(c.type_name || c.name || '未知'),
              rawName: c.type_name || c.name || '',
              typeId: String(c.type_id || c.id || ''),
              parentId: String(c.type_pid || c.parentId || '0'),
              updateTime: Date.now()
            });
          });
          return categories.length;
        });
      });
    },

    getCategories: function(sourceId) {
      return openDB().then(function(d) {
        var store = d.transaction('categories', 'readonly').objectStore('categories');
        return promisifyRequest(store.index('sourceId').getAll(sourceId));
      });
    },

    getAllCategories: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('categories', 'readonly').objectStore('categories').getAll());
      });
    },

    // 获取所有源中已有的分类名（去重）
    getDistinctCategoryNames: function() {
      return this.getAllCategories().then(function(list) {
        var seen = {}, out = [];
        list.forEach(function(c) {
          var n = c.name;
          if (n && n !== '推荐' && !seen[n]) { seen[n] = 1; out.push(n); }
        });
        return out;
      });
    },

    // ===== 影片操作 =====
    saveMovies: function(sourceId, category, movies) {
      return openDB().then(function(d) {
        var tx = d.transaction('movies', 'readwrite');
        var store = tx.objectStore('movies');
        var idx = store.index('sourceId_category');
        var cat = _normalizeCatName(category || '推荐');
        var range = IDBKeyRange.only([sourceId, cat]);
        // 删除该源+分类的旧数据 - 使用 getAllKeys + delete 替代递归游标
        return promisifyRequest(idx.getAllKeys(range)).then(function(keys) {
          keys.forEach(function(key) { store.delete(key) });
          return Promise.resolve();
        }).then(function() {
          var now = Date.now();
          movies.forEach(function(v) {
            store.add({
              sourceId: sourceId,
              category: cat,
              vodId: String(v.id || v.vod_id || ''),
              title: v.title || '',
              pic: v.pic || '',
              tag: v.tag || '',
              type: v.type || '',
              year: v.year || '',
              area: v.area || '',
              actor: v.actor || '',
              director: v.director || '',
              score: v.score || '',
              quality: v.quality || '',
              play: v.play || '',
              desc: v.desc || '',
              raw: JSON.stringify(v.raw || {}),
              updateTime: now
            });
          });
          return movies.length;
        });
      });
    },

    getMovies: function(sourceId, category, limit) {
      return openDB().then(function(d) {
        var store = d.transaction('movies', 'readonly').objectStore('movies');
        var idx = store.index('sourceId_category');
        var cat = _normalizeCatName(category || '推荐');
        var range = IDBKeyRange.only([sourceId, cat]);
        return promisifyRequest(idx.getAll(range)).then(function(list) {
          if (limit && limit > 0) list = list.slice(0, limit);
          return list.map(function(m) {
            return {
              id: m.vodId,
              cat: m.category,
              title: m.title,
              pic: m.pic,
              tag: m.tag,
              type: m.type,
              year: m.year,
              area: m.area,
              actor: m.actor,
              director: m.director,
              score: m.score,
              quality: m.quality,
              play: m.play,
              desc: m.desc,
              raw: (function() { try { return JSON.parse(m.raw); } catch(e) { return {}; } })()
            };
          });
        });
      });
    },

    // 增量保存：只添加数据库中不存在的影片，返回实际新增数量
    saveMoviesIncremental: function(sourceId, category, movies) {
      return openDB().then(function(d) {
        var store = d.transaction('movies', 'readwrite').objectStore('movies');
        var idx = store.index('sourceId_category');
        var cat = _normalizeCatName(category || '推荐');
        var range = IDBKeyRange.only([sourceId, cat]);
        return promisifyRequest(idx.getAll(range)).then(function(existing) {
          var seen = {};
          existing.forEach(function(m) { if(m.vodId) seen[m.vodId] = 1; });
          var now = Date.now();
          var added = 0;
          movies.forEach(function(v) {
            var vid = String(v.id || v.vod_id || '');
            if (!vid || seen[vid]) return;
            seen[vid] = 1;
            store.add({
              sourceId: sourceId,
              category: cat,
              vodId: vid,
              title: v.title || '',
              pic: v.pic || '',
              tag: v.tag || '',
              type: v.type || '',
              year: v.year || '',
              area: v.area || '',
              actor: v.actor || '',
              director: v.director || '',
              score: v.score || '',
              quality: v.quality || '',
              play: v.play || '',
              desc: v.desc || '',
              raw: JSON.stringify(v.raw || {}),
              updateTime: now
            });
            added++;
          });
          return added;
        });
      });
    },

    // 检查某分类是否有数据
    hasMovies: function(sourceId, category) {
      return openDB().then(function(d) {
        var store = d.transaction('movies', 'readonly').objectStore('movies');
        var idx = store.index('sourceId_category');
        var range = IDBKeyRange.only([sourceId, _normalizeCatName(category || '推荐')]);
        return promisifyRequest(idx.count(range)).then(function(c) { return c > 0; });
      });
    },

    // 获取所有有数据的分类
    getCategoriesWithData: function(sourceId) {
      return openDB().then(function(d) {
        var store = d.transaction('movies', 'readonly').objectStore('movies');
        return promisifyRequest(store.index('sourceId').openCursor(sourceId)).then(function collect(cursor, cats) {
          cats = cats || {};
          if (!cursor) return Object.keys(cats);
          cats[cursor.value.category] = 1;
          return promisifyRequest(cursor.continue()).then(function(next) { return collect(next, cats); });
        });
      });
    },

    // 清空某源数据
    clearSource: function(sourceId) {
      return openDB().then(function(d) {
        var tx = d.transaction(['movies', 'categories'], 'readwrite');
        var promises = [];
        ['movies', 'categories'].forEach(function(name) {
          var store = tx.objectStore(name);
          var idx = store.index('sourceId');
          var range = IDBKeyRange.only(sourceId);
          var keyPromise = promisifyRequest(idx.getAllKeys(range)).then(function(keys) {
            keys.forEach(function(key) { store.delete(key) });
          });
          keyPromise.catch(function(e) { console.error('clearSource delete error:', e) });
          promises.push(keyPromise);
        });
        return Promise.all(promises).then(function() {
          return promisifyRequest(tx);
        });
      });
    },

    // 删除过期数据（默认30天）
    deleteOldMovies: function(days) {
      days = days || 30;
      var cutoff = Date.now() - days * 24 * 3600 * 1000;
      return openDB().then(function(d) {
        var store = d.transaction('movies', 'readwrite').objectStore('movies');
        var idx = store.index('updateTime');
        var range = IDBKeyRange.upperBound(cutoff);
        return promisifyRequest(idx.getAllKeys(range)).then(function(keys) {
          keys.forEach(function(key) { store.delete(key) });
          return keys.length;
        }).catch(function(e) { console.error('deleteOldMovies error:', e) });
      });
    },

    // 统计信息
    getStats: function() {
      return openDB().then(function(d) {
        var out = { sources: 0, categories: 0, movies: 0 };
        return promisifyRequest(d.transaction('sources', 'readonly').objectStore('sources').count()).then(function(c) {
          out.sources = c;
          return promisifyRequest(d.transaction('categories', 'readonly').objectStore('categories').count());
        }).then(function(c) {
          out.categories = c;
          return promisifyRequest(d.transaction('movies', 'readonly').objectStore('movies').count());
        }).then(function(c) {
          out.movies = c;
          return out;
        });
      });
    },

    // ==================== 仓库管理 ====================
    getWarehouseCategories: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('warehouseCategories', 'readonly').objectStore('warehouseCategories').getAll());
      });
    },

    saveWarehouseCategory: function(name) {
      return openDB().then(function(d) {
        var tx = d.transaction('warehouseCategories', 'readwrite');
        var store = tx.objectStore('warehouseCategories');
        return promisifyRequest(store.add({ name: name || '默认分类', createdAt: Date.now() }));
      });
    },

    deleteWarehouseCategory: function(id) {
      return openDB().then(function(d) {
        var tx = d.transaction(['warehouseCategories', 'warehouses'], 'readwrite');
        tx.objectStore('warehouseCategories').delete(id);
        // 同时删除该分类下的所有仓库
        var store = tx.objectStore('warehouses');
        var idx = store.index('categoryId');
        var range = IDBKeyRange.only(id);
        return promisifyRequest(idx.getAllKeys(range)).then(function(keys) {
          keys.forEach(function(key) { store.delete(key) });
        }).catch(function(e) { console.error('deleteWarehouseCategory error:', e) });
      });
    },

    saveWarehouse: function(categoryId, name, url) {
      return openDB().then(function(d) {
        var tx = d.transaction('warehouses', 'readwrite');
        var store = tx.objectStore('warehouses');
        return promisifyRequest(store.add({
          categoryId: categoryId || 1,
          name: name || '未命名仓库',
          url: url || '',
          type: 'tvbox',
          lastFetched: null,
          createdAt: Date.now()
        }));
      });
    },

    getWarehousesByCategory: function(categoryId) {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('warehouses', 'readonly').objectStore('warehouses').index('categoryId').getAll(IDBKeyRange.only(categoryId)));
      });
    },

    getAllWarehouses: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('warehouses', 'readonly').objectStore('warehouses').getAll());
      });
    },

    deleteWarehouse: function(id) {
      return openDB().then(function(d) {
        var tx = d.transaction(['warehouses', 'siteConfigs'], 'readwrite');
        tx.objectStore('warehouses').delete(id);
        // 同时删除该仓库下的站点
        var store = tx.objectStore('siteConfigs');
        var idx = store.index('warehouseId');
        var range = IDBKeyRange.only(id);
        return promisifyRequest(idx.getAllKeys(range)).then(function(keys) {
          keys.forEach(function(key) { store.delete(key) });
        }).catch(function(e) { console.error('deleteWarehouse error:', e) });
      });
    },

    updateWarehouseLastFetched: function(id, timestamp) {
      return openDB().then(function(d) {
        var tx = d.transaction('warehouses', 'readwrite');
        var store = tx.objectStore('warehouses');
        return promisifyRequest(store.index('id').get(id)).then(function(existing) {
          if (existing) {
            existing.lastFetched = timestamp;
            store.put(existing);
          }
        });
      });
    },

    // ==================== 站点管理 ====================
    saveSiteConfig: function(warehouseId, siteData) {
      return openDB().then(function(d) {
        var tx = d.transaction('siteConfigs', 'readwrite');
        var store = tx.objectStore('siteConfigs');
        // 先检查是否已存在
        return promisifyRequest(store.index('api').get(siteData.api || '')).then(function(existing) {
          if (existing && existing.id !== siteData.id) {
            // 同名不同ID，删除旧的
            store.delete(existing.id);
          }
          store.add({
            warehouseId: warehouseId || null,
            name: siteData.name || '未命名站点',
            key: siteData.key || '',
            api: siteData.api || '',
            type: siteData.type || 'json',
            timeout: siteData.timeout || 10,
            searchable: siteData.searchable || 0,
            quickSearch: siteData.quickSearch || 0,
            categories: siteData.categories || [],
            ext: siteData.ext || {},
            sourceType: siteData.sourceType || 'warehouse', // warehouse | local
            createdAt: Date.now()
          });
        }).catch(function() {
          // 如果索引不存在，直接添加
          store.add({
            warehouseId: warehouseId || null,
            name: siteData.name || '未命名站点',
            key: siteData.key || '',
            api: siteData.api || '',
            type: siteData.type || 'json',
            timeout: siteData.timeout || 10,
            searchable: siteData.searchable || 0,
            quickSearch: siteData.quickSearch || 0,
            categories: siteData.categories || [],
            ext: siteData.ext || {},
            sourceType: siteData.sourceType || 'warehouse',
            createdAt: Date.now()
          });
        });
      });
    },

    getSitesByWarehouse: function(warehouseId) {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('siteConfigs', 'readonly').objectStore('siteConfigs').index('warehouseId').getAll(IDBKeyRange.only(warehouseId)));
      });
    },

    getLocalSites: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('siteConfigs', 'readonly').objectStore('siteConfigs').where('sourceType').equals('local').toArray());
      });
    },

    getAllSites: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('siteConfigs', 'readonly').objectStore('siteConfigs').getAll());
      });
    },

    deleteSiteConfig: function(id) {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('siteConfigs', 'readwrite').objectStore('siteConfigs').delete(id));
      });
    },

    // ==================== 直播管理 ====================
    saveLiveChannels: function(fromSite, channels) {
      return openDB().then(function(d) {
        var tx = d.transaction('liveChannels', 'readwrite');
        var store = tx.objectStore('liveChannels');
        // 先清空该来源的频道 - 使用 getAllKeys + delete 替代递归游标
        var idx = store.index('fromSite');
        var range = IDBKeyRange.only(fromSite);
        return promisifyRequest(idx.getAllKeys(range)).then(function(keys) {
          keys.forEach(function(key) { store.delete(key) });
          return Promise.resolve();
        }).then(function() {
          var added = 0;
          channels.forEach(function(ch) {
            store.add({
              fromSite: fromSite,
              name: ch.name || '未知频道',
              url: ch.url || '',
              group: ch.group || '其他',
              logo: ch.logo || '',
              createdAt: Date.now()
            });
            added++;
          });
          return added;
        });
      });
    },

    getLiveChannelsByGroup: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('liveChannels', 'readonly').objectStore('liveChannels').getAll());
      });
    },

    getAllLiveChannels: function() {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('liveChannels', 'readonly').objectStore('liveChannels').toArray());
      });
    },

    deleteLiveChannel: function(id) {
      return openDB().then(function(d) {
        return promisifyRequest(d.transaction('liveChannels', 'readwrite').objectStore('liveChannels').delete(id));
      });
    },

    // ==================== 搜索配置 ====================
    getSearchThreads: function() {
      try {
        return parseInt(localStorage.getItem('search_threads') || '16');
      } catch(e) {
        return 16;
      }
    },

    setSearchThreads: function(count) {
      try {
        localStorage.setItem('search_threads', String(count));
      } catch(e) {}
    },

    getLiveChannels: function() {
      return this.getAllLiveChannels();
    },

    saveLiveChannel: function(channel) {
      return openDB().then(function(d) {
        return new Promise(function(resolve, reject) {
          var store = d.transaction('liveChannels', 'readwrite').objectStore('liveChannels');
          var req = store.add({
            id: 'live_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
            name: channel.name || '未命名',
            url: channel.url || '',
            group: channel.group || '默认',
            logo: channel.logo || '',
            timestamp: Date.now()
          });
          req.onsuccess = function() { resolve(req.result); };
          req.onerror = function() { reject(req.error); };
        });
      });
    },

    getHistoryCount: function() {
      return openDB().then(function(d) {
        return new Promise(function(resolve, reject) {
          var store = d.transaction('movieHistory', 'readonly').objectStore('movieHistory');
          var req = store.count();
          req.onsuccess = function() { resolve(req.result); };
          req.onerror = function() { reject(req.error); };
        });
      });
    },

    saveHistory: function(item) {
      return openDB().then(function(d) {
        return new Promise(function(resolve, reject) {
          var store = d.transaction('movieHistory', 'readwrite').objectStore('movieHistory');
          var req = store.add(item);
          req.onsuccess = function() { resolve(req.result); };
          req.onerror = function() { reject(req.error); };
        });
      });
    },

    clearHistory: function() {
      return openDB().then(function(d) {
        return new Promise(function(resolve, reject) {
          var store = d.transaction('movieHistory', 'readwrite').objectStore('movieHistory');
          var req = store.clear();
          req.onsuccess = function() { resolve(); };
          req.onerror = function() { reject(req.error); };
        });
      });
    },

    clearFavorites: function() {
      return openDB().then(function(d) {
        return new Promise(function(resolve, reject) {
          var store = d.transaction('favorites', 'readwrite').objectStore('favorites');
          var req = store.clear();
          req.onsuccess = function() { resolve(); };
          req.onerror = function() { reject(req.error); };
        });
      });
    }
  };
})();
