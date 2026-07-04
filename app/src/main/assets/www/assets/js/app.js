// 个人助手 - 核心入口 v11.0
// 只保留影视主页 + 彩票功能

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
    var activeBtn=scroll.querySelector('.active');
    if(activeBtn){activeBtn.scrollIntoView({behavior:'smooth',inline:'center',block:'nearest'})}
    return;
  }
  var html=MOVIE_CATS.map(function(c){return '<button class="'+(movieState.cat===c?'active':'')+'" onclick="tvSetCat(\''+c+'\')">'+c+'</button>'}).join('');
  scroll.innerHTML=html;
  if(dropdown)dropdown.innerHTML=MOVIE_CATS.map(function(c){return '<button class="'+(movieState.cat===c?'active':'')+'" onclick="tvSetCat(\''+c+'\');toggleCatDropdown()">'+c+'</button>'}).join('');
  var activeBtn=scroll.querySelector('.active');
  if(activeBtn){activeBtn.scrollIntoView({behavior:'smooth',inline:'center',block:'nearest'})}
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
  var grid=document.getElementById('tvGrid');
  if(grid){grid.style.opacity='0.3';grid.style.transition='opacity .15s'}
  movieSetCat(c);
  renderTvCats();
  setTimeout(function(){if(grid)grid.style.opacity='1'},200);
}
function tvSearch(v){
  movieState.keyword=v||'';
  var kw=(v||'').trim();
  if(!kw){tvShowAll();return}
  saveSearchHistory(kw);
  movieSearch(v);
  renderTvCats();
}
function tvShowAll(){
  movieState.cat='推荐';movieState.keyword='';
  var inp=document.getElementById('tvSearchInput');if(inp)inp.value='';
  movieShowAll();
  renderTvCats();
}
function tvLoadNext(){
  if(typeof loadMovieList==='function'){
    loadMovieList(movieState.cat||'推荐');
  }else{
    alert('请先配置影视源（点击主页顶部按钮选择源）');
  }
}

function toggleCatDropdown(){
  var el=document.getElementById('tvCatDropdown');
  if(!el)return;
  el.style.display=el.style.display==='none'?'block':'none';
}

// --- Panels ---
function showRepoPanel(){}
function hideRepoPanel(){var el=document.getElementById('repoPanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showSitePanel(){var el=document.getElementById('sitePanelOverlay');if(el){el.style.display='flex';setTimeout(function(){el.classList.add('show')},10);renderSitePanel()}}
function hideSitePanel(){var el=document.getElementById('sitePanelOverlay');if(el){el.classList.remove('show');setTimeout(function(){el.style.display='none'},250)}}
function showMoreMenu(){}
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

// ===================== INIT =====================
(function(){
  function safeBuild(k){
    try{buildTab(k);return}
    catch(e){
      console.error(k,'build failed',e);
      var tabEl=document.getElementById('tab-'+k);
      if(tabEl)tabEl.innerHTML='<div class="placeholder"><h2>加载失败</h2></div>';
    }
  }

  renderMovieHome();
  renderTvCats();
  safeBuild('dlt');
  safeBuild('ssq');
  buildLotteryPages();
  initTabSnap();
  updateAllLotteryCD();
  setInterval(function(){updateCD('dlt');updateCD('ssq');updateAllLotteryCD()},1000);

  var _sysEl=document.getElementById('systime');
  if(_sysEl){
    _sysEl.textContent=new Date().toLocaleString('zh-CN');
    setInterval(function(){
      var el=document.getElementById('systime');
      if(el)el.textContent=new Date().toLocaleString('zh-CN');
    },1000);
  }

  fetchAPI('dlt');fetchAPI('ssq');
  setInterval(function(){fetchAPI('dlt');fetchAPI('ssq')},300000);
})();


// --- 渲染我的页面时刷新采集源选择 ---
