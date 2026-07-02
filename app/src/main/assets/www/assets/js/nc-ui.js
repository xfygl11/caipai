// NewCloud UI 辅助模块：骨架屏、空状态、错误、搜索历史、面板、Tab切换、下拉刷新、长按

// === UI Helper: Skeleton / Empty / Error ===
function showSkeleton(){var el=document.getElementById('skeletonGrid');if(el)el.style.display='grid'}
function hideSkeleton(){var el=document.getElementById('skeletonGrid');if(el)el.style.display='none'}
function showEmptyGuide(){var el=document.getElementById('emptyGuide');if(el)el.classList.add('show')}
function hideEmptyGuide(){var el=document.getElementById('emptyGuide');if(el)el.classList.remove('show')}
function showError(msg){
  var el=document.getElementById('errorState');var txt=document.getElementById('errorStateText');
  if(txt)txt.textContent=msg||'加载失败';
  if(el)el.classList.add('show');
}
function hideError(){var el=document.getElementById('errorState');if(el)el.classList.remove('show')}
function retryLoad(){hideError();if(movieState.usingRemote){movieRefresh()}else{renderMovieHome()}}

// === Search History ===
function loadSearchHistory(){
  try{return JSON.parse(localStorage.getItem('movie_search_history')||'[]')}catch(e){return[]}
}
function saveSearchHistory(kw){
  if(!kw||!kw.trim())return;
  var list=loadSearchHistory();
  var filtered=list.filter(function(x){return x!==kw.trim()});
  filtered.unshift(kw.trim());
  try{localStorage.setItem('movie_search_history',JSON.stringify(filtered.slice(0,20)))}catch(e){}
}
function clearSearchHistory(){try{localStorage.removeItem('movie_search_history')}catch(e){}hideSearchHistory()}
function showSearchHistory(){
  var box=document.getElementById('searchHistoryDropdown');
  if(!box)return;
  var list=loadSearchHistory();
  if(!list.length){
    box.innerHTML='<div class="sh-empty">暂无搜索历史</div>';
    box.style.display='block';return;
  }
  var h='<div class="sh-header"><span>搜索历史</span><button onclick="clearSearchHistory();event.stopPropagation()">清空</button></div>';
  for(var i=0;i<Math.min(list.length,10);i++){
    h+='<div class="sh-item" onclick="selectSearchHistoryItem(\''+list[i].replace(/'/g,"\\'")+'\');event.stopPropagation()">'+list[i]+'</div>';
  }
  box.innerHTML=h;
  box.style.display='block';
}
function hideSearchHistory(){var box=document.getElementById('searchHistoryDropdown');if(box)box.style.display='none'}
function selectSearchHistoryItem(kw){
  var inp=document.getElementById('movieSearchInput');
  if(inp)inp.value=kw;
  hideSearchHistory();
  movieSearch(kw);
}

// === Config Import/Export ===
function exportConfig(){
  var cfg={
    config_url:localStorage.getItem('movie_config_url')||'',
    sites:movieConfig.sites||[],
    classes:movieConfig.classes||[],
    parses:movieConfig.parses||[],
    lives:movieConfig.lives||[],
    favs:lsGet('movie_favs'),
    fav_meta:lsGet('movie_fav_meta'),
    history:lsGet('movie_history')
  };
  var json=JSON.stringify(cfg,null,2);
  var textArea=document.createElement('textarea');
  textArea.value=json;
  textArea.style.position='fixed';
  textArea.style.left='-9999px';
  document.body.appendChild(textArea);
  textArea.select();
  document.execCommand('copy');
  document.body.removeChild(textArea);
  alert('配置JSON已复制到剪贴板（'+json.length+'字符），可粘贴保存');
}
function showImportExport(){
  var json=prompt('粘贴配置JSON文本导入（或留空仅导出到剪贴板）:','');
  if(json&&json.trim()){
    try{
      var cfg=JSON.parse(json.trim());
      if(cfg.config_url){
        localStorage.setItem('movie_config_url',cfg.config_url);
        var inp=getMovieConfigInput();
        if(inp)inp.value=cfg.config_url;
      }
      if(Array.isArray(cfg.favs)){lsSet('movie_favs',cfg.favs)}
      if(Array.isArray(cfg.fav_meta)){lsSet('movie_fav_meta',cfg.fav_meta)}
      if(Array.isArray(cfg.history)){lsSet('movie_history',cfg.history)}
      alert('配置导入成功');
      switchMainPage('home');
      renderMovieHome();renderMine();
    }catch(e){alert('JSON解析失败: '+e)}
  }else{exportConfig()}
}

// === Preset Sources ===
var PRESET_SOURCES=[
  {name:'饭太硬',url:'http://www.饭太硬.net/tv'},
  {name:'欧歌',url:'https://ouge.open.eu.org/ITV'},
  {name:'肥猫',url:'http://www.肥猫.love'},
  {name:'100km.top',url:'http://100km.top/0'},
  {name:'多多',url:'https://yydsys.top/duo'},
  {name:'运输车',url:'https://cf.weixine.top/运输车'},
  {name:'小米',url:'http://xh.wwwtt.cc/tv'},
  {name:'月光',url:'https://jihulab.com/ygbh/TVBox/-/raw/main/月光.json'}
];
function showPresetSources(){
  var msg='选择预设源（输入序号）:\n';
  for(var i=0;i<PRESET_SOURCES.length;i++){
    msg+=(i+1)+'. '+PRESET_SOURCES[i].name+' - '+PRESET_SOURCES[i].url+'\n';
  }
  var n=prompt(msg,'');
  if(!n)return;
  var idx=parseInt(n,10)-1;
  if(PRESET_SOURCES[idx]){
    var inp=getMovieConfigInput();
    if(inp)inp.value=PRESET_SOURCES[idx].url;
    switchMainPage('home');
    loadMovieConfig();
  }
}

// === Settings Center ===
function clearAllCache(){
  if(!confirm('确定要清理所有缓存吗？包括影片缓存、解析缓存、播放进度等'))return;
  var keysToRemove=[];
  for(var i=0;i<localStorage.length;i++){
    var k=localStorage.key(i);
    if(k&&(k.indexOf('parse_cache_')===0||k.indexOf('play_progress_')===0||k.indexOf('movie_list_cache_')===0)){
      keysToRemove.push(k);
    }
  }
  for(var j=0;j<keysToRemove.length;j++){
    localStorage.removeItem(keysToRemove[j]);
  }
  movieState.listCache={};
  alert('已清理 '+keysToRemove.length+' 条缓存数据');
  updateCacheSize();
}
function getCacheSize(){
  var total=0;
  for(var i=0;i<localStorage.length;i++){
    var k=localStorage.key(i);
    if(k&&(k.indexOf('parse_cache_')===0||k.indexOf('play_progress_')===0)){
      try{total+=localStorage.getItem(k).length}catch(e){}
    }
  }
  if(total<1024)return total+'B';
  if(total<1024*1024)return (total/1024).toFixed(1)+'KB';
  return (total/1024/1024).toFixed(1)+'MB';
}
function updateCacheSize(){
  var el=document.getElementById('cacheSizeText');
  if(el)el.textContent=getCacheSize();
}
function showAbout(){
  alert('个人助手\n版本 9.9 (Build 74)\n\n功能:\n- 修复分类获取：API无class时自动从种子或list数据提取分类\n- 采集去重：与数据库对比，只保存新数据，满100条新数据才切换分类\n- 修复非凡采集：使用用户选择的源地址请求\n- 修复数据库查看：分类下拉框异步加载完成后再渲染数据\n\n技术: WebIDE (file:// WebView)\n数据: IndexedDB + localStorage');
}

// === Pull to Refresh ===
(function(){
  var startX=0,startY=0,pulling=false,pullEl=null,pullEligible=false;
  function initPullRefresh(){
    pullEl=document.createElement('div');
    pullEl.className='pull-refresh-indicator';
    pullEl.textContent='下拉刷新';
    document.body.appendChild(pullEl);
    var homeEl=document.getElementById('main-home');
    if(homeEl){
      homeEl.addEventListener('touchstart',function(e){
        pullEligible=window.scrollY<=0&&!e.target.closest('.video-modal,.tv-panel-overlay,.tv-cat-dropdown,.search-history-dropdown,.tv-cat-scroll,.episode-list,.parser-btns,.movie-tabs,.tw,.pred-line,.rand-line,.lot-line,.ddet,.video-player-wrap,.art-video-player');
        startX=e.touches[0].clientX;startY=e.touches[0].clientY;pulling=false;
      },{passive:true});
      homeEl.addEventListener('touchmove',function(e){
        if(!pullEligible)return;
        var dx=e.touches[0].clientX-startX,dy=e.touches[0].clientY-startY;
        if(dy>8&&dy>Math.abs(dx)*1.25&&window.scrollY<=0){if(e.cancelable)e.preventDefault();e.stopPropagation();}
        if(dy>80&&window.scrollY<=0&&pulling===false){
          if(e.cancelable)e.preventDefault();
          e.stopPropagation();
          pulling=true;
          if(pullEl)pullEl.classList.add('show');
          if(pullEl)pullEl.textContent='释放刷新';
        }else if(pulling&&dy<40){
          if(e.cancelable)e.preventDefault();
          e.stopPropagation();
          pulling=false;
          if(pullEl)pullEl.classList.remove('show');
        }
      },{passive:false});
      homeEl.addEventListener('touchend',function(e){
        if(pulling){
          if(e&&e.cancelable)e.preventDefault();
          if(e)e.stopPropagation();
          pulling=false;
          pullEligible=false;
          if(pullEl){pullEl.textContent='刷新中...';setTimeout(function(){pullEl.classList.remove('show')},1200)}
          movieRefresh();
        }
      },{passive:false});
    }
  }
  if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',initPullRefresh)}
  else{initPullRefresh()}
})();

// === Enhanced renderMovieHome with empty/error state ===
(function(){
  var orig=renderMovieHome;
  window._renderMovieHomeCore=orig;
  renderMovieHome=function(){
    orig();
    if(!movieState.usingRemote&&!movieState.loaded){
      showEmptyGuide();
    }else{
      hideEmptyGuide();
    }
    updateLoadMoreBtn();
    updateCacheSize();
  };
})();

// TAB SWITCH
function smoothScrollX(box,left){
  if(!box)return;
  if(box.scrollTo)box.scrollTo({left:left,behavior:'smooth'});
  else box.scrollLeft=left;
}
function centerNavButton(btn){
  var nav=document.querySelector('.nav-bar');
  if(!nav||!btn)return;
  var left=btn.offsetLeft-(nav.clientWidth-btn.offsetWidth)/2;
  left=Math.max(0,Math.min(left,nav.scrollWidth-nav.clientWidth));
  smoothScrollX(nav,left);
}
function switchTab(tab){
  var root=document.getElementById('tabsViewport'),el=document.getElementById('tab-'+tab);
  if(root&&el){
    var left=el.offsetLeft-root.offsetLeft;
    smoothScrollX(root,left);
  }
  setActiveTab(tab);
}
function setActiveTab(tab){
  var btns=document.querySelectorAll('.nav-btn'),tabs=document.querySelectorAll('.tab-content');
  for(var i=0;i<btns.length;i++)btns[i].classList.toggle('active',btns[i].getAttribute('data-tab')===tab);
  for(var j=0;j<tabs.length;j++)tabs[j].classList.toggle('active',tabs[j].getAttribute('data-tab')===tab);
  var active=document.querySelector('.nav-btn.active');
  centerNavButton(active);
}
function initTabSnap(){
  var root=document.getElementById('tabsViewport');
  if(!root)return;
  var snapTimer=null;
  function snapToNearest(){
    var idx=Math.round(root.scrollLeft/root.clientWidth),tabs=document.querySelectorAll('.tab-content');
    if(tabs[idx]){
      var left=tabs[idx].offsetLeft-root.offsetLeft;
      if(Math.abs(root.scrollLeft-left)>1)root.scrollTo({left:left,behavior:'smooth'});
      if(tabs[idx].dataset)setActiveTab(tabs[idx].dataset.tab);
    }
  }
  root.addEventListener('scroll',function(){
    clearTimeout(snapTimer);
    snapTimer=setTimeout(snapToNearest,120);
  },{passive:true});
  if('IntersectionObserver' in window){
    var io=new IntersectionObserver(function(entries){
      var best=null;
      entries.forEach(function(e){if(e.isIntersecting&&(!best||e.intersectionRatio>best.intersectionRatio))best=e});
      if(best&&best.target&&best.target.dataset)setActiveTab(best.target.dataset.tab);
    },{root:root,threshold:[.55,.7,.85]});
    document.querySelectorAll('.tab-content').forEach(function(el){io.observe(el)});
  }else{
    var ticking=false;
    root.addEventListener('scroll',function(){
      if(ticking)return;ticking=true;
      requestAnimationFrame(function(){
        var idx=Math.round(root.scrollLeft/root.clientWidth),tabs=document.querySelectorAll('.tab-content');
        if(tabs[idx]&&tabs[idx].dataset)setActiveTab(tabs[idx].dataset.tab);
        ticking=false;
      });
    },{passive:true});
  }
}

// === Main Page Switch ===
function switchMainPage(page){
  document.querySelectorAll('.main-page').forEach(function(el){el.classList.toggle('active',el.getAttribute('data-main')===page)});
  document.querySelectorAll('.main-nav-btn').forEach(function(el){el.classList.toggle('active',el.getAttribute('data-main')===page)});
  if(page==='library')renderLibrary();
  if(page==='mine')renderMine();
  window.scrollTo({top:0,behavior:'smooth'});
}

// === Movie Refresh / Search / ShowAll ===
function movieRefresh(){if(movieState.usingRemote)loadMovieList(movieState.cat);else renderMovieHome()}
function movieSearch(v){
  movieState.keyword=v||'';
  if(movieState.usingRemote&&movieState.keyword.trim()){
    clearTimeout(movieState.searchTimer);
    movieState.searchTimer=setTimeout(function(){searchMovieRemote(movieState.keyword.trim())},500);
  }else{renderMovieHome()}
}
function movieShowAll(){movieState.cat='推荐';movieState.keyword='';var inp=document.getElementById('movieSearchInput');if(inp)inp.value='';if(movieState.usingRemote)loadMovieList('推荐');else renderMovieHome()}
