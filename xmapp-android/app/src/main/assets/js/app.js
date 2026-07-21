// 个人助手 - 核心入口
// 版本 9.9 (Build 74)
//
// 模块架构：
//   nc-db.js        - IndexedDB 本地数据库

// Cross-file scope bridge: import globals from other loaded scripts
var MOVIE_DATA = window.MOVIE_DATA || [];
var MOVIE_CATS = window.MOVIE_CATS || [];
var movieState = window.movieState || { cat: '', page: 1, usingRemote: false, loaded: false, results: [], dbCats: [] };
var lsGet = window.lsGet || function(k) { try { return JSON.parse(localStorage.getItem(k) || '[]'); } catch(e) { return []; } };
var lsSet = window.lsSet || function(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch(e) {}; };

// XSS protection helpers
function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function escHtmlAttr(val) {
  return String(val || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// Debug: log imported globals
console.log('[APP] Imported: MOVIE_DATA=', !!window.MOVIE_DATA, 'MOVIE_CATS=', !!window.MOVIE_CATS, 'movieState=', !!window.movieState, 'loadMovieList=', !!window.loadMovieList, 'renderMovieHome=', !!window.renderMovieHome);

// Fallback for lsGet/lsSet if nc-movie-engine.js hasn't defined them
if (typeof window.lsGet !== 'function') {
  window.lsGet = lsGet;
  window.lsSet = lsSet;
}
//   nc-lottery.js    - 彩票数据、预测、同步、渲染
//   nc-movie-engine.js - 影视数据、加载、缓存、配置、播放
//   nc-ui.js         - 骨架屏、空状态、搜索历史、面板、下拉刷新
//   nc-page.js      - 页面注册系统、分类页推荐兜底
//   nc-cache.js     - 采集缓存模块
//   nc-repo.js      - 仓库/API管理模块
//   nc-site-manage.js - 站点管理模块
//   nc-search.js    - 多源并发搜索模块
//   nc-live.js      - 直播模块
//   nc-player.js    - 播放器手势增强模块
//   nc-transitions.js - 页面切换动效模块
//   exo-player-wrapper.js - EXO Player封装
//   app.js (本文件)  - TV UI 覆盖层、站点管理、搜索初始化、直播初始化、彩票初始化

// ===================== TV NEW UI =====================

// --- Override renderMovieHome for new UI ---
window._renderMovieHomeLegacy=renderMovieHome;
renderMovieHome=function(){
  var cats=document.getElementById('tvCatScroll'),grid=document.getElementById('tvGrid');
  if(!cats||!grid){return window._renderMovieHomeLegacy&&window._renderMovieHomeLegacy()}
  renderTvCats();
  if(movieState.cat==='直播'){renderLiveGrid(grid);return}
  var fav=lsGet('movie_favs'),kw=(movieState.keyword||'').trim().toLowerCase();
  var rawList=(window.getMovieData?window.getMovieData():window.MOVIE_DATA)||[];
  var list=rawList.filter(function(v){
    if(movieState.cat!=='推荐'&&v.cat!==movieState.cat)return false;
    if(kw&&String(v.title).toLowerCase().indexOf(kw)<0&&String(v.type).toLowerCase().indexOf(kw)<0)return false;
    return true;
  });
  console.log('[RENDER] movieState.cat='+movieState.cat+' rawList='+rawList.length+' filtered='+list.length+' sampleCat='+((rawList[0]&&rawList[0].cat)||'empty')+' sampleTitle='+((rawList[0]&&rawList[0].title)||'none'));
  if(rawList.length && !list.length && movieState.cat!=='推荐'){
    console.log('[RENDER] FILTERED OUT all', rawList.length, 'items: movieState.cat=', movieState.cat, 'sample item cat=', rawList[0].cat, 'sample title=', rawList[0].title);
  }
  // Android 16: 使用 requestIdleCallback 避免阻塞主线程
  var batchSize = 20;
  var batchIdx = 0;
  var batchList = list.slice();
  window.MOVIE_INDEX = window.MOVIE_INDEX || {};
  
  function renderBatch() {
    var end = Math.min(batchIdx + batchSize, batchList.length);
    for (; batchIdx < end; batchIdx++) {
      var v = batchList[batchIdx];
      window.MOVIE_INDEX[String(v.id)] = v;
      var on = fav.indexOf(v.id) >= 0;
      var img = v.pic ? 'background-image:url(\'' + v.pic + '\')' : '';
      var year = v.year || (v.title && v.title.match(/\d{4}/) ? v.title.match(/\d{4}/)[0] : '');
      var hd = v.quality || v.tag || 'HD';
      // 分批构建HTML字符串，减少DOM操作
    }
    // 只在最后一批执行DOM写入
    if (end >= batchList.length) {
      grid.innerHTML = batchList.map(function(v) {
        var on = fav.indexOf(v.id) >= 0;
        var img = v.pic ? 'background-image:url(\'' + v.pic + '\')' : '';
        var year = v.year || (v.title && v.title.match(/\d{4}/) ? v.title.match(/\d{4}/)[0] : '');
        var hd = v.quality || v.tag || 'HD';
        var escTitle = escapeHtml(v.title);
        var escId = escHtmlAttr(v.id);
        return '<div class="tv-card" data-movie-id="' + escId + '"><div class="tv-poster" style="' + img + '"><span class="tv-year-tag">' + year + '</span><span class="tv-hd-tag">' + hd + '</span><div class="tv-card-title">' + escTitle + '</div></div><div class="tv-card-info">' + (v.type || v.cat || '影视') + ' ' + (on ? ' · 已收藏' : '') + '</div></div>';
      }).join('');

      grid.querySelectorAll('.tv-card[data-movie-id]').forEach(function(card) {
        card.addEventListener('click', function() {
          var id = this.getAttribute('data-movie-id');
          if (window.moviePlay) window.moviePlay(id);
        });
      });
    } else {
      setTimeout(renderBatch, 0);
      return;
    }
  }
  renderBatch();
  renderMine();
  hideEmptyGuide();
  updateLoadMoreBtn();
  updateCacheSize();
};

function renderTvCats(){
  var scroll=document.getElementById('tvCatScroll'),dropdown=document.getElementById('tvCatGrid');
  if(!scroll)return;
  var catsToRender=[];
  var dbCats=movieState.dbCats||[];
  console.log('[RENDERTV] dbCats.length='+dbCats.length+' dbCats='+JSON.stringify(dbCats.slice(0,10)));
  if(dbCats.length){
    catsToRender=['推荐'].concat(dbCats);
  }else{
    catsToRender=MOVIE_CATS.slice(0,12);
    if(catsToRender.indexOf('推荐')<0)catsToRender.unshift('推荐');
  }
  var html=catsToRender.map(function(c){
    var isActive=movieState.cat===c?'active':'';
    var safe = escHtmlAttr(c);
    return '<button class="'+isActive+'" data-cat="'+safe+'">'+c+'</button>';
  }).join('');
  scroll.innerHTML=html;

  scroll.querySelectorAll('button[data-cat]').forEach(function(btn) {
    btn.addEventListener('click', function() {
      var cat = this.getAttribute('data-cat');
      if (window.tvSetCat) window.tvSetCat(cat);
    });
  });

  if(dropdown){
    var dropHtml=catsToRender.map(function(c){
      var isActive=movieState.cat===c?'active':'';
      var safe = escHtmlAttr(c);
      return '<button class="'+isActive+'" data-cat="'+safe+'">'+c+'</button>';
    }).join('');
    dropdown.innerHTML=dropHtml;

    dropdown.querySelectorAll('button[data-cat]').forEach(function(btn) {
      btn.addEventListener('click', function() {
        var cat = this.getAttribute('data-cat');
        if (window.tvSetCat) window.tvSetCat(cat);
        if (window.toggleCatDropdown) window.toggleCatDropdown();
      });
    });
  }
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
  if(c==='直播'){renderMovieHome();return}
  var grid=document.getElementById('tvGrid');
  var secName=document.getElementById('tvSectionName');
  if(secName)secName.textContent=c==='推荐'?'推荐':c;
  if(c==='推荐'){
    if(window.movieConfig && window.movieConfig.site && window.movieConfig.site.api && window.loadMovieList){
      window.loadMovieList('推荐', 1);
    } else {
      if(window.MOVIE_DATA&&window.MOVIE_DATA.length){grid.innerHTML='';renderMovieHome();return}
      else{window.MOVIE_DATA=[];renderMovieHome();return}
    }
    return;
  }
  // If a remote site is active, load via loadMovieList
  if(window.movieConfig && window.movieConfig.site && window.movieConfig.site.api){
    console.log('[APP] Remote site:', window.movieConfig.site.name, 'loadMovieList type:', typeof window.loadMovieList);
    if(window.loadMovieList){
      console.log('[APP] Calling loadMovieList with cat:', c, '(typeof:', typeof c, ')');
      window.loadMovieList(c, 1);
    } else {
      console.error('[APP] loadMovieList is undefined!');
    }
    return;
  }
  if(window.NCDB){
    var base=ncSourceBase()||FFZY_API_BASE.replace(/\/$/,'');
    NCDB.getSourceByBase(base).then(function(src){
      if(src&&src.id){
        NCDB.getMovies(src.id,c,60).then(function(list){
          if(list&&list.length){window.MOVIE_DATA=list;renderMovieHome();updateLoadMoreBtn();setMovieStatus('已加载 '+c+' 本地数据 ('+list.length+'部)',true);}
          else{window.MOVIE_DATA=[];renderMovieHome();updateLoadMoreBtn();setMovieStatus(c+' 暂无本地数据，请到「我的」页面采集',false);showEmptyGuide();}
        }).catch(function(){window.MOVIE_DATA=[];renderMovieHome();showEmptyGuide()});
      }else{window.MOVIE_DATA=[];renderMovieHome();updateLoadMoreBtn();setMovieStatus('请先采集数据',false);showEmptyGuide()}
    }).catch(function(){window.MOVIE_DATA=[];renderMovieHome();showEmptyGuide()});
    return;
  }
  window.MOVIE_DATA=[];renderMovieHome();
  setMovieStatus('请先在「我的」页面加载配置源或采集数据',false);
  showEmptyGuide();
}
function tvSearch(v){movieState.keyword=v||'';renderMovieHome()}
function tvShowAll(){movieState.cat='推荐';movieState.keyword='';var inp=document.getElementById('tvSearchInput');if(inp)inp.value='';renderMovieHome()}
function tvLoadNext(){
  if(movieState.usingRemote && window.loadMovieList){
    window.loadMovieList(movieState.cat, movieState.currentPage + 1);
  } else {
    alert('请先到「我的」页面采集更多数据');
  }
}

function toggleCatDropdown(){
  var el=document.getElementById('tvCatDropdown');
  if(!el)return;
  el.style.display=el.style.display==='none'?'block':'none';
}

// --- Panels ---
var panelDragState = { startY: 0, currentY: 0, dragging: false };

function handlePanelDragStart(e) {
  panelDragState.startY = e.touches[0].clientY;
  panelDragState.dragging = true;
}

function handlePanelDragMove(e) {
  if (!panelDragState.dragging) return;
  panelDragState.currentY = e.touches[0].clientY;
  var diff = panelDragState.currentY - panelDragState.startY;
  var panel = document.querySelector('.tv-repo-panel');
  if (panel && diff > 50) {
    panel.style.transition = 'transform 0.3s ease';
    panel.style.transform = 'translateY(' + diff + 'px)';
  }
}

function handlePanelDragEnd(e) {
  if (!panelDragState.dragging) return;
  panelDragState.dragging = false;
  var panel = document.querySelector('.tv-repo-panel');
  if (panel) {
    panel.style.transition = 'transform 0.3s ease';
    var diff = panelDragState.currentY - panelDragState.startY;
    if (diff > 100) {
      hideRepoPanel();
    } else {
      panel.style.transform = 'translateY(0)';
    }
    setTimeout(function() {
      if (panel) panel.style.transition = '';
    }, 300);
  }
  panelDragState.startY = 0;
  panelDragState.currentY = 0;
}

function showRepoPanel(){var el=document.getElementById('repoPanelOverlay');if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10);renderRepoPanel()}}
function hideRepoPanel(){var el=document.getElementById('repoPanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showSitePanel(){var el=document.getElementById('sitePanelOverlay');console.log('[app] showSitePanel: el=', !!el);if(el){el.style.display='flex';console.log('[app] set display:flex, computed:', getComputedStyle(el).display);setTimeout(function(){el.classList.add('show');console.log('[app] added show class');},10);var rp=window.renderSitePanel;console.log('[app] renderSitePanel from window:', typeof rp);if(rp){rp();console.log('[app] renderSitePanel called');}else{console.log('[app] renderSitePanel is undefined!')}}}// Debug panel injected here
function hideSitePanel(){var el=document.getElementById('sitePanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showMoreMenu(){var el=document.getElementById('morePanelOverlay');if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10)}}
function hideMorePanel(){var el=document.getElementById('morePanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showConfigInput(){var el=document.getElementById('tvConfigInputRow');if(el)el.style.display='flex'}
function toggleParserPanel(){var row=document.getElementById('parserSelectRow');if(row)row.style.display=row.style.display==='none'?'flex':'none'}
function showWindowManager(){alert('窗口管理功能开发中')}

// --- Long Press on Home Nav ---
var _lpTimer=null,_lpTriggered=false;
function startHomeLongPress(){_lpTriggered=false;_lpTimer=setTimeout(function(){_lpTriggered=true;showRepoPanel()},600)}
function endHomeLongPress(e){if(_lpTimer){clearTimeout(_lpTimer);_lpTimer=null}}
(function initLongPress(){
  var btn=document.getElementById('navHomeBtn');
  if(!btn)return;
  btn.addEventListener('touchstart',function(e){
    startHomeLongPress();
    e.preventDefault();
  },{passive:false});
  btn.addEventListener('touchend',function(e){
    endHomeLongPress(e);
    if(_lpTriggered){
      e.preventDefault();
      e.stopPropagation();
      _lpTriggered=false;
      return;
    }
    e.preventDefault();
    switchMainPage('home');
  },{passive:false});
  btn.addEventListener('mousedown',function(e){
    startHomeLongPress();
    e.preventDefault();
  });
  btn.addEventListener('mouseup',function(e){endHomeLongPress(e)});
  btn.addEventListener('click',function(e){
    if(_lpTriggered){e.stopPropagation();e.preventDefault();_lpTriggered=false;return}
    e.preventDefault();
    switchMainPage('home');
  });
  btn.onclick=null;
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
  fetch(url).then(function(r){if(!r.ok)throw 'HTTP '+r.status;return r.json()}).then(onOk).catch(onErr);
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
      window.MOVIE_DATA=[];
      renderTvCats();
      renderMovieHome();
      hideDbViewer();
    });
  }).catch(function(e){alert('清空失败：'+e)});
}

function clearMovieData(){
  if(window.NCDB){
    NCDB.getSources().then(function(sources){
      var promises=sources.map(function(s){return NCDB.clearSource(s.id)});
      promises.push(NCDB.clearHistory());
      promises.push(NCDB.clearFavorites());
      Promise.all(promises).then(function(){
        alert('已清空所有数据');
        movieState.dbCats=[];
        window.MOVIE_DATA=[];
        renderTvCats();
        renderMovieHome();
        hideDbViewer();
      });
    }).catch(function(e){alert('清空失败：'+e)});
  }else{
    lsSet('movie_favs',[]);lsSet('movie_fav_meta',[]);lsSet('movie_history',[]);
    window.MOVIE_DATA=[];
    renderTvCats();
    renderMovieHome();
    alert('已清空影视历史和收藏');
  }
}

// ===================== INIT =====================
(function(){
  function safeBuild(k){
    try{buildTab(k);return}
    catch(e){
      console.error(e);
      var tabEl=document.getElementById('tab-'+k);
      if(tabEl)tabEl.innerHTML='<div class="placeholder"><h2>加载失败</h2><p>本地数据已保留，请尝试重启应用或手动导出备份后再处理</p></div>';
    }
  }
  // 初始化本地数据库
  if(window.NCDB){
    NCDB.init().then(function(){
      updateDbRenderCats();
      // 尝试从数据库加载推荐数据
      NCDB.getSources().then(function(sources){
        if(sources&&sources.length){
          var src=sources[sources.length-1];
          NCDB.getMovies(src.id,'推荐',60).then(function(list){
            if(list&&list.length){
              window.MOVIE_DATA=list;
              movieState.cat='推荐';
              movieState.usingRemote=true;
              movieState.loaded=true;
              renderMovieHome();
              updateLoadMoreBtn();
              setMovieStatus('已加载本地数据 ('+list.length+'部)',true);
            }
          });
        }
      });
      // 自动加载上次保存的站点配置
      var savedUrl=localStorage.getItem('movie_config_url');
      if(savedUrl){
        console.log('[INIT] Auto-loading saved site config:', savedUrl);
        setTimeout(function(){
          if(window.loadMovieConfig)window.loadMovieConfig();
        },500);
      }
      // 初始化搜索页面
      if(window.initSearchPage)window.initSearchPage();
      // 初始化直播页面
      if(window.initLivePage)window.initLivePage();
      // 更新直播统计
      if(window.updateMineLiveCount)updateMineLiveCount();
    }).catch(function(e){console.error('数据库初始化失败',e);});
  }
  initMainDrawHistory();
  renderMovieHome();
  renderLibrary();
  renderMine();
  initTabSnap();
  // 彩票页面初始化
  if(typeof buildLotteryPage==='function'){buildLotteryPage('dlt');buildLotteryPage('ssq')}
  if(typeof buildLotteryPages==='function')buildLotteryPages();
  if(typeof updateAllLotteryCD==='function')updateAllLotteryCD();
  // 彩票模块事件委托 - 替代 onclick= 内联事件
  (function initLotteryEventDelegation(){
    var lotteryTabs=['dlt','ssq','qlc','fc3d','pl3','pl5','qxc','kl8'];
    lotteryTabs.forEach(function(id){
      var tabEl=document.getElementById('tab-'+id);
      if(!tabEl)return;
      tabEl.addEventListener('click',function(e){
        var target=e.target;
        while(target&&target!==tabEl&&target.nodeType===1){
          if(target.hasAttribute('data-action'))break;
          target=target.parentNode;
        }
        if(!target||target===tabEl)return;
        var action=target.getAttribute('data-action');
        var argsStr=target.getAttribute('data-args')||'';
        var rawArgs=argsStr.split(',').map(function(s){return s.trim()});
        if(!action)return;
        var args=rawArgs.map(function(a){
          if(a==='true')return true;
          if(a==='false')return false;
          if(/^\d+$/.test(a))return parseInt(a,10);
          return a;
        });
        if(typeof window[action]==='function'){
          try{window[action].apply(window,args)}catch(err){console.error('lottery action '+action+' error:',err)}
        }
      });
    });
  })();
  // 彩票倒计时 - 每2秒更新，降低CPU占用
  var _lotteryInterval = setInterval(function(){if(typeof updateCD==='function'){updateCD('dlt');updateCD('ssq')}if(typeof updateAllLotteryCD==='function')updateAllLotteryCD()},2000);
  var _sysEl=document.getElementById('systime');
  if(_sysEl){
    _sysEl.textContent=new Date().toLocaleString('zh-CN');
    var _sysInterval = setInterval(function(){var el=document.getElementById('systime');if(el)el.textContent=new Date().toLocaleString('zh-CN')},1000);
  }
  if(typeof fetchAPI==='function'){fetchAPI('dlt');fetchAPI('ssq')}
  var _fetchInterval = setInterval(function(){if(typeof fetchAPI==='function'){fetchAPI('dlt');fetchAPI('ssq')}},300000);
  
  // 暴露清理函数供页面切换时调用
  window._clearAppIntervals = function() {
    clearInterval(_lotteryInterval);
    clearInterval(_sysInterval);
    clearInterval(_fetchInterval);
  };
})();

// ===================== 搜索设置 =====================
var SEARCH_SETTINGS_KEY='search_settings';
function getDefaultSearchSettings(){
  return {threads:16,exact:false,viewMode:'list'};
}
function getSearchSettings(){
  try{return JSON.parse(localStorage.getItem(SEARCH_SETTINGS_KEY))||getDefaultSearchSettings()}
  catch(e){return getDefaultSearchSettings}
}
function saveSearchSettings(settings){
  localStorage.setItem(SEARCH_SETTINGS_KEY,JSON.stringify(settings));
  var td=document.getElementById('threadsDisplay');
  if(td)td.textContent=settings.threads||16;
}
function updateSearchThreadsDisplay(threads){
  var td=document.getElementById('threadsDisplay');
  if(td)td.textContent=threads||16;
}
function goToSearchPage(){
  switchMainPage('search');
  if(window.initSearchPage)initSearchPage();
}
function showSearchSettings(){
  var s=getSearchSettings();
  document.getElementById('threads16').checked=s.threads===16;
  document.getElementById('threads24').checked=s.threads===24;
  document.getElementById('threads32').checked=s.threads===32;
  document.getElementById('threads64').checked=s.threads===64;
  document.getElementById('exactToggle').checked=s.exact||false;
  document.getElementById('viewModeList').checked=s.viewMode==='list';
  document.getElementById('viewModeGrid').checked=s.viewMode==='grid';
  var el=document.getElementById('searchSettingsOverlay');
  if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10)}
}
function hideSearchSettings(){
  var s=getSearchSettings();
  if(document.getElementById('threads16').checked)s.threads=16;
  if(document.getElementById('threads24').checked)s.threads=24;
  if(document.getElementById('threads32').checked)s.threads=32;
  if(document.getElementById('threads64').checked)s.threads=64;
  s.exact=document.getElementById('exactToggle').checked;
  s.viewMode=document.getElementById('viewModeList').checked?'list':'grid';
  saveSearchSettings(s);
  var el=document.getElementById('searchSettingsOverlay');
  if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}
  // 更新搜索页面的线程数显示
  if(window.updateSearchThreadsDisplay)window.updateSearchThreadsDisplay(s.threads);
}

