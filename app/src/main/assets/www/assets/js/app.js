// 个人助手 - 核心入口
// 版本 9.9 (Build 74)
//
// 模块架构：
//   nc-db.js        - IndexedDB 本地数据库
//   nc-lottery.js    - 彩票数据、预测、同步、渲染
//   nc-movie-engine.js - 影视数据、加载、缓存、配置、播放
//   nc-ui.js         - 骨架屏、空状态、搜索历史、面板、下拉刷新
//   nc-page.js      - 页面注册系统、分类页推荐兜底
//   nc-cache.js     - 采集缓存模块
//   nc-repo.js      - 仓库/API管理模块
//   nc-catalog-search.js - 分类导航与搜索模块
//   nc-player.js    - 播放器手势增强模块
//   nc-transitions.js - 页面切换动效模块
//   app.js (本文件)  - TV UI 覆盖层、手动采集、数据库查看、初始化

// ===================== TV NEW UI =====================

window._ncAppRenderMovieHome = true;

// --- Search history helper ---
function saveSearchHistory(kw){
  if(!kw||!kw.trim())return;
  try{
    var list=JSON.parse(localStorage.getItem('movie_search_history')||'[]');
    list=list.filter(function(x){return x!==kw.trim()});
    list.unshift(kw.trim());
    localStorage.setItem('movie_search_history',JSON.stringify(list.slice(0,20)));
  }catch(e){}
}

// --- Override renderMovieHome for new UI ---
window._renderMovieHomeLegacy=renderMovieHome;
renderMovieHome=function(){
  var cats=document.getElementById('tvCatScroll'),grid=document.getElementById('tvGrid');
  if(!cats||!grid){return window._renderMovieHomeLegacy&&window._renderMovieHomeLegacy()}
  renderTvCats();
  if(movieState.cat==='直播'){renderLiveGrid(grid);return}
  var fav=lsGet('movie_favs'),kw=(movieState.keyword||'').trim().toLowerCase();
  var list=MOVIE_DATA.filter(function(v){return (movieState.cat==='推荐'||movieState.cat==='搜索'||v.cat===movieState.cat)&&(!kw||String(v.title).toLowerCase().indexOf(kw)>=0||String(v.type).toLowerCase().indexOf(kw)>=0)});
  var secName=document.getElementById('tvSectionName');
  if(secName)secName.textContent=movieState.cat;
  if(!list.length){grid.innerHTML='<div class="lib-item" style="grid-column:1/-1"><b>没有找到影片</b><span>请到「我的」页面采集数据或切换分类</span></div>';hideEmptyGuide();return}
  window.MOVIE_INDEX=window.MOVIE_INDEX||{};
  grid.innerHTML=list.map(function(v){
    window.MOVIE_INDEX[String(v.id)]=v;
    var on=fav.indexOf(v.id)>=0;
    var img=v.pic?'background-image:url(\''+v.pic+'\')':'';
    var year=v.year||(v.title&&v.title.match(/\d{4}/)?v.title.match(/\d{4}/)[0]:'');
    var hd=v.quality||v.tag||'HD';
    return '<div class="tv-card" onclick="moviePlay(\''+v.id+'\')"><div class="tv-poster" style="'+img+'"><span class="tv-year-tag">'+year+'</span><span class="tv-hd-tag">'+hd+'</span><div class="tv-card-title">'+v.title+'</div></div><div class="tv-card-info">'+(v.type||v.cat||'影视')+' '+(on?' · 已收藏':'')+'</div></div>';
  }).join('');
  renderMine();
  hideEmptyGuide();
  updateLoadMoreBtn();
  updateCacheSize();
};

