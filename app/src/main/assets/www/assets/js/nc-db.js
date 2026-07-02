// NewCloud 本地数据库模块：IndexedDB 封装
// 存储采集源、分类、影片数据

var NCDB = (function() {
  var DB_NAME = 'NewCloudDB';
  var DB_VERSION = 1;
  var db = null;

  // 独立的分类名标准化（不依赖外部模块）
  var _CAT_ALIAS = {'电影':'电影片','剧集':'连续剧','连续剧':'连续剧','电视剧':'连续剧','综艺':'综艺片','综艺片':'综艺片','动漫':'动漫片','动漫片':'动漫片','电影片':'电影片'};
  function _normalizeCatName(n) { return _CAT_ALIAS[String(n || '').trim()] || String(n || '').trim() || '推荐'; }

  function openDB() {
    return new Promise(function(resolve, reject) {
      if (db) { resolve(db); return; }
      if (!window.indexedDB) { reject(new Error('浏览器不支持 IndexedDB')); return; }
      try {
        var req = indexedDB.open(DB_NAME, DB_VERSION);
        req.onerror = function(e) {
          var msg = '无法打开本地数据库';
          if (req.error && req.error.name === 'QuotaExceededError') msg = '存储空间不足';
          reject(new Error(msg));
        };
        req.onsuccess = function() { db = req.result; resolve(db); };
        req.onupgradeneeded = function(e) {
          var d = e.target.result;
          if (!d.objectStoreNames.contains('sources')) {
            var sStore = d.createObjectStore('sources', { keyPath: 'id', autoIncrement: true });
            sStore.createIndex('url', 'url', { unique: false });
            sStore.createIndex('base', 'base', { unique: false });
          }
          if (!d.objectStoreNames.contains('categories')) {
            var cStore = d.createObjectStore('categories', { keyPath: 'id', autoIncrement: true });
            cStore.createIndex('sourceId', 'sourceId', { unique: false });
            cStore.createIndex('sourceId_name', ['sourceId', 'name'], { unique: false });
          }
          if (!d.objectStoreNames.contains('movies')) {
            var mStore = d.createObjectStore('movies', { keyPath: 'id', autoIncrement: true });
            mStore.createIndex('sourceId', 'sourceId', { unique: false });
            mStore.createIndex('category', 'category', { unique: false });
            mStore.createIndex('sourceId_category', ['sourceId', 'category'], { unique: false });
            mStore.createIndex('vodId', 'vodId', { unique: false });
            mStore.createIndex('updateTime', 'updateTime', { unique: false });
          }
        };
      } catch(e) { reject(new Error('数据库初始化失败: ' + e.message)); }
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
        // 先删除该源旧分类
        var idx = store.index('sourceId');
        var range = IDBKeyRange.only(sourceId);
        return promisifyRequest(idx.openCursor(range)).then(function deleteNext(cursor) {
          if (!cursor) return;
          cursor.delete();
          return promisifyRequest(cursor.continue()).then(deleteNext);
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
        // 删除该源+分类的旧数据
        return promisifyRequest(idx.openCursor(range)).then(function deleteNext(cursor) {
          if (!cursor) return;
          cursor.delete();
          return promisifyRequest(cursor.continue()).then(deleteNext);
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
        ['movies', 'categories'].forEach(function(name) {
          var store = tx.objectStore(name);
          var idx = store.index('sourceId');
          var range = IDBKeyRange.only(sourceId);
          promisifyRequest(idx.openCursor(range)).then(function deleteNext(cursor) {
            if (!cursor) return;
            cursor.delete();
            return promisifyRequest(cursor.continue()).then(deleteNext);
          });
        });
        return promisifyRequest(tx);
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
        return promisifyRequest(idx.openCursor(range)).then(function deleteNext(cursor) {
          if (!cursor) return;
          cursor.delete();
          return promisifyRequest(cursor.continue()).then(deleteNext);
        });
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
    }
  };
})();
