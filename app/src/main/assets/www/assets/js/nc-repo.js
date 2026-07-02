// NewCloud 仓库/API 管理模块
(function(){
  var STORE_KEY='nc_api_sources_v1';
  var selectedRepo=0;
  var addVisible=false;

  function defaultSources(){
    return [
      {name:'非凡采集',url:'http://cj.ffzyapi.com/api.php/provide/vod',builtin:true},
      {name:'暴风采集',url:'https://bfzyapi.com/api.php/provide/vod/',builtin:true},
      {name:'虎牙采集',url:'https://www.huyaapi.com/api.php/provide/vod/',builtin:true},
      {name:'金鹰采集',url:'https://jyzyapi.com/provide/vod/',builtin:true},
      {name:'新浪影视',url:'http://api.xinlangapi.com/xinlangapi.php/provide/vod/',builtin:true},
      {name:'红牛影视',url:'https://www.hongniuzy2.com/api.php/provide/vod/',builtin:true},
      {name:'闪电影视',url:'http://sdzyapi.com/api.php/provide/vod/',builtin:true},
      {name:'光速影视',url:'https://api.guangsuapi.com/api.php/provide/vod/',builtin:true},
      {name:'茶杯狐',url:'https://hhzyapi.com/api.php/provide/vod',builtin:true}
    ];
  }
  function loadSources(){
    try{
      var raw=localStorage.getItem(STORE_KEY);
      if(raw){
        var arr=JSON.parse(raw);
        if(Array.isArray(arr)&&arr.length>0){
          var seen={};
          arr.forEach(function(x){if(x&&x.url)seen[String(x.url).replace(/\/$/,'')]=1});
          defaultSources().forEach(function(x){
            var key=String(x.url).replace(/\/$/,'');
            if(!seen[key])arr.push(x);
          });
          return arr;
        }
      }
    }catch(e){}
    return defaultSources();
  }
  function saveSources(arr){
    try{localStorage.setItem(STORE_KEY,JSON.stringify(arr||[]))}catch(e){}
  }
  function setApiName(name){
    window.NC_CURRENT_API_NAME=name||'非凡采集';
    var el=document.getElementById('tvSourceName');
    if(el)el.textContent=window.NC_CURRENT_API_NAME;
  }
  function setApiUrl(url){window.NC_CURRENT_API_URL=url||''}
  function apiRepos(){
    return [
      {name:'宝儿接口',sources:loadSources()},
      {name:'本地缓存',sources:[{name:'非凡缓存',url:'seed://ffzy'}]}
    ];
  }

  window.renderRepoPanel=function(){
    var sidebar=document.getElementById('repoSidebar'),content=document.getElementById('repoContent');
    if(!sidebar||!content)return;
    var repos=apiRepos();
    if(selectedRepo>=repos.length)selectedRepo=0;
    sidebar.innerHTML=repos.map(function(r,i){
      return '<button class="'+(i===selectedRepo?'active':'')+'" onclick="selectRepo('+i+')">'+r.name+'</button>';
    }).join('');
    var repo=repos[selectedRepo],sources=repo.sources||[];
    var form=addVisible?[
      '<div class="nc-add-form">',
      '<input id="ncApiNameInput" placeholder="API名称，例如：非凡采集">',
      '<input id="ncApiUrlInput" placeholder="API链接，例如：http://.../provide/vod">',
      '<div class="nc-add-actions">',
      '<button onclick="confirmAddRepoSource()">保存接口</button>',
      '<button onclick="cancelAddRepoSource()">取消</button>',
      '</div></div>'
    ].join(''):'';
    if(!sources.length){
      content.innerHTML=form+'<div class="nc-repo-empty">暂无接口，点击右上角“添加”创建。</div>';
      return;
    }
    content.innerHTML=form+sources.map(function(s,i){
      var active=(window.NC_CURRENT_API_NAME||'非凡采集')===s.name;
      return '<div class="nc-source-row">'+
        '<button class="nc-source-main '+(active?'active':'')+'" onclick="selectRepoSource('+i+')">'+
        '<b>'+s.name+'</b><span class="nc-source-url">'+s.url+'</span></button>'+
        (selectedRepo===0?'<button class="nc-source-del" onclick="deleteRepoSource('+i+',event)">删</button>':'')+
        '</div>';
    }).join('');
  };
  window.NCRepoSources=function(){return loadSources()};
  window.NCSelectRepoSourceByUrl=function(url){
    var list=loadSources(),target=String(url||'').replace(/\/$/,'');
    for(var i=0;i<list.length;i++){
      if(String(list[i].url||'').replace(/\/$/,'')===target){
        selectedRepo=0;
        return window.selectRepoSource(i);
      }
    }
  };

  window.selectRepo=function(idx){
    selectedRepo=idx;
    addVisible=false;
    renderRepoPanel();
  };

  window.addRepo=function(){
    selectedRepo=0;
    addVisible=true;
    renderRepoPanel();
    setTimeout(function(){
      var n=document.getElementById('ncApiNameInput');
      if(n)n.focus();
    },50);
  };

  window.cancelAddRepoSource=function(){
    addVisible=false;
    renderRepoPanel();
  };

  window.confirmAddRepoSource=function(){
    var name=(document.getElementById('ncApiNameInput')||{}).value||'';
    var url=(document.getElementById('ncApiUrlInput')||{}).value||'';
    name=name.trim();url=url.trim();
    if(!name||!url){alert('请填写API名称和链接');return}
    var list=loadSources().filter(function(x){return x.url!==url});
    list.push({name:name,url:url});
    saveSources(list);
    addVisible=false;
    renderRepoPanel();
  };

  window.deleteRepoSource=function(idx,e){
    if(e)e.stopPropagation();
    var list=loadSources();
    if(!list[idx])return;
    if(!confirm('删除接口：'+list[idx].name+'？'))return;
    list.splice(idx,1);
    saveSources(list);
    renderRepoPanel();
  };

  window.selectRepoSource=function(idx){
    var repos=apiRepos(),repo=repos[selectedRepo],src=repo&&repo.sources&&repo.sources[idx];
    if(!src)return;
    hideRepoPanel();
    setApiName(src.name);
    setApiUrl(src.url);
    if(src.url==='seed://ffzy'){
      if(window.FFZY_SEED){
        FFZY_CLASSES=window.FFZY_SEED.class||[];
        MOVIE_CATS=['推荐'].concat(FFZY_CLASSES.filter(function(c){return String(c.type_pid)==='0'}).map(function(c){return window.normalizeCatName?normalizeCatName(c.type_name):c.type_name}));
        var items=(window.FFZY_SEED.list||[]).map(function(v){return normalizeVod(v,'推荐')});
        MOVIE_DATA=items;
        movieState.usingRemote=true;movieState.loaded=true;movieState.cat='推荐';
        renderMovieHome();updateLoadMoreBtn();
        if(window.NCDB){
          var base='seed://ffzy';
          NCDB.saveSource('非凡缓存',base,base).then(function(srcId){
            NCDB.saveCategories(srcId,FFZY_CLASSES);
            NCDB.saveMovies(srcId,'推荐',items);
            if(window.updateDbRenderCats)updateDbRenderCats();
          });
        }
      }
      return;
    }
    var inp=document.getElementById('tvConfigUrl')||document.getElementById('movieConfigUrl');
    if(inp)inp.value=src.url;
    // 不再自动采集，只记录源选择，用户需到我的页面手动点击采集
    setMovieStatus('已选择源：'+src.name+'，请到「我的」页面开始采集',true);
  };

  var oldFfzyInit=window.ffzyInit;
  if(oldFfzyInit){
    window.ffzyInit=function(){
      setApiName(window.NC_CURRENT_API_NAME||'非凡采集');
      return oldFfzyInit.apply(this,arguments);
    };
  }

  setApiName(window.NC_CURRENT_API_NAME||'非凡采集');
})();