function renderTvCats(){
  var scroll=document.getElementById('tvCatScroll'),dropdown=document.getElementById('tvCatGrid');
  if(!scroll)return;
  if(window.NCDB&&movieState.dbCats&&movieState.dbCats.length){
    var cats=['推荐'].concat(movieState.dbCats);
    var html=cats.map(function(c){return '<button class="'+(movieState.cat===c?'active':'')+'" onclick="tvSetCat(\''+c+'\')">'+c+'</button>'}).join('');
    scroll.innerHTML=html;
    if(dropdown)dropdown.innerHTML=cats.map(function(c){return '<button class="'+(movieState.cat===c?'active':'')+'" onclick="tvSetCat(\''+c+'\');toggleCatDropdown()">'+c+'</button>'}).join('');
    return;
  }
  var html=MOVIE_CATS.map(function(c){return '<button class="'+(movieState.cat===c?'active':'')+'" onclick="tvSetCat(\''+c+'\')">'+c+'</button>'}).join('');
  scroll.innerHTML=html;
  if(dropdown)dropdown.innerHTML=MOVIE_CATS.map(function(c){return '<button class="'+(movieState.cat===c?'active':'')+'" onclick="tvSetCat(\''+c+'\');toggleCatDropdown()">'+c+'</button>'}).join('');
}

// 从数据库刷新分类列表（只显示已有数据的分类）
function updateDbRenderCats(){
  if(!window.NCDB)return;
  NCDB.getDistinctCategoryNames().then(function(names){
    movieState.dbCats=names;
    renderTvCats();
  });
}

function tvSetCat(c){
  movieState.cat=c;
  // 尝试从种子数据加载分类内容
  if(c!=='推荐'&&window.FFZY_SEED){
    var tid=ffzyClassId(c)||'';
    var params=c==='推荐'||!tid?'ac=detail':'ac=detail&t='+encodeURIComponent(tid);
    var data=ffzySeedResponse(params);
    var list=(data&&data.list||[]).map(function(v){return normalizeVod(v,c)}).slice(0,80);
    if(list.length){MOVIE_DATA=list;renderMovieHome();updateLoadMoreBtn();setMovieStatus(c+' 种子数据 ('+list.length+'部)',true);return}
  }
  if(c==='直播'){renderMovieHome();return}
  if(window.NCDB){
    var base=ncSourceBase()||FFZY_API_BASE.replace(/\/$/,'');
    NCDB.getSourceByBase(base).then(function(src){
      if(src&&src.id){
        NCDB.getMovies(src.id,c,60).then(function(list){
          if(list&&list.length){MOVIE_DATA=list;renderMovieHome();updateLoadMoreBtn();setMovieStatus('已加载 '+c+' 本地数据 ('+list.length+'部)',true);}
          else{MOVIE_DATA=[];renderMovieHome();setMovieStatus(c+' 暂无本地数据',false);}
        }).catch(function(){MOVIE_DATA=[];renderMovieHome();});
      }else{MOVIE_DATA=[];renderMovieHome();setMovieStatus('请先采集数据',false);}
    }).catch(function(){MOVIE_DATA=[];renderMovieHome();});
    return;
  }
  renderMovieHome();
}
function tvSearch(v){
  movieState.keyword=v||'';
  var kw=(v||'').trim();
  if(!kw){tvShowAll();return}
  saveSearchHistory(kw);
  // 搜索种子数据
  if(window.FFZY_SEED){
    var data=ffzySeedResponse('ac=detail&wd='+encodeURIComponent(kw));
    var list=(data&&data.list||[]).map(function(v){return normalizeVod(v,'搜索')}).slice(0,80);
    if(list.length){MOVIE_DATA=list;movieState.cat='搜索';renderMovieHome();setMovieStatus('搜索: '+kw+' ('+list.length+'条)',true);return}
  }
  renderMovieHome();
}
function tvShowAll(){
  movieState.cat='推荐';movieState.keyword='';
  var inp=document.getElementById('tvSearchInput');if(inp)inp.value='';
  if(window.FFZY_SEED){
    var data=ffzySeedResponse('ac=detail');
    var list=(data&&data.list||[]).map(function(v){return normalizeVod(v,'推荐')}).slice(0,60);
    if(list.length){MOVIE_DATA=list;renderMovieHome();setMovieStatus('推荐 ('+list.length+'部)',true);updateLoadMoreBtn();return}
  }
  renderMovieHome();
}
function tvLoadNext(){alert('请先到「我的」页面采集更多数据')}

function toggleCatDropdown(){
  var el=document.getElementById('tvCatDropdown');
  if(!el)return;
  el.style.display=el.style.display==='none'?'block':'none';
}