// ===================== 直播源管理 =====================
function showLiveSourceManager(){
  var el=document.getElementById('liveSourceOverlay');
  if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10);renderLiveSourceManager()}
}
function hideLiveSourceManager(){
  var el=document.getElementById('liveSourceOverlay');
  if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}
}
function renderLiveSourceManager(){
  if(!window.NCDB)return;
  var listEl=document.getElementById('liveSourceList');
  if(!listEl)return;
  NCDB.getLiveChannels().then(function(channels){
    if(!channels.length){listEl.innerHTML='<div style="text-align:center;color:#667788;padding:40px">暂无直播源</div>'}
    else{
      // 按 fromSite 分组统计
      var sourceMap = {};
      channels.forEach(function(ch) {
        var src = ch.fromSite || '本地';
        if (!sourceMap[src]) {
          sourceMap[src] = { name: src, count: 0, channels: [] };
        }
        sourceMap[src].count++;
        sourceMap[src].channels.push(ch);
      });
      
      var sources = Object.values(sourceMap).sort(function(a, b) {
        if (a.name === '本地') return -1;
        if (b.name === '本地') return 1;
        return a.name.localeCompare(b.name);
      });
      
      listEl.innerHTML = sources.map(function(src) {
        var escName = escapeHtml(src.name);
        var escCount = src.count;
        var sampleUrl = src.channels[0] ? escapeHtml(src.channels[0].url.substring(0, 50)) : '';
        return '<div class="live-src-item">' +
          '<div class="live-src-info">' +
            '<b>' + escName + '</b>' +
            '<span>' + escCount + ' 个频道</span>' +
            (sampleUrl ? '<small>' + sampleUrl + '...</small>' : '') +
          '</div>' +
          '<div class="live-src-actions">' +
            '<button class="live-src-btn" data-source-name="' + escapeAttr(src.name) + '">刷新</button>' +
          '</div>' +
        '</div>';
      }).join('');

      // 绑定刷新按钮
      listEl.querySelectorAll('.live-src-btn[data-source-name]').forEach(function(btn) {
        btn.addEventListener('click', function(e) {
          e.stopPropagation();
          var srcName = this.getAttribute('data-source-name');
          if (window.refreshLiveSource) window.refreshLiveSource(srcName);
        });
      });
    }
  });
}
function testLiveChannel(url){
  if(window.NativeHttp&&NativeHttp.httpGet){
    try{var text=NativeHttp.httpGet(url);if(text){alert('直播源可用\\n'+text.substring(0,200))}else{alert('直播源返回空')}}
    catch(e){alert('测试失败：'+e.message)}
    return;
  }
  fetch(url,{method:'HEAD'}).then(function(r){if(r.ok)alert('直播源可用');else alert('直播源不可用')}).catch(function(){alert('直播源测试失败')});
}
function deleteLiveChannel(id){
  if(!confirm('确定删除此直播源？'))return;
  if(window.NCDB)NCDB.deleteLiveChannel(id).then(function(){renderLiveSourceManager();updateMineLiveCount()});
}

