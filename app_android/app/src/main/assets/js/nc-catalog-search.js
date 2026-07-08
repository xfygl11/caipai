// NewCloud 分类与搜索模块
(function(){
  var ROOT_NAMES=['推荐'];
  var searchMode=false;
  var lastKeyword='';
  var swipeStartX=0,swipeStartY=0,swipeLock=false,swipeMode='';

  function rootClasses(){
    var cls=(movieConfig&&movieConfig.classes&&movieConfig.classes.length)?movieConfig.classes:FFZY_CLASSES;
    var roots=(cls||[]).filter(function(c){return String(c.type_pid)==='0'}).map(function(c){return window.normalizeCatName?normalizeCatName(c.type_name):c.type_name});
    return ['推荐'].concat(roots.length?roots:['电影片','连续剧','综艺片','动漫片']);
  }
  function classByName(name){
    var cls=(movieConfig&&movieConfig.classes&&movieConfig.classes.length)?movieConfig.classes:FFZY_CLASSES;
    for(var i=0;i<(cls||[]).length;i++){
      if(cls[i].type_name===name)return cls[i];
    }
    return null;
  }
  function childClasses(parentName){
    var cls=(movieConfig&&movieConfig.classes&&movieConfig.classes.length)?movieConfig.classes:FFZY_CLASSES;
    if(!cls||!cls.length)return [];
    if(parentName==='推荐')return (cls||[]).filter(function(c){return String(c.type_pid)==='0'});
    var p=classByName(parentName);
    if(!p)return [];
    return (cls||[]).filter(function(c){return String(c.type_pid)===String(p.type_id)});
  }
  function setSectionTitle(text,search){
    var sec=document.getElementById('tvSectionName');
    var btn=document.querySelector('.tv-section-more');
    var page=document.querySelector('.tv-page');
    if(sec)sec.textContent=text;
    if(page)page.classList.toggle('nc-search-mode',!!search);
    if(btn){
      btn.textContent=search?'返回推荐':'全部 ›';
      btn.className=search?'nc-search-back':'tv-section-more';
      btn.onclick=function(){tvShowAll()};
    }
  }

  window.renderTvCats=function(){
    var scroll=document.getElementById('tvCatScroll'),dropdown=document.getElementById('tvCatGrid');
    if(!scroll)return;
    ROOT_NAMES=rootClasses();
    MOVIE_CATS=ROOT_NAMES.slice();
    scroll.innerHTML=ROOT_NAMES.map(function(c){
      return '<button class="'+(movieState.cat===c&&!searchMode?'active':'')+'" onclick="tvSetCat(\''+c+'\')">'+c.replace('片','')+'</button>';
    }).join('');
    if(dropdown){
      var kids=childClasses(movieState.cat);
      if(!kids.length&&movieState.cat==='推荐')kids=((movieConfig&&movieConfig.classes&&movieConfig.classes.length)?movieConfig.classes:FFZY_CLASSES||[]).filter(function(c){return String(c.type_pid)!=='0'}).slice(0,24);
      dropdown.innerHTML='<div class="nc-cat-level-title">'+(movieState.cat==='推荐'?'二级分类':'三级分类')+'</div>'+
        (kids.length?kids.map(function(c){
          return '<button onclick="tvSetCat(\''+c.type_name+'\');toggleCatDropdown()">'+c.type_name+'</button>';
        }).join(''):'<button>暂无子分类</button>');
    }
  };

  var oldRenderMovieHome=window.renderMovieHome;
  window.renderMovieHome=function(){
    oldRenderMovieHome.apply(this,arguments);
    renderTvCats();
    if(searchMode) setSectionTitle('搜索：'+lastKeyword,true);
  };

  function directSeedList(cat){
    var base=String((window.ncSourceBase&&ncSourceBase())||window.NC_CURRENT_API_URL||'');
    if(base&&!/ffzyapi|cj\.ffzy/i.test(base))return null;
    if(window.NCCache&&NCCache.listFromCatCache){
      var cached=NCCache.listFromCatCache(cat,1);
      if(cached&&cached.length)return cached;
    }
    if(!window.FFZY_SEED)return null;
    var tid=ffzyClassId(cat)||'';
    if(cat!=='推荐'&&!tid)return null;
    var data=ffzySeedResponse(cat==='推荐'||!tid?'ac=detail':'ac=detail&t='+encodeURIComponent(tid));
    var list=(data&&data.list||[]).map(function(v){return normalizeVod(v,cat)}).slice(0,80);
    return list.length?list:null;
  }

  window.tvSetCat=function(c){
    searchMode=false;
    lastKeyword='';
    movieState.keyword='';
    var input=document.getElementById('tvSearchInput');
    if(input)input.value='';
    movieState.cat=c;
    setSectionTitle(c,false);
    if(c==='直播'){renderMovieHome();return}
    if(movieState.usingRemote){
      var seed=directSeedList(c);
      if(seed){
        MOVIE_DATA=seed;
        movieState.cat=c;
        movieState.currentPage=1;
        movieState.hasMore=false;
        renderMovieHome();
        updateLoadMoreBtn();
      }
      // 不再自动联网采集，只显示本地数据
      setTimeout(function(){
        if(movieState.cat===c)centerCatButton(c);
      },80);
    }else renderMovieHome();
  };

  window.tvShowAll=function(){
    searchMode=false;
    lastKeyword='';
    movieState.cat='推荐';
    movieState.keyword='';
    var inp=document.getElementById('tvSearchInput');
    if(inp)inp.value='';
    setSectionTitle('推荐',false);
    if(movieState.usingRemote){
      var seed=directSeedList('推荐');
      if(seed){
        MOVIE_DATA=seed;
        movieState.currentPage=1;
        movieState.hasMore=false;
        renderMovieHome();
        updateLoadMoreBtn();
      }
      // 不再自动联网采集
    }
    else renderMovieHome();
  };

  window.tvSearch=function(v){
    movieState.keyword=v||'';
    var kw=(v||'').trim();
    clearTimeout(movieState.searchTimer);
    if(!kw){
      if(searchMode)tvShowAll();
      return;
    }
    movieState.searchTimer=setTimeout(function(){runTvSearch(kw)},450);
  };

  window.runTvSearch=function(kw){
    lastKeyword=kw;
    searchMode=true;
    movieState.cat='搜索';
    movieState.keyword=kw;
    setSectionTitle('搜索：'+kw,true);
    saveSearchHistory(kw);
    if(movieState.usingRemote){
      var seedData=window.FFZY_SEED?ffzySeedResponse('ac=detail&wd='+encodeURIComponent(kw)):null;
      var seedList=(seedData&&seedData.list||[]).map(function(v){return normalizeVod(v,'搜索')}).slice(0,80);
      if(seedList.length){
        MOVIE_DATA=seedList;
        movieState.hasMore=false;
        movieState.currentPage=1;
        renderMovieHome();
        updateLoadMoreBtn();
        setSectionTitle('搜索：'+kw,true);
      }
      ffzyFetch('ac=detail&wd='+encodeURIComponent(kw)).then(function(data){
        var found=(data.list||[]).map(function(v){return normalizeVod(v,'搜索')}).slice(0,80);
        if(!found.length&&seedList.length)found=seedList;
        MOVIE_DATA=found;
        movieState.hasMore=false;
        movieState.currentPage=1;
        movieState.cat='搜索';
        movieState.keyword=kw;
        searchMode=true;
        renderMovieHome();
        updateLoadMoreBtn();
        setSectionTitle('搜索：'+kw,true);
      }).catch(function(){
        MOVIE_DATA=seedList;
        movieState.hasMore=false;
        movieState.currentPage=1;
        renderMovieHome();
        updateLoadMoreBtn();
        setSectionTitle('搜索：'+kw,true);
      });
    }else{
      renderMovieHome();
    }
  };

  function centerCatButton(name){
    var scroll=document.getElementById('tvCatScroll');
    if(!scroll)return;
    var btns=scroll.querySelectorAll('button');
    for(var i=0;i<btns.length;i++){
      if(btns[i].textContent===String(name).replace('片','')||btns[i].textContent===name){
        var left=btns[i].offsetLeft-(scroll.clientWidth-btns[i].offsetWidth)/2;
        if(scroll.scrollTo)scroll.scrollTo({left:Math.max(0,left),behavior:'smooth'});
        else scroll.scrollLeft=Math.max(0,left);
        break;
      }
    }
  }

  function animateContent(dir){
    var c=document.querySelector('.tv-content');
    if(!c)return;
    c.classList.remove('nc-slide-left','nc-slide-right');
    void c.offsetWidth;
    c.classList.add(dir>0?'nc-slide-left':'nc-slide-right');
    setTimeout(function(){c.classList.remove('nc-slide-left','nc-slide-right')},260);
  }

  function currentRootIndex(){
    ROOT_NAMES=rootClasses();
    var idx=ROOT_NAMES.indexOf(movieState.cat);
    if(idx<0){
      var cls=classByName(movieState.cat);
      if(cls&&String(cls.type_pid)!=='0'){
        for(var i=0;i<ROOT_NAMES.length;i++){
          var r=classByName(ROOT_NAMES[i]);
          if(r&&String(r.type_id)===String(cls.type_pid))return i;
        }
      }
      return 0;
    }
    return idx;
  }

  function switchRootByStep(step){
    if(searchMode)return;
    ROOT_NAMES=rootClasses();
    var idx=currentRootIndex();
    var next=Math.max(0,Math.min(ROOT_NAMES.length-1,idx+step));
    if(next===idx)return;
    animateContent(step);
    tvSetCat(ROOT_NAMES[next]);
  }

  function bindPageSwipe(){
    var page=document.querySelector('.tv-page');
    if(!page||page.dataset.ncSwipe)return;
    page.dataset.ncSwipe='1';
    page.addEventListener('touchstart',function(e){
      if(!e.touches||!e.touches[0])return;
      if(e.target.closest('.video-modal,.tv-panel-overlay,.tv-cat-dropdown,.tv-cat-scroll,.episode-list,.parser-btns,.movie-tabs,.tw,.pred-line,.rand-line,.lot-line,.ddet'))return;
      swipeStartX=e.touches[0].clientX;
      swipeStartY=e.touches[0].clientY;
      swipeLock=false;
      swipeMode='';
    },{passive:true});
    page.addEventListener('touchmove',function(e){
      if(!e.touches||!e.touches[0]||swipeLock)return;
      var dx=e.touches[0].clientX-swipeStartX,dy=e.touches[0].clientY-swipeStartY;
      if(!swipeMode&&Math.abs(dx)>18){
        swipeMode=Math.abs(dx)>Math.abs(dy)*1.35?'x':'y';
      }
      if(swipeMode==='x'){
        if(e.cancelable)e.preventDefault();
        e.stopPropagation();
      }
      if(Math.abs(dx)>90&&Math.abs(dx)>Math.abs(dy)*1.8){
        if(e.cancelable)e.preventDefault();
        e.stopPropagation();
        swipeLock=true;
        switchRootByStep(dx<0?1:-1);
      }
    },{passive:false});
  }

  window.renderSitePanel=function(){
    var grid=document.getElementById('siteGrid');
    if(!grid)return;
    var sites=(window.NCRepoSources&&NCRepoSources())||[];
    if(!sites.length){grid.innerHTML='<div class="nc-repo-empty">暂无采集源，请到仓库管理添加。</div>';return}
    var active=String(window.NC_CURRENT_API_URL||'').replace(/\/$/,'');
    grid.innerHTML=sites.map(function(s,i){
      var url=String(s.url||''),on=String(url).replace(/\/$/,'')===active;
      return '<div class="tv-site-item '+(on?'active':'')+'" onclick="selectSite(\''+encodeURIComponent(url)+'\')">'+
        '<div class="tv-site-icon">'+(i+1)+'</div>'+
        '<div class="tv-site-info"><span class="tv-site-name">'+(s.name||('采集源'+(i+1)))+'</span><span class="tv-site-desc">'+url+'</span></div>'+
        '</div>';
    }).join('');
  };

  window.selectSite=function(key){
    var url=decodeURIComponent(key||'');
    if(window.NCSelectRepoSourceByUrl)NCSelectRepoSourceByUrl(url);
    hideSitePanel();
  };

  function bindSearchEnter(){
    var input=document.getElementById('tvSearchInput');
    if(!input||input.dataset.ncBind)return;
    input.dataset.ncBind='1';
    input.addEventListener('keydown',function(e){
      if(e.key==='Enter'){
        e.preventDefault();
        var kw=input.value.trim();
        if(kw)runTvSearch(kw);
      }
    });
  }

  var oldFfzyInit=window.ffzyInit;
  if(oldFfzyInit){
    window.ffzyInit=function(){
      var ret=oldFfzyInit.apply(this,arguments);
      setTimeout(function(){ROOT_NAMES=rootClasses();renderTvCats()},600);
      return ret;
    };
  }

  setTimeout(function(){bindSearchEnter();bindPageSwipe();renderTvCats()},500);
})();