// --- Panels ---
function showRepoPanel(){var el=document.getElementById('repoPanelOverlay');if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10);renderRepoPanel()}}
function hideRepoPanel(){var el=document.getElementById('repoPanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showSitePanel(){var el=document.getElementById('sitePanelOverlay');if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10);renderSitePanel()}}
function hideSitePanel(){var el=document.getElementById('sitePanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showMoreMenu(){var el=document.getElementById('morePanelOverlay');if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10)}}
function hideMorePanel(){var el=document.getElementById('morePanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showConfigInput(){var el=document.getElementById('tvConfigInputRow');if(el)el.style.display='flex'}
function toggleParserPanel(){var row=document.getElementById('parserSelectRow');if(row)row.style.display=row.style.display==='none'?'flex':'none'}
function showWindowManager(){alert('窗口管理功能开发中')}

// --- Long Press on Home Nav ---
var _lpTimer=null,_lpTriggered=false;
function startHomeLongPress(){_lpTriggered=false;_lpTimer=setTimeout(function(){_lpTriggered=true;showRepoPanel()},600)}
function endHomeLongPress(){if(_lpTimer){clearTimeout(_lpTimer);_lpTimer=null}}
(function initLongPress(){
  var btn=document.getElementById('navHomeBtn');
  if(!btn)return;
  btn.addEventListener('touchstart',function(){startHomeLongPress()},{passive:true});
  btn.addEventListener('touchend',function(e){
    endHomeLongPress();
    if(_lpTriggered){
      e.preventDefault();
      e.stopPropagation();
      _lpTriggered=false;
      return;
    }
    switchMainPage('home');
  },{passive:false});
  btn.addEventListener('mousedown',function(e){startHomeLongPress()});
  btn.addEventListener('mouseup',function(e){endHomeLongPress()});
  btn.addEventListener('click',function(e){if(_lpTriggered){e.stopPropagation();e.preventDefault();_lpTriggered=false}});
})();

// --- Update source name on load ---
(function(){
  var origApply=applyMovieConfig;
  applyMovieConfig=function(cfg,url){
    var r=origApply(cfg,url);
    var nameEl=document.getElementById('tvSourceName');
    if(nameEl&&movieConfig.site)nameEl.textContent=movieConfig.site.name||movieConfig.site.key||'个人助手 TV';
    TV_SITES=(movieConfig.sites||[]).slice(0,20).map(function(s){return {name:s.name||s.key,key:s.key||s.name,desc:s.type_name||'影视',icon:'◉'}});
    return r;
  };
})();

// --- Search history compat ---
function selectSearchHistoryItem(kw){
  var inp=document.getElementById('tvSearchInput')||document.getElementById('movieSearchInput');
  if(inp)inp.value=kw;
  hideSearchHistory();
  tvSearch(kw);
}

// ===================== 手动采集功能（弹窗） =====================

var COLLECT_STATE={running:false,stop:false,currentCat:0,cycle:1,sourceName:'',sourceUrl:'',sourceBase:'',sourceId:null,categories:[],isFFZY:false,catCollected:{}};

function showCollectPanel(){
  var el=document.getElementById('collectOverlay');
  if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10)}
  refreshCollectSourceSelect();
}
function hideCollectPanel(){
  var el=document.getElementById('collectOverlay');
  if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}
}

function refreshCollectSourceSelect(){
  var sel=document.getElementById('collectSourceSelect');
  if(!sel)return;
  sel.innerHTML='<option value="">选择采集源</option>';
  var sources=window.NCRepoSources?NCRepoSources():[];
  sources.forEach(function(s,i){
    var opt=document.createElement('option');
    opt.value=String(s.url||'');
    opt.textContent=s.name||('采集源'+(i+1));
    sel.appendChild(opt);
  });
}

function onCollectSourceChange(){
  var sel=document.getElementById('collectSourceSelect');
  var val=sel?sel.value:'';
  if(!val)return;
  COLLECT_STATE.sourceUrl=val;
  var sources=window.NCRepoSources?NCRepoSources():[];
  var found=sources.filter(function(s){return String(s.url||'')===val})[0];
  COLLECT_STATE.sourceName=found?found.name:'采集源';
  setCollectStatus('已选择：'+COLLECT_STATE.sourceName);
}

function setCollectStatus(msg){var el=document.getElementById('collectStatus');if(el)el.textContent=msg||''}
function setCollectLog(msg){var el=document.getElementById('collectLog');if(el){el.innerHTML=(msg?'<div>'+msg+'</div>':'')+el.innerHTML;el.scrollTop=0}}
function showCollectItem(title,target){
  var wrap=document.getElementById('collectCurrentItem');
  var tEl=document.getElementById('collectItemTitle');
  var cEl=document.getElementById('collectItemTarget');
  if(wrap)wrap.style.display='block';
  if(tEl)tEl.textContent=title||'';
  if(cEl)cEl.textContent=target?'分类到：'+target:'';
}
function hideCollectItem(){var el=document.getElementById('collectCurrentItem');if(el)el.style.display='none'}
function updateItemProgress(cur,total){
  var bar=document.getElementById('collectItemProgressBar');
  var txt=document.getElementById('collectItemProgressText');
  if(bar)bar.style.width=total?((cur/total)*100)+'%':'0%';
  if(txt)txt.textContent=(cur||0)+'/'+(total||0);
}
function updateTotalProgress(cur,total){
  var bar=document.getElementById('collectTotalProgressBar');
  var txt=document.getElementById('collectTotalProgressText');
  if(bar)bar.style.width=total?((cur/total)*100)+'%':'0%';
  if(txt)txt.textContent=(cur||0)+'/'+(total||0);
}

function startCollect(){
  if(COLLECT_STATE.running){alert('采集正在进行中');return}
  var sel=document.getElementById('collectSourceSelect');
  var url=sel?sel.value:'';
  if(!url){alert('请先选择采集源');return}
  COLLECT_STATE.running=true;
  COLLECT_STATE.stop=false;
  COLLECT_STATE.currentCat=0;
  COLLECT_STATE.cycle=1;
  COLLECT_STATE.catCollected={};
  COLLECT_STATE.sourceUrl=url;
  COLLECT_STATE.sourceBase=url.replace(/\/$/,'');
  COLLECT_STATE.isFFZY=/ffzy|provide\/vod/i.test(url);
  var sources=window.NCRepoSources?NCRepoSources():[];
  var found=sources.filter(function(s){return String(s.url||'')===url})[0];
  COLLECT_STATE.sourceName=found?found.name:'采集源';
  setCollectStatus('正在连接 '+COLLECT_STATE.sourceName+'...');
  updateItemProgress(0,0);
  updateTotalProgress(0,0);
  hideCollectItem();
  document.getElementById('collectLog').innerHTML='';
  // 先获取分类（统一使用 doCollectFetch，FFZY也用用户选择的sourceBase）
  var initParams='ac=detail';
  new Promise(function(resolve,reject){
    doCollectFetch(COLLECT_STATE.sourceBase+'?'+initParams,initParams,resolve,reject);
  }).then(function(data){
    if(!data||data.code!==1){finishCollect('连接失败：'+(data&&data.msg?data.msg:'未知错误'));return}
    var classes=data.class||[];
    // 如果API返回的class为空，尝试从种子数据获取
    if(!classes.length&&window.FFZY_SEED&&window.FFZY_SEED.class){classes=window.FFZY_SEED.class}
    // 如果还是为空，从list数据中自动提取分类
    if(!classes.length){
      var list=data.list||[];
      var catMap={};
      list.forEach(function(v){
        var t=v.type_name||v.type||'';
        if(t&&!catMap[t]){catMap[t]=1}
      });
      classes=[];
      for(var cn in catMap){classes.push({type_id:'',type_pid:0,type_name:cn})}
    }
    // 获取根分类：type_pid 为 0/null/undefined/空字符串 的都是根分类
    var roots=classes.filter(function(c){
      var pid=c.type_pid;
      return pid==null||pid==undefined||pid===''||pid==0||pid==='0';
    });
    // 如果过滤后没有根分类，使用所有分类进行采集
    if(!roots.length&&classes.length)roots=classes.slice();
    COLLECT_STATE.categories=roots;
    setCollectLog('获取到 '+roots.length+' 个分类：'+roots.map(function(c){return c.type_name||''}).join(', '));
    // 保存源和分类
    NCDB.saveSource(COLLECT_STATE.sourceName,url,COLLECT_STATE.sourceBase).then(function(srcId){
      COLLECT_STATE.sourceId=srcId;
      NCDB.saveCategories(srcId,classes);
      // 先处理推荐（推荐用全量保存，因为推荐需要替换）
      var items=(data.list||[]).slice(0,60).map(function(v){return normalizeVod(v,'推荐')});
      NCDB.saveMovies(srcId,'推荐',items);
      setCollectLog('推荐 已保存 '+items.length+' 部');
      updateTotalProgress(0,1);
      setCollectLog('===== 开始第 1 轮采集 =====');
      // 开始循环采集
      collectCategoryLoop(0,1,0);
    }).catch(function(e){finishCollect('保存源失败：'+e)});
  }).catch(function(err){finishCollect('连接失败：'+err)});
}

function collectCategoryLoop(idx,page,collectedInPage){
  if(COLLECT_STATE.stop){finishCollect('已停止');return}
  var cats=COLLECT_STATE.categories;
  if(!cats||!cats.length){finishCollect('没有可采集的分类');return}
  idx=((idx % cats.length)+cats.length)%cats.length;
  if(idx===0&&page===1&&collectedInPage===0){
    var anyProgress=false;
    for(var k in COLLECT_STATE.catCollected){if(COLLECT_STATE.catCollected[k]>0){anyProgress=true;break}}
    if(anyProgress){
      COLLECT_STATE.cycle++;
      setCollectLog('===== 开始第 '+COLLECT_STATE.cycle+' 轮采集 =====');
    }else{
      if(COLLECT_STATE.cycle>1){finishCollect('本轮无新数据，采集完成');return}
    }
  }
  var cat=cats[idx];
  var catName=normalizeCatName(cat.type_name||'');
  var typeId=cat.type_id||'';
  var catKey=String(typeId||idx);
  var alreadyNew=COLLECT_STATE.catCollected[catKey]||0;
  if(alreadyNew>=100){
    setCollectLog(catName+' 新数据已满100部，跳过');
    setTimeout(function(){collectCategoryLoop(idx+1,1,0)},200);
    return;
  }
  setCollectStatus('第'+COLLECT_STATE.cycle+'轮 · '+catName+' 新数据('+alreadyNew+'/100) 第'+page+'页');
  var params=COLLECT_STATE.isFFZY?'ac=list':'ac=detail';
  if(typeId)params+='&t='+typeId;
  if(page>1)params+='&pg='+page;
  function onFetchSuccess(data){
    if(!data||data.code!==1){
      setCollectLog(catName+' 第'+page+'页错误，跳到下一分类');
      setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
      return;
    }
    var rawItems=data.list||[];
    if(!rawItems.length){
      setCollectLog(catName+' 无更多数据，跳到下一分类');
      setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
      return;
    }
    // 去重：与数据库已有数据对比，只保留新数据
    var need=100-alreadyNew;
    var existingIds={};
    NCDB.getMovies(COLLECT_STATE.sourceId,catName,9999).then(function(existing){
      existing.forEach(function(m){if(m.id)existingIds[m.id]=1});
      var newItems=[];
      for(var i=0;i<rawItems.length;i++){
        var v=rawItems[i];
        var vid=String(v.vod_id||v.id||'');
        if(vid&&existingIds[vid])continue;
        newItems.push(normalizeVod(v,catName));
        if(newItems.length>=need)break;
      }
      if(newItems.length===0){
        setCollectLog(catName+' 第'+page+'页无新数据');
        if(page>=5){
          setCollectLog(catName+' 连续5页无新数据，跳到下一分类');
          setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
        }else{
          setTimeout(function(){collectCategoryLoop(idx,page+1,0)},300);
        }
        return;
      }
      // 逐条显示新数据
      function showBatch(i){
        if(COLLECT_STATE.stop){finishCollect('已停止');return}
        if(i>=newItems.length){
          NCDB.saveMoviesIncremental(COLLECT_STATE.sourceId,catName,newItems).then(function(added){
            COLLECT_STATE.catCollected[catKey]=alreadyNew+added;
            var totalNew=COLLECT_STATE.catCollected[catKey];
            setCollectLog(catName+' 新增 '+added+' 部 (累计新数据 '+totalNew+'/100)');
            if(totalNew>=100){
              hideCollectItem();
              setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
            }else if(rawItems.length<20){
              hideCollectItem();
              setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
            }else{
              setTimeout(function(){collectCategoryLoop(idx,page+1,0)},300);
            }
          }).catch(function(e){
            setCollectLog(catName+' 保存失败：'+e);
            setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
          });
          return;
        }
        var nv=newItems[i];
        showCollectItem(nv.title,catName);
        updateItemProgress(i+1,newItems.length);
        setCollectStatus('['+catName+'] '+nv.title+' ('+(i+1)+'/'+newItems.length+')');
        if(i%3===0){setCollectLog('['+catName+'] '+nv.title+' (页'+page+')')}
        setTimeout(function(){showBatch(i+1)},30);
      }
      showBatch(0);
    }).catch(function(e){
      setCollectLog(catName+' 去重查询失败：'+e);
      setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
    });
  }
  function onFetchError(err){
    setCollectLog(catName+' 请求失败：'+err);
    setTimeout(function(){collectCategoryLoop(idx+1,1,0)},300);
  }
  var reqUrl=COLLECT_STATE.sourceBase+'?'+params;
  doCollectFetch(reqUrl,params,onFetchSuccess,onFetchError);
}

function doCollectFetch(url,params,onOk,onErr){
  if(window.NativeHttp&&NativeHttp.httpGet){
    setTimeout(function(){
      try{var text=NativeHttp.httpGet(url);if(!text){onErr('空响应');return}onOk(JSON.parse(text))}catch(e){onErr(e.message)}
    },0);
    return;
  }
  fetch(url,{cache:'no-store'}).then(function(r){if(!r.ok)throw 'HTTP '+r.status;return r.json()}).then(onOk).catch(function(e){
    var seed=ffzySeedResponse(params);
    if(seed){onOk(seed);return}
    onErr(e);
  });
}

function stopCollect(){
  COLLECT_STATE.stop=true;
  setCollectStatus('已停止');
  hideCollectItem();
  COLLECT_STATE.running=false;
}

function finishCollect(msg){
  COLLECT_STATE.running=false;
  setCollectStatus(msg);
  hideCollectItem();
  updateDbRenderCats();
}

// ===================== 数据库查看功能 =====================

var DB_VIEW_STATE={sourceId:'',category:'',page:1,limit:30,total:0,list:[]};

function showDbViewer(){
  var el=document.getElementById('dbViewerOverlay');
  if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10)}
  refreshDbSourceSelect();
  refreshDbCategorySelect().then(function(){
    dbViewLoad();
  }).catch(function(){dbViewLoad()});
}
function hideDbViewer(){
  var el=document.getElementById('dbViewerOverlay');
  if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}
}