// ===================== 配置导入/导出 =====================
function showImportExport(){
  var el=document.getElementById('importExportOverlay');
  if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10)}
}
function hideImportExport(){
  var el=document.getElementById('importExportOverlay');
  if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}
}
function exportConfig(){
  var data={};
  var keys=['movie_config','search_settings','live_sources'];
  keys.forEach(function(k){var v=localStorage.getItem(k);if(v)data[k]=v});
  if(window.NCDB){
    NCDB.getSources().then(function(sources){
      NCDB.getLiveChannels().then(function(channels){
        data.sources=sources;data.channels=channels;
        var blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'});
        var url=URL.createObjectURL(blob);
        var a=document.createElement('a');a.href=url;a.download='movie_app_config_'+new Date().toISOString().slice(0,10)+'.json';a.click();URL.revokeObjectURL(url);
        hideImportExport();
      });
    });
  }else{
    var blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'});
    var url=URL.createObjectURL(blob);
    var a=document.createElement('a');a.href=url;a.download='movie_app_config_'+new Date().toISOString().slice(0,10)+'.json';a.click();URL.revokeObjectURL(url);
    hideImportExport();
  }
}
function importConfig(){
  var inp=document.getElementById('importFileInput');
  if(!inp||!inp.files.length){alert('请选择文件');return}
  var reader=new FileReader();
  reader.onload=function(e){
    try{
      var data=JSON.parse(e.target.result);
      var keys=['sources','channels','movie_config','search_settings','live_sources'];
      keys.forEach(function(k){if(data[k])localStorage.setItem(k,typeof data[k]==='string'?data[k]:JSON.stringify(data[k]))});
      alert('配置导入成功！请刷新页面。');
      hideImportExport();
    }catch(err){alert('导入失败：'+err.message)}
  };
  reader.readAsText(inp.files[0]);
}

