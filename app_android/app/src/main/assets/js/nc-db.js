(function(){
var DB_NAME='TVBoxDB';
var DB_VERSION=5;
var db=null;

function openDB(){
  return new Promise(function(resolve,reject){
    if(db){resolve(db);return}
    var request=indexedDB.open(DB_NAME,DB_VERSION);
    request.onupgradeneeded=function(e){
      var d=e.target.result;
      if(!d.objectStoreNames.contains('warehouses')){
        var ws=d.createObjectStore('warehouses',{keyPath:'id'});
        ws.createIndex('name','name',{unique:false});
      }
      if(!d.objectStoreNames.contains('siteConfigs')){
        var sc=d.createObjectStore('siteConfigs',{keyPath:'id'});
        sc.createIndex('warehouseId','warehouseId',{unique:false});
        sc.createIndex('sourceType','sourceType',{unique:false});
      }
      if(!d.objectStoreNames.contains('movies')){
        var mv=d.createObjectStore('movies',{keyPath:'id'});
        mv.createIndex('sourceId_category',['sourceId','category'],{unique:false});
        mv.createIndex('sourceId','sourceId',{unique:false});
      }
      if(!d.objectStoreNames.contains('liveChannels')){
        var lv=d.createObjectStore('liveChannels',{keyPath:'id'});
        lv.createIndex('group','group',{unique:false});
        lv.createIndex('source','source',{unique:false});
      }
      if(!d.objectStoreNames.contains('history')){
        var ht=d.createObjectStore('history',{keyPath:'id'});
        ht.createIndex('timestamp','timestamp',{unique:false});
        ht.createIndex('movieId','movieId',{unique:false});
      }
      if(!d.objectStoreNames.contains('favorites')){
        var fav=d.createObjectStore('favorites',{keyPath:'movieId'});
        fav.createIndex('timestamp','timestamp',{unique:false});
      }
    };
    request.onsuccess=function(e){db=e.target.result;resolve(db)};
    request.onerror=function(e){reject(e.target.error)};
  });
}

function promisifyRequest(req){
  return new Promise(function(resolve,reject){
    if(!req){reject(new Error('IndexedDB request is null or undefined'));return}
    try{
      req.onsuccess=function(){resolve(req.result)};
      req.onerror=function(){reject(req.error)};
    }catch(e){reject(e)}
  });
}

window.NCDB={
  init:function(){return openDB()},
  // ===== 仓库管理 =====
  saveWarehouse:function(name,url){
    var id='wh_'+Date.now()+'_'+Math.random().toString(36).substr(2,6);
    return openDB().then(function(d){
      var tx=d.transaction('warehouses','readwrite');
      var store=tx.objectStore('warehouses');
      var item={id:id,name:name,url:url,createdAt:Date.now()};
      return promisifyRequest(store.add(item)).then(function(){return id});
    });
  },
  getAllWarehouses:function(){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('warehouses','readonly').objectStore('warehouses').getAll());
    });
  },
  deleteWarehouse:function(id){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('warehouses','readwrite').objectStore('warehouses').delete(id));
    });
  },
  // ===== 站点配置 =====
  saveSiteConfig:function(warehouseId,site){
    return openDB().then(function(d){
      var tx=d.transaction('siteConfigs','readwrite');
      var store=tx.objectStore('siteConfigs');
      var item=Object.assign({warehouseId:warehouseId,sourceType:'warehouse',createdAt:Date.now()},site);
      if(!item.id) item.id='sc_'+Date.now()+'_'+Math.random().toString(36).substr(2,6);
      return promisifyRequest(store.add(item));
    });
  },
  getAllSites:function(){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('siteConfigs','readonly').objectStore('siteConfigs').getAll());
    });
  },
  getLocalSites:function(){
    return openDB().then(function(d){
      var store=d.transaction('siteConfigs','readonly').objectStore('siteConfigs');
      var idx=store.index('sourceType');
      return promisifyRequest(idx.getAll('local'));
    });
  },
  deleteSiteConfig:function(id){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('siteConfigs','readwrite').objectStore('siteConfigs').delete(id));
    });
  },
  // ===== 影片缓存 =====
  saveMovies:function(sourceId,category,movies){
    return openDB().then(function(d){
      var tx=d.transaction('movies','readwrite');
      var store=tx.objectStore('movies');
      var cat=category||'推荐';
      var idx=store.index('sourceId_category');
      var range=IDBKeyRange.only([sourceId,cat]);
      return promisifyRequest(idx.getAllKeys(range)).then(function(keys){
        keys.forEach(function(k){store.delete(k)});
        return Promise.resolve();
      }).then(function(){
        var now=Date.now();
        var addPromises=[];
        movies.forEach(function(v){
          var item={
            id:'mv_'+now+'_'+Math.random().toString(36).substr(2,8),
            sourceId:sourceId,category:cat,vodId:String(v.id||v.vod_id||''),
            title:v.title||'',pic:v.pic||'',tag:v.tag||'',type:v.type||'',year:v.year||'',area:v.area||'',
            actor:v.actor||'',director:v.director||'',score:v.score||'',quality:v.quality||'',
            play:v.play||'',desc:v.desc||'',raw:JSON.stringify(v.raw||{}),
            createdAt:now
          };
          addPromises.push(promisifyRequest(store.add(item)));
        });
        return Promise.all(addPromises).then(function(){return promisifyRequest(tx.complete)});
      });
    });
  },
  getMovies:function(sourceId,category){
    return openDB().then(function(d){
      var store=d.transaction('movies','readonly').objectStore('movies');
      var idx=store.index('sourceId_category');
      var cat=category||'推荐';
      var range=IDBKeyRange.only([sourceId,cat]);
      return promisifyRequest(idx.getAll(range)).then(function(list){
        return list.map(function(m){
          return {
            id:m.vodId,cat:m.category,title:m.title,pic:m.pic,tag:m.tag,type:m.type,
            year:m.year,area:m.area,actor:m.actor,director:m.director,score:m.score,
            quality:m.quality,play:m.play,desc:m.desc,
            raw:(function(){try{return JSON.parse(m.raw)}catch(e){return{}}})()
          };
        });
      });
    });
  },
  // ===== 直播频道 =====
  saveLiveChannels:function(source,channels){
    return openDB().then(function(d){
      var tx=d.transaction('liveChannels','readwrite');
      var store=tx.objectStore('liveChannels');
      return promisifyRequest(store.index('source').getAll(source)).then(function(items){
        items.forEach(function(c){store.delete(c.id)});
        return Promise.resolve();
      }).then(function(){
        var addPromises=[];
        var now=Date.now();
        channels.forEach(function(c){
          addPromises.push(promisifyRequest(store.add({
            id:'lv_'+now+'_'+Math.random().toString(36).substr(2,8),
            name:c.name,url:c.url,group:c.group||'直播',logo:c.logo||'',source:source,createdAt:now
          })));
        });
        return Promise.all(addPromises).then(function(){return promisifyRequest(tx.complete)});
      });
    });
  },
  getAllLiveChannels:function(){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('liveChannels','readonly').objectStore('liveChannels').getAll());
    });
  },
  getLiveGroups:function(){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('liveChannels','readonly').objectStore('liveChannels').getAll()).then(function(list){
        var seen={},out=[];
        list.forEach(function(c){if(c.group&&!seen[c.group]){seen[c.group]=1;out.push(c.group)}});
        return out;
      });
    });
  },
  getLiveChannelsByGroup:function(group){
    return openDB().then(function(d){
      var store=d.transaction('liveChannels','readonly').objectStore('liveChannels');
      var idx=store.index('group');
      return promisifyRequest(idx.getAll(group));
    });
  },
  // ===== 历史记录 =====
  saveHistory:function(item){
    return openDB().then(function(d){
      var tx=d.transaction('history','readwrite');
      var store=tx.objectStore('history');
      return promisifyRequest(store.index('movieId').getAll(item.movieId)).then(function(exists){
        if(exists.length){exists.forEach(function(e){store.delete(e.id)})}
        var hItem=Object.assign({id:'ht_'+Date.now()+'_'+Math.random().toString(36).substr(2,6),timestamp:Date.now()},item);
        return promisifyRequest(store.add(hItem)).then(function(){return tx.complete});
      });
    }).then(function(){return NCDB.getHistory()}).then(function(list){
      if(list.length>200){
        var toDelete=list.slice(200);
        return Promise.all(toDelete.map(function(item){return NCDB.deleteHistory(item.id)}));
      }
    }).catch(function(){})
  },
  getHistory:function(){
    return openDB().then(function(d){
      var store=d.transaction('history','readonly').objectStore('history');
      var idx=store.index('timestamp');
      return promisifyRequest(idx.getAll()).then(function(list){return list.reverse()});
    });
  },
  deleteHistory:function(id){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('history','readwrite').objectStore('history').delete(id));
    });
  },
  clearHistory:function(){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('history','readwrite').objectStore('history').clear());
    });
  },
  // ===== 收藏 =====
  toggleFavorite:function(v){
    return openDB().then(function(d){
      var tx=d.transaction('favorites','readwrite');
      var store=tx.objectStore('favorites');
      return promisifyRequest(store.get(String(v.id))).then(function(exists){
        if(exists){return promisifyRequest(store.delete(String(v.id)))}
        else{
          var favItem={movieId:String(v.id),title:v.title||'',pic:v.pic||'',type:v.type||'',tag:v.tag||'',timestamp:Date.now()};
          return promisifyRequest(store.add(favItem));
        }
      }).then(function(){return promisifyRequest(tx.complete)});
    });
  },
  getAllFavorites:function(){
    return openDB().then(function(d){
      var store=d.transaction('favorites','readonly').objectStore('favorites');
      var idx=store.index('timestamp');
      return promisifyRequest(idx.getAll()).then(function(list){return list.reverse()});
    });
  },
  isFavorite:function(movieId){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('favorites','readonly').objectStore('favorites').get(String(movieId)));
    }).then(function(item){return !!item});
  },
  removeFavorite:function(movieId){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('favorites','readwrite').objectStore('favorites').delete(String(movieId)));
    });
  },
  clearFavorites:function(){
    return openDB().then(function(d){
      return promisifyRequest(d.transaction('favorites','readwrite').objectStore('favorites').clear());
    });
  },
  // ===== 清理缓存 =====
  clearCache:function(){
    return openDB().then(function(d){
      var tx=d.transaction(['movies','liveChannels'],'readwrite');
      tx.objectStore('movies').clear();
      tx.objectStore('liveChannels').clear();
      return promisifyRequest(tx.complete);
    });
  }
};
})();