function refreshDbSourceSelect(){
  var sel=document.getElementById('dbViewSource');
  if(!sel)return;
  sel.innerHTML='<option value="">全部源</option>';
  NCDB.getSources().then(function(sources){
    sources.forEach(function(s){
      var opt=document.createElement('option');
      opt.value=String(s.id);
      opt.textContent=s.name||('源'+s.id);
      sel.appendChild(opt);
    });
  }).catch(function(e){console.error('refreshDbSourceSelect',e)});
}

function refreshDbCategorySelect(){
  var sel=document.getElementById('dbViewCategory');
  if(!sel)return Promise.resolve();
  sel.innerHTML='<option value="">全部分类</option>';
  var srcId=document.getElementById('dbViewSource').value;
  if(!srcId){
    return NCDB.getDistinctCategoryNames().then(function(names){
      names.forEach(function(n){var opt=document.createElement('option');opt.value=n;opt.textContent=n;sel.appendChild(opt)});
    }).catch(function(e){console.error('refreshDbCategorySelect',e)});
  }
  return NCDB.getCategories(parseInt(srcId)).then(function(cats){
    cats.forEach(function(c){var opt=document.createElement('option');opt.value=c.name;opt.textContent=c.name;sel.appendChild(opt)});
  }).catch(function(e){console.error('refreshDbCategorySelect',e)});
}