// ===================== 清空数据 =====================
function clearMovieData(){
  if(!confirm('确定要清空所有影视历史、收藏和播放记录吗？此操作不可恢复。'))return;
  if(!window.NCDB){alert('数据库未初始化');return}
  NCDB.getSources().then(function(sources){
    var promises=[];
    sources.forEach(function(s){promises.push(NCDB.clearSource(s.id))});
    promises.push(NCDB.clearHistory());
    promises.push(NCDB.clearFavorites());
    Promise.all(promises).then(function(){
      window.MOVIE_DATA=[];
      movieState.history=[];
      movieState.favorites=[];
      renderMovieHome();
      renderLibrary();
      renderMine();
      alert('已清空所有数据');
    });
  }).catch(function(e){alert('清空失败：'+e)});
}

// ===================== 启动加载画面 =====================
window.addEventListener('load', function() {
  var overlay = document.getElementById('appLoadingOverlay');
  if (overlay) {
    setTimeout(function() { overlay.classList.add('hidden'); }, 300);
  }
});

// ===================== 网络状态检测 =====================
(function() {
  function updateNetworkStatus() {
    var bar = document.getElementById('networkStatusBar');
    if (!bar) return;
    if (navigator.onLine === false) {
      bar.style.display = 'block';
    } else {
      bar.style.display = 'none';
    }
  }
  window.addEventListener('online', updateNetworkStatus);
  window.addEventListener('offline', updateNetworkStatus);
  updateNetworkStatus();
})();
