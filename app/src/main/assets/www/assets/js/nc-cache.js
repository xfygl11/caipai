// NewCloud 采集缓存模块：启动先读本地，随后在线刷新并持久化
(function(){
  var CACHE_PREFIX='nc_ffzy_cache_';
  var HOME_KEY='ac=detail';
  function key(params){return CACHE_PREFIX+encodeURIComponent(params||HOME_KEY)}
  function save(params,data){
    if(!data||data.code!==1)return;
    try{localStorage.setItem(key(params),JSON.stringify({time:Date.now(),data:data}))}catch(e){}
  }
  function read(params){
    try{
      var raw=localStorage.getItem(key(params));
      if(!raw)return null;
      var obj=JSON.parse(raw);
      return obj&&obj.data?obj.data:null;
    }catch(e){return null}
  }
  function hydrate(data){
    if(!data||!data.list||!data.list.length)return false;
    FFZY_CLASSES=data.class&&data.class.length?data.class:(FFZY_CLASSES||[]);
    var roots=(FFZY_CLASSES||[]).filter(function(c){return String(c.type_pid)==='0'}).map(function(c){return c.type_name});
    MOVIE_CATS=['推荐'].concat(roots.length?roots:['电影片','连续剧','综艺片','动漫片']);
    var list=(data.list||[]).map(function(v){return normalizeVod(v,'推荐')}).slice(0,120);
    movieState.usingRemote=true;
    movieState.loaded=true;
    if(!movieState.cat||movieState.cat==='推荐'){
      MOVIE_DATA=list;
      movieState.cat='推荐';
      movieState.currentPage=1;
      movieState.hasMore=true;
      renderMovieHome();
      updateLoadMoreBtn();
    }
    setMovieStatus('已读取本地缓存，正在更新...',true);
    return true;
  }
  function catParams(cat,page){
    page=page||1;
    var tid=(cat&&cat!=='推荐'&&window.ffzyClassId)?ffzyClassId(cat):'';
    return 'ac=detail&page='+page+(tid?'&t='+tid:'');
  }
  function saveCat(cat,page,data){save(catParams(cat,page),data)}
  function readCat(cat,page){return read(catParams(cat,page))}
  function listFromCatCache(cat,page){
    var data=readCat(cat,page||1);
    if(!data||!data.list||!data.list.length)return null;
    return data.list.map(function(v){return normalizeVod(v,cat)}).slice(0,80);
  }
  window.NCCache={save:save,read:read,hydrate:hydrate,catParams:catParams,saveCat:saveCat,readCat:readCat,listFromCatCache:listFromCatCache};

  var oldFetch=window.ffzyFetch;
  if(oldFetch){
    window.ffzyFetch=function(params){
      return oldFetch(params).then(function(data){
        save(params||HOME_KEY,data);
        return data;
      }).catch(function(e){
        var cached=read(params||HOME_KEY);
        if(cached)return cached;
        throw e;
      });
    };
  }

  var oldInit=window.ffzyInit;
  if(oldInit){
    window.ffzyInit=function(){
      var cached=read(HOME_KEY);
      if(cached)hydrate(cached);
      return oldInit.apply(this,arguments);
    };
  }
})();