function onDbViewSourceChange(){
  var catSel=document.getElementById('dbViewCategory');
  var oldCat=catSel?catSel.value:'';
  refreshDbCategorySelect().then(function(){
    var newSel=document.getElementById('dbViewCategory');
    if(newSel&&oldCat){
      for(var i=0;i<newSel.options.length;i++){
        if(newSel.options[i].value===oldCat){newSel.value=oldCat;break}
      }
    }
    dbViewLoad();
  }).catch(function(){dbViewLoad()});
}

function dbViewLoad(){
  DB_VIEW_STATE.page=1;
  dbViewFetch();
}

function dbViewFetch(){
  var srcSel=document.getElementById('dbViewSource');
  var catSel=document.getElementById('dbViewCategory');
  var listEl=document.getElementById('dbViewList');
  var srcId=srcSel?srcSel.value:'';
  var cat=catSel?catSel.value:'';
  DB_VIEW_STATE.sourceId=srcId;
  DB_VIEW_STATE.category=cat;
  if(listEl)listEl.innerHTML='<div style="text-align:center;color:#8899aa;padding:20px">加载中...</div>';
  if(srcId){
    NCDB.getMovies(parseInt(srcId),cat,9999).then(function(list){
      DB_VIEW_STATE.total=list?list.length:0;
      DB_VIEW_STATE.list=list||[];
      dbViewRender();
    }).catch(function(e){
      console.error('dbViewFetch single',e);
      DB_VIEW_STATE.total=0;DB_VIEW_STATE.list=[];dbViewRender();
    });
  }else{
    NCDB.getSources().then(function(sources){
      if(!sources||!sources.length){DB_VIEW_STATE.total=0;DB_VIEW_STATE.list=[];dbViewRender();return}
      var promises=sources.map(function(s){
        return NCDB.getMovies(s.id,cat,9999).catch(function(e){console.error('dbViewFetch source',s.id,e);return []});
      });
      Promise.all(promises).then(function(results){
        var all=[];
        results.forEach(function(r){all=all.concat(r||[])});
        DB_VIEW_STATE.total=all.length;
        DB_VIEW_STATE.list=all;
        dbViewRender();
      }).catch(function(e){
        console.error('dbViewFetch all',e);
        DB_VIEW_STATE.total=0;DB_VIEW_STATE.list=[];dbViewRender();
      });
    }).catch(function(e){
      console.error('dbViewFetch sources',e);
      DB_VIEW_STATE.total=0;DB_VIEW_STATE.list=[];dbViewRender();
    });
  }
}

function dbViewRender(){
  var listEl=document.getElementById('dbViewList');
  var pageEl=document.getElementById('dbViewPage');
  var prevBtn=document.getElementById('dbViewPrevBtn');
  var nextBtn=document.getElementById('dbViewNextBtn');
  if(!listEl)return;
  var start=(DB_VIEW_STATE.page-1)*DB_VIEW_STATE.limit;
  var end=start+DB_VIEW_STATE.limit;
  var pageList=(DB_VIEW_STATE.list||[]).slice(start,end);
  var totalPages=Math.ceil(DB_VIEW_STATE.total/DB_VIEW_STATE.limit)||1;
  if(pageEl)pageEl.textContent='第'+DB_VIEW_STATE.page+'/'+totalPages+'页 (共'+DB_VIEW_STATE.total+'条)';
  if(prevBtn)prevBtn.disabled=DB_VIEW_STATE.page<=1;
  if(nextBtn)nextBtn.disabled=DB_VIEW_STATE.page>=totalPages;
  if(!pageList.length){listEl.innerHTML='<div style="text-align:center;color:#8899aa;padding:20px">暂无数据</div>';return}
  listEl.innerHTML=pageList.map(function(v){
    var img=v.pic?'<img src="'+v.pic+'" style="width:60px;height:80px;object-fit:cover;border-radius:4px">':'<div style="width:60px;height:80px;background:#1e3a5f;border-radius:4px;display:flex;align-items:center;justify-content:center;color:#4aa8ff;font-size:10px">无图</div>';
    return '<div style="display:flex;gap:10px;padding:8px;border-bottom:1px solid #1a2a3a">'+img+'<div style="flex:1;min-width:0"><div style="font-size:14px;color:#e0e0e0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">'+v.title+'</div><div style="font-size:11px;color:#8899aa;margin-top:4px">'+(v.cat||v.type||'')+' · '+(v.year||'')+'</div></div></div>';
  }).join('');
}

function dbViewPrev(){
  if(DB_VIEW_STATE.page>1){DB_VIEW_STATE.page--;dbViewRender()}
}
function dbViewNext(){
  var totalPages=Math.ceil(DB_VIEW_STATE.total/DB_VIEW_STATE.limit)||1;
  if(DB_VIEW_STATE.page<totalPages){DB_VIEW_STATE.page++;dbViewRender()}
}

function clearDbData(){
  if(!confirm('确定要清空所有采集数据吗？此操作不可恢复。'))return;
  if(!window.NCDB){alert('数据库未初始化');return}
  NCDB.getSources().then(function(sources){
    if(!sources||!sources.length){alert('数据库为空');return}
    var promises=sources.map(function(s){return NCDB.clearSource(s.id).catch(function(){return null})});
    Promise.all(promises).then(function(){
      alert('已清空所有数据');
      movieState.dbCats=[];
      MOVIE_DATA=[];
      renderTvCats();
      renderMovieHome();
      hideDbViewer();
    });
  }).catch(function(e){alert('清空失败：'+e)});
}

// ===================== INIT =====================
(function(){
  function safeBuild(k){
    try{buildTab(k);return}
    catch(e){
      console.error(k,'build failed',e);
      var tabEl=document.getElementById('tab-'+k);
      if(tabEl)tabEl.innerHTML='<div class="placeholder"><h2>加载失败</h2><p>本地数据已保留，请尝试重启应用或手动导出备份后再处理</p></div>';
    }
  }

  // 用种子数据填充 MOVIE_DATA 确保首次加载有内容显示
  if(!MOVIE_DATA||!MOVIE_DATA.length){
    if(window.FFZY_SEED&&window.FFZY_SEED.list){
      FFZY_CLASSES=window.FFZY_SEED.class||[];
      MOVIE_DATA=(window.FFZY_SEED.list||[]).slice(0,60).map(function(v){return normalizeVod(v,'推荐')});
      movieState.cat='推荐';
      setMovieStatus('已加载种子数据 ('+MOVIE_DATA.length+'部)',true);
    }
  }

  // 初始化本地数据库
  if(window.NCDB){
    NCDB.init().then(function(){
      updateDbRenderCats();
      NCDB.getSources().then(function(sources){
        if(sources&&sources.length){
          var src=sources[sources.length-1];
          NCDB.getMovies(src.id,'推荐',60).then(function(list){
            if(list&&list.length){MOVIE_DATA=list;movieState.cat='推荐';renderMovieHome();updateLoadMoreBtn();setMovieStatus('已加载本地数据 ('+list.length+'部)',true);}
          }).catch(function(e){console.error('加载本地影片失败',e);});
        }else{
          // 首次运行：将种子数据写入数据库
          var base=(FFZY_API_BASE||'http://cj.ffzyapi.com/api.php/provide/vod').replace(/\/$/,'');
          NCDB.saveSource('种子数据(演示)','seed://demo',base).then(function(srcId){
            if(window.FFZY_SEED&&window.FFZY_SEED.class){
              NCDB.saveCategories(srcId,window.FFZY_SEED.class);
            }
            NCDB.saveMovies(srcId,'推荐',MOVIE_DATA.slice(0,60));
            updateDbRenderCats();
          }).catch(function(e){console.error('种子数据写入失败',e);});
        }
      }).catch(function(e){console.error('获取数据源失败',e);});
    }).catch(function(e){
      console.error('数据库初始化失败，使用内存模式',e);
    });
  }

  initMainDrawHistory();
  renderMovieHome();
  renderLibrary();
  renderMine();
  safeBuild('dlt');
  safeBuild('ssq');
  buildLotteryPages();
  initTabSnap();
  updateAllLotteryCD();
  setInterval(function(){updateCD('dlt');updateCD('ssq');updateAllLotteryCD()},1000);
  var _sysEl=document.getElementById('systime');
  if(_sysEl){
    _sysEl.textContent=new Date().toLocaleString('zh-CN');
    setInterval(function(){var el=document.getElementById('systime');if(el)el.textContent=new Date().toLocaleString('zh-CN')},1000);
  }
  fetchAPI('dlt');fetchAPI('ssq');
  setInterval(function(){fetchAPI('dlt');fetchAPI('ssq')},300000);
})();

// --- 渲染我的页面时刷新采集源选择 ---
(function(){
  var origRenderMine=renderMine;
  renderMine=function(){
    if(origRenderMine)origRenderMine();
    refreshCollectSourceSelect();
  };
})();
