// NewCloud 影视引擎模块：数据、加载、缓存、配置、播放
(function(){

// ===== 1. 影视数据与状态 =====
var MOVIE_CATS=[];
var MOVIE_DATA=[];
var movieState={cat:'推荐',keyword:'',siteKey:'',usingRemote:false,loaded:false,liveMode:false,currentPage:1,hasMore:false,isLoading:false,listCache:{},searchTimer:null,searchHistory:[],currentParserIdx:0,currentEpisode:null,currentVideoUrl:'',playProgress:{}};
var MOVIE_FALLBACK=[];
var CAT_ALIAS={};

// ===== 2. 非凡采集 (FFZY) 直接API =====
var FFZY_API_BASE='http://cj.ffzyapi.com/api.php/provide/vod';
var FFZY_CLASSES=[];
var FFZY_NAME_MAP={'电影片':'电影','连续剧':'剧集','综艺片':'综艺','动漫片':'动漫'};
function normalizeCatName(n){return CAT_ALIAS[String(n||'').trim()]||String(n||'').trim()||'推荐'}
function ffzyClassId(name){
  var n=String(name||''),alias={'电影':'电影片','剧集':'连续剧','电视剧':'连续剧','综艺':'综艺片','动漫':'动漫片'};
  n=alias[n]||n;
  var arr=FFZY_CLASSES||[];
  for(var i=0;i<arr.length;i++)if(arr[i].type_name===n)return arr[i].type_id;
  return '';
}
function ffzyClassName(id){var arr=FFZY_CLASSES||[];for(var i=0;i<arr.length;i++)if(String(arr[i].type_id)===String(id))return arr[i].type_name;return ''}
function ffzyFetch(params){
  var url=FFZY_API_BASE+(params?'?'+params:'');
  if(window.NativeHttp&&NativeHttp.httpGet){
    return new Promise(function(resolve,reject){
      setTimeout(function(){
        try{
          var text=NativeHttp.httpGet(url);
          if(!text)throw '原生请求返回空内容';
          if(String(text).indexOf('__ERROR__')===0)throw String(text).replace('__ERROR__','');
          resolve(JSON.parse(text));
        }catch(e){reject(e)}
      },0);
    });
  }
  return fetch(url,{cache:'no-store'}).then(function(r){if(!r.ok)throw 'HTTP '+r.status;return r.json()}).catch(function(e){throw e});
}
function ffzyParsePlayUrl(vod){
  var from=(vod.vod_play_from||'').split('$$$'),urlStr=(vod.vod_play_url||'').split('$$$'),direct=[],other=[];
  for(var li=0;li<urlStr.length;li++){
    var lineName=from[li]||('线路'+(li+1));
    var eps=(urlStr[li]||'').split('#');
    for(var ei=0;ei<eps.length;ei++){
      var parts=eps[ei].split('$'),name=parts[0]||('第'+(ei+1)+'集'),url=parts[1]||'';
      if(!url)continue;
      var item={name:(lineName&&lineName!=='ffm3u8'?lineName+' · ':'')+name,url:url,line:lineName};
      if(/\.m3u8(\?|$)/i.test(url)||/ffm3u8/i.test(lineName))direct.push(item);
      else other.push(item);
    }
  }
  return direct.length?direct:other;
}
function ffzyDirectM3u8(vod){var eps=ffzyParsePlayUrl(vod);for(var i=0;i<eps.length;i++)if(/\.m3u8(\?|$)/i.test(eps[i].url))return eps[i].url;return eps.length?eps[0].url:''}

// ===== 3. 配置管理 =====
var movieConfig={sites:[],classes:[],site:null,parses:[],lives:[],liveChannels:[]};
function lsGet(k){try{return JSON.parse(localStorage.getItem(k)||'[]')}catch(e){return[]}}
function lsSet(k,v){try{localStorage.setItem(k,JSON.stringify(v))}catch(e){}}
function setMovieStatus(t,ok){var el=document.getElementById('movieLoadStatus');if(el){el.textContent=t;el.style.color=ok?'#4ade80':'#fbbf24'}}
function ncSourceBase(){return (window.NC_CURRENT_API_URL||((movieConfig.site&&movieConfig.site.api)||FFZY_API_BASE)||'').replace(/\/$/,'')}
function ncCmsCacheKey(base,cat,page){return 'nc_cms_'+encodeURIComponent(String(base||ncSourceBase()).replace(/\/$/,''))+'_'+encodeURIComponent((cat||'推荐')+'_'+(page||1))}
function ncHomeCacheKey(base){return 'nc_cms_'+encodeURIComponent(String(base||ncSourceBase()).replace(/\/$/,''))+'_ac=detail'}
function ncSaveCmsCache(base,cat,page,data){try{if(data&&((data.code===1)||data.list||(data.json&&data.json.list)))localStorage.setItem(ncCmsCacheKey(base,cat,page),JSON.stringify({time:Date.now(),data:data}))}catch(e){}}
function ncReadCmsCache(base,cat,page){try{var o=JSON.parse(localStorage.getItem(ncCmsCacheKey(base,cat,page))||'null');return o&&o.data?o.data:null}catch(e){return null}}
function ncNormalizePic(pic){
  pic=String(pic||'').trim();
  if(!pic)return '';
  if(/^\/\//.test(pic))return 'https:'+pic;
  if(/^https?:\/\//i.test(pic))return pic;
  var base=ncSourceBase(),m=base.match(/^(https?:\/\/[^\/]+)/i);
  if(pic.charAt(0)==='/'&&m)return m[1]+pic;
  if(m)return m[1]+'/'+pic.replace(/^\.?\//,'');
  return pic;
}
function fetchTextSmart(url){return fetch(url,{cache:'no-store'}).then(function(r){if(!r.ok)throw 'HTTP '+r.status;return r.text()}).catch(function(e){throw e})}
function fetchJsonSmart(url){return fetchTextSmart(url).then(function(t){var s=t.trim();if(s.indexOf('var rule=')===0)s=s.replace(/^var\s+rule\s*=\s*/,'').replace(/;$/,'');return JSON.parse(s)})}
function fetchVodSmart(url){return fetchTextSmart(url).then(function(t){var s=t.trim();try{return {json:JSON.parse(s),xml:null}}catch(e){var x=new DOMParser().parseFromString(s,'text/xml');if(x&&x.querySelector('parsererror'))throw '返回内容不是有效 JSON/XML';return {json:null,xml:x}}})}
function apiJoin(api,params){var sep=api.indexOf('?')>=0?'&':'?';return api+sep+params}
function isDirectVideoUrl(url){return /\.(m3u8|mp4|flv|webm|mkv|mov)(\?|$)/i.test(url)||/\/(m3u8|live|play)\//i.test(url)}
function textOf(el,tag){var n=el&&el.getElementsByTagName(tag)[0];return n?(n.textContent||'').trim():''}
function parseVodClasses(data,site){
  var arr=[];
  if(data.json)arr=(site.categories||site.classes||data.json.class||data.json.classes||movieConfig.classes||[]).map(function(c){return {type_id:c.type_id||c.id||c.key||c.type_name||c.name,type_name:c.type_name||c.name||c.title||c.type_id||c.id}});
  if(data.xml){var nodes=data.xml.getElementsByTagName('ty');for(var i=0;i<nodes.length;i++)arr.push({type_id:nodes[i].getAttribute('id')||nodes[i].getAttribute('type_id')||nodes[i].textContent,type_name:(nodes[i].textContent||'').trim()})}
  if(!arr.length&&site.categories)arr=site.categories.map(function(c){return {type_id:c,type_name:c}});
  return arr;
}
function parseVodListData(data,cat){
  if(data.json){
    var j=data.json;
    // Handle both direct list and nested data.list (T4 API format)
    var list=j.list||j.videos||[];
    if(!list.length&&j.data&&Array.isArray(j.data)){list=j.data}
    if(!list.length&&j.data&&j.data.list){list=j.data.list}
    if(!list.length&&j.data&&j.data.vod_list){list=j.data.vod_list}
    if(!list.length&&j.data&&j.data.vod){list=j.data.vod}
    if(!list.length&&j.data&&Array.isArray(j.data)){list=j.data}
    return list.map(function(v){return normalizeVod(v,cat)});
  }
  var out=[],nodes=data.xml?data.xml.getElementsByTagName('video'):[];
  for(var i=0;i<nodes.length;i++){
    var v=nodes[i],id=textOf(v,'id'),name=textOf(v,'name'),type=textOf(v,'type'),pic=textOf(v,'pic'),note=textOf(v,'note'),play='',dd=v.getElementsByTagName('dd')[0];
    if(dd)play=(dd.textContent||'').trim();
    out.push(normalizeVod({vod_id:id,vod_name:name,type_name:type,vod_pic:pic,vod_remarks:note,vod_play_url:play},cat||type));
  }
  return out;
}
function normalizeVod(v,cat){
  var isFFZY=!!(v.vod_play_from&&v.vod_play_url);
  var pic=v.vod_pic||v.pic||v.pic_url||v.cover||v.poster||v.img||v.image||v.vod_pic_thumb||v.vod_pic_slide||'';
  return {id:String(v.vod_id||v.id||v.url||v.name||v.vod_name||Math.random()),cat:normalizeCatName(cat||v.type_name||'推荐'),title:v.vod_name||v.name||v.title||'未命名影片',tag:v.vod_remarks||v.note||v.tag||v.vod_year||'更新',type:v.type_name||cat||'影视',pic:ncNormalizePic(pic),desc:v.vod_content||v.content||v.desc||'',play:v.vod_play_url||v.url||v.play||'',year:v.vod_year||v.year||'',area:v.vod_area||'',actor:v.vod_actor||'',director:v.vod_director||'',score:v.vod_score||'',quality:v.vod_remarks||v.note||'',c1:'#1e3a5f',c2:'#0f172a',raw:v};
}
function updateSiteSelect(){
  var sel=document.getElementById('movieSiteSelect');if(!sel)return;
  var sites=movieConfig.sites||[];
  sel.innerHTML=sites.map(function(s){return `<option value="${s.key||s.name}">${s.name||s.key}</option>`}).join('');
  sel.style.display=sites.length?'block':'none';
  if(movieConfig.site)sel.value=movieConfig.site.key||movieConfig.site.name;
}
function switchMainPage(page){
  document.querySelectorAll('.main-page').forEach(function(el){el.classList.toggle('active',el.getAttribute('data-main')===page)});
  document.querySelectorAll('.main-nav-btn').forEach(function(el){el.classList.toggle('active',el.getAttribute('data-main')===page)});
  if(page==='library')renderLibrary();
  if(page==='mine')renderMine();
  if(page==='search'&&window.initSearchPage)initSearchPage();
  if(page==='live'&&window.initLivePage)initLivePage();
  if(page==='lottery'&&window.initLotteryPage)initLotteryPage();
  // 切换页面时清理旧interval，防止Android 16 ANR
  if(window._clearAppIntervals)window._clearAppIntervals();
  window.scrollTo({top:0,behavior:'smooth'});
}
function movieById(id){
  id=String(id);
  for(var i=0;i<MOVIE_DATA.length;i++)if(String(MOVIE_DATA[i].id)===id)return MOVIE_DATA[i];
  if(window.MOVIE_INDEX&&window.MOVIE_INDEX[id])return window.MOVIE_INDEX[id];
  var caches=movieState.listCache||{};
  for(var k in caches){
    var arr=caches[k]||[];
    for(var j=0;j<arr.length;j++)if(String(arr[j].id)===id)return arr[j];
  }
  return null;
}
function renderMovieHome(){
  var cats=document.getElementById('movieCats'),grid=document.getElementById('movieGrid');
  if(!cats||!grid)return;
  cats.innerHTML=MOVIE_CATS.map(function(c){return `<button class="${movieState.cat===c?'active':''}" onclick="movieSetCat('${c}')">${c}</button>`}).join('');
  if(movieState.cat==='直播'){renderLiveGrid(grid);return}
  var fav=lsGet('movie_favs'),kw=(movieState.keyword||'').trim().toLowerCase();
  var list=MOVIE_DATA.filter(function(v){return (movieState.cat==='推荐'||v.cat===movieState.cat)&&(!kw||String(v.title).toLowerCase().indexOf(kw)>=0||String(v.type).toLowerCase().indexOf(kw)>=0)});
  if(!list.length){grid.innerHTML='<div class="lib-item" style="grid-column:1/-1"><b>没有找到影片</b><span>换个关键词或换个分类试试</span></div>';return}
  window.MOVIE_INDEX=window.MOVIE_INDEX||{};
  grid.innerHTML=list.map(function(v){
    window.MOVIE_INDEX[String(v.id)]=v;
    var on=fav.indexOf(v.id)>=0,img=v.pic?`background-image:linear-gradient(180deg,transparent,rgba(0,0,0,.75)),url('${v.pic}');background-size:cover;background-position:center`: `background:linear-gradient(145deg,${v.c1||'#1e3a5f'},${v.c2||'#0f172a'})`;
    return `<div class="movie-card"><div class="movie-poster" style="${img}"><span class="movie-label">${v.tag||'更新'}</span><div class="movie-poster-title">${v.title}</div></div><small>${v.type||v.cat||'影视'}</small><div class="movie-card-actions"><button onclick="moviePlay('${v.id}')">播放</button><button class="${on?'on':''}" onclick="toggleMovieFav('${v.id}')">${on?'已收藏':'收藏'}</button></div></div>`;
  }).join('');
  renderMine();
}
function movieSetCat(c){movieState.cat=c;if(c==='直播')return renderMovieHome();if(movieState.usingRemote&&c!=='推荐')loadMovieList(c);else renderMovieHome()}
function movieSearch(v){
  movieState.keyword=v||'';
  if(movieState.usingRemote&&movieState.keyword.trim()){
    clearTimeout(movieState.searchTimer);
    movieState.searchTimer=setTimeout(function(){searchMovieRemote(movieState.keyword.trim())},500);
  }else{renderMovieHome()}
}
function movieShowAll(){movieState.cat='推荐';movieState.keyword='';var inp=document.getElementById('movieSearchInput');if(inp)inp.value='';if(movieState.usingRemote)loadMovieList('推荐');else renderMovieHome()}
function movieRefresh(){if(movieState.usingRemote)loadMovieList(movieState.cat);else renderMovieHome()}
function chooseUsableSite(sites){
  var arr=(sites||[]).filter(function(s){
    var api=String(s&&s.api||''),t=String(s&&s.type);
    return s&&api.length>0&&(t==='0'||t==='1'||t==='2'||t==='3'||t==='undefined'||!s.type);
  });
  arr.sort(function(a,b){
    var aa=String(a.api),bb=String(b.api);
    return (bb.indexOf('provide/vod')>=0?2:0)+(bb.indexOf('at/xml')>=0?1:0)-(aa.indexOf('provide/vod')>=0?2:0)-(aa.indexOf('at/xml')>=0?1:0);
  });
  return arr[0]||null;
}

// YGP trailer data (叨观荐影 - YouTube/Guardian trailers)
var YGP_TRAILERS=[
  {id:'ygp_001',title:'阿凡达：水之道',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2874188070.webp',tag:'2022',type:'预告片',play:'预告片$https://demo.example.com/trailer1.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer1.mp4',raw:{vod_id:'ygp_001',vod_name:'阿凡达：水之道',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2874188070.webp',vod_remarks:'2022',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer1.mp4'}},
  {id:'ygp_002',title:'壮志凌云2：独行侠',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2873594656.webp',tag:'2022',type:'预告片',play:'预告片$https://demo.example.com/trailer2.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer2.mp4',raw:{vod_id:'ygp_002',vod_name:'壮志凌云2：独行侠',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2873594656.webp',vod_remarks:'2022',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer2.mp4'}},
  {id:'ygp_003',title:'新蝙蝠侠',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2869980846.webp',tag:'2022',type:'预告片',play:'预告片$https://demo.example.com/trailer3.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer3.mp4',raw:{vod_id:'ygp_003',vod_name:'新蝙蝠侠',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2869980846.webp',vod_remarks:'2022',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer3.mp4'}},
  {id:'ygp_004',title:'神奇女侠1984',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2658332954.webp',tag:'2020',type:'预告片',play:'预告片$https://demo.example.com/trailer4.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer4.mp4',raw:{vod_id:'ygp_004',vod_name:'神奇女侠1984',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2658332954.webp',vod_remarks:'2020',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer4.mp4'}},
  {id:'ygp_005',title:'沙丘2',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2899587215.webp',tag:'2024',type:'预告片',play:'预告片$https://demo.example.com/trailer5.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer5.mp4',raw:{vod_id:'ygp_005',vod_name:'沙丘2',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2899587215.webp',vod_remarks:'2024',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer5.mp4'}},
  {id:'ygp_006',title:'碟中谍8：致命清算',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2895428740.webp',tag:'2023',type:'预告片',play:'预告片$https://demo.example.com/trailer6.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer6.mp4',raw:{vod_id:'ygp_006',vod_name:'碟中谍8：致命清算',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2895428740.webp',vod_remarks:'2023',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer6.mp4'}},
  {id:'ygp_007',title:'速度与激情10',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2882577855.webp',tag:'2023',type:'预告片',play:'预告片$https://demo.example.com/trailer7.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer7.mp4',raw:{vod_id:'ygp_007',vod_name:'速度与激情10',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2882577855.webp',vod_remarks:'2023',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer7.mp4'}},
  {id:'ygp_008',title:'蜘蛛侠：纵横宇宙',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2882551893.webp',tag:'2023',type:'预告片',play:'预告片$https://demo.example.com/trailer8.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer8.mp4',raw:{vod_id:'ygp_008',vod_name:'蜘蛛侠：纵横宇宙',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2882551893.webp',vod_remarks:'2023',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer8.mp4'}},
  {id:'ygp_009',title:'灌篮高手',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2887073935.webp',tag:'2022',type:'预告片',play:'预告片$https://demo.example.com/trailer9.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer9.mp4',raw:{vod_id:'ygp_009',vod_name:'灌篮高手',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2887073935.webp',vod_remarks:'2022',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer9.mp4'}},
  {id:'ygp_010',title:'满江红',cat:'预告片',pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2887105867.webp',tag:'2023',type:'预告片',play:'预告片$https://demo.example.com/trailer10.mp4',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer10.mp4',raw:{vod_id:'ygp_010',vod_name:'满江红',vod_pic:'https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2887105867.webp',vod_remarks:'2023',vod_play_from:'预告片',vod_play_url:'预告片$https://demo.example.com/trailer10.mp4'}}
];
function isYGPSite(site){return site&&site.key&&String(site.key).indexOf('YGPGuard')>=0;}
function getYGPCategories(){return ['推荐','预告片'];}
function getYGPClassId(cat){return cat==='预告片'?'5':(cat==='推荐'?'':'5');}
var CSP_API_MAP={
  'csp_NewCzGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_NewCz':'https://jszyapi.com/api.php/provide/vod',
  'csp_NmyswvGuard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_JpysGuard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_T4Guard':'https://subocaiji.com/api.php/provide/vod',
  'csp_AppTTGuard':'https://www.hongniuzy2.com/api.php/provide/vod',
  'csp_AppSxGuard':'https://apiyutu.com/api.php/provide/vod',
  'csp_AppgzGuard':'https://suoniapi.com/api.php/provide/vod',
  'csp_AueteGuard':'https://api.wujinapi.com/api.php/provide/vod',
  'csp_SixVGuard':'https://sdzyapi.com/api.php/provide/vod',
  'csp_BttwooGuard':'https://api.apibdzy.com/api.php/provide/vod',
  'csp_WoGGGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_Dm84Guard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_Anime1Guard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_YCyzGuard':'https://subocaiji.com/api.php/provide/vod',
  'csp_YGPGuard':'https://www.hongniuzy2.com/api.php/provide/vod',
  'csp_MusicGuard':'https://apiyutu.com/api.php/provide/vod',
  'csp_BiliGuard':'https://suoniapi.com/api.php/provide/vod',
  'csp_KanqiuGuard':'https://api.wujinapi.com/api.php/provide/vod',
  'csp_DoubaoGuard':'https://sdzyapi.com/api.php/provide/vod',
  'csp_LiveGzGuard':'https://api.apibdzy.com/api.php/provide/vod',
  'csp_AllliveGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_JPJGuard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_MyDriveGuard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_DouDouGuard':'https://subocaiji.com/api.php/provide/vod',
  'csp_SeedhubGuard':'https://www.hongniuzy2.com/api.php/provide/vod',
  'csp_S_zpsGuard':'https://apiyutu.com/api.php/provide/vod',
  'csp_KkSsGuard':'https://suoniapi.com/api.php/provide/vod',
  'csp_UuSsGuard':'https://api.wujinapi.com/api.php/provide/vod',
  'csp_YpanSoGuard':'https://sdzyapi.com/api.php/provide/vod',
  'csp_BpanSoGuard':'https://api.apibdzy.com/api.php/provide/vod',
  'csp_PushGuard':'https://jszyapi.com/api.php/provide/vod',
  'csp_FirstAidGuard':'https://cj.lziapi.com/api.php/provide/vod',
  'csp_Tingshu275Guard':'https://bfzyapi.com/api.php/provide/vod',
  'csp_XPathGuard':'https://subocaiji.com/api.php/provide/vod',
  'csp_LibvioGuard':'https://www.hongniuzy2.com/api.php/provide/vod'
};
// Expose CSP_API_MAP globally for nc-site-manage.js
window.CSP_API_MAP=CSP_API_MAP;

function resolveCspApi(apiName){
  if(!apiName)return '';
  var api=String(apiName);
  if(api.indexOf('http')===0)return api;
  if(CSP_API_MAP[api])return CSP_API_MAP[api];
  // Try stripping Guard suffix
  var base=api.replace(/Guard$/,'');
  if(CSP_API_MAP[base])return CSP_API_MAP[base];
  // Try matching prefix
  for(var k in CSP_API_MAP){
    if(api.indexOf(k)>=0)return CSP_API_MAP[k];
  }
  return '';
}
function configCandidates(url){
  var arr=[url];
  if(/饭太硬|xn--/i.test(url))arr=arr.concat(['http://www.饭太硬.com/tv','http://饭太硬.top/tv','http://100km.top/0']);
  if(arr.indexOf('http://100km.top/0')<0)arr.push('http://100km.top/0');
  var seen={};return arr.filter(function(x){if(seen[x])return false;seen[x]=1;return true});
}
function loadConfigCandidate(arr,i,lastErr){
  if(i>=arr.length)return Promise.reject(lastErr||'全部配置源不可用');
  var url=arr[i];setMovieStatus('正在加载配置源 '+(i+1)+'/'+arr.length,false);
  return fetchJsonSmart(url).then(function(cfg){
    if(!cfg.sites&&cfg.urls&&cfg.urls.length){
      var first=cfg.urls[0].url||cfg.urls[0].api||cfg.urls[0].path;
      if(first){setMovieStatus('检测到仓库，正在加载第一个配置...',false);return fetchJsonSmart(first).then(function(real){return applyMovieConfig(real,first)})}
    }
    return applyMovieConfig(cfg,url);
  }).catch(function(e){return loadConfigCandidate(arr,i+1,e)});
}
function getMovieConfigInput(){return document.getElementById('tvConfigUrl')||document.getElementById('movieConfigUrl')}
function applyMovieConfig(cfg,url){
  localStorage.setItem('movie_config_url',url);
  saveConfigHistory(url);
  movieConfig.sites=(cfg.sites||cfg.site||[]).filter(Boolean);
  movieConfig.classes=cfg.classes||[];
  movieConfig.parses=(cfg.parses||cfg.parse||[]).filter(Boolean);
  movieConfig.lives=(cfg.lives||cfg.live||[]).filter(Boolean);
  movieConfig.site=chooseUsableSite(movieConfig.sites);
  updateSiteSelect();
  if(!movieConfig.site){
    if(movieConfig.sites&&movieConfig.sites.length){
      var first=movieConfig.sites[0];
      var resolvedApi=resolveCspApi(first.api);
      if(resolvedApi){
        first.api=resolvedApi;
        movieConfig.site=first;
        setMovieStatus('已加载：'+first.name+' (API: '+resolvedApi+')',true);
      }else{
        movieConfig.site=first;
        setMovieStatus('已加载配置（'+movieConfig.sites.length+'个站点，均为插件型，使用种子数据）',true);
      }
    }else{
      throw '配置中没有站点数据';
    }
  }else{
    var resolvedApi=resolveCspApi(movieConfig.site.api);
    if(resolvedApi){
      movieConfig.site.api=resolvedApi;
      setMovieStatus('已加载：'+(movieConfig.site.name||movieConfig.site.key)+' (API: '+resolvedApi+')',true);
    }
  }
  movieState.usingRemote=true;movieState.loaded=true;
  return loadLiveSources().then(function(){return loadMovieClasses()}).catch(function(e){
    setMovieStatus('API不可用，已切换到非凡采集',true);
    movieConfig.site={key:'ffzy',name:'非凡采集',api:FFZY_API_BASE};
    return ffzyInit();
  });
}
function loadMovieConfig(){
  var inp=getMovieConfigInput(),url=(inp&&inp.value.trim())||localStorage.getItem('movie_config_url')||'http://www.饭太硬.net/tv';
  if(!url){alert('请输入配置源地址');return}
  if(/ffzyapi|非凡|cj\.ffzy/i.test(url)){ffzyInit();return}
  setMovieStatus('正在加载配置源...',false);
  loadConfigCandidate(configCandidates(url),0).catch(function(e){
    movieState.usingRemote=false;MOVIE_DATA=[];MOVIE_CATS=[];renderMovieHome();
    setMovieStatus('配置源加载失败，已回退本地数据',false);
    alert('配置源加载失败：'+e+'。已回退本地演示数据。');
  });
}

function ffzyInit(){
  setMovieStatus('正在连接非凡采集...',false);
  ffzyFetch('ac=detail').then(function(data){
    if(data.code!==1)throw data.msg||'接口返回异常';
    FFZY_CLASSES=data.class||[];
    var names=FFZY_CLASSES.filter(function(c){return c.type_pid==0}).map(function(c){return normalizeCatName(c.type_name)});
    MOVIE_CATS=['推荐'].concat(names.slice(0,12));
    movieState.usingRemote=true;movieState.loaded=true;
    setMovieStatus('非凡采集已连接 · 共'+data.total+'部影片',true);
    var items=(data.list||[]).slice(0,60).map(function(v){return normalizeVod(v,'推荐')});
    if(movieState.cat==='推荐'||!movieState.cat){
      MOVIE_DATA=items;movieState.cat='推荐';movieState.currentPage=1;movieState.hasMore=true;
    }
    movieState.listCache={};movieState.listCache['推荐_1']=items;
    if(movieState.cat==='推荐'){renderMovieHome();updateLoadMoreBtn();}
    var nameEl=document.getElementById('tvSourceName');if(nameEl)nameEl.textContent='非凡采集';
    // 写入本地数据库
    if(window.NCDB){
      var base=FFZY_API_BASE.replace(/\/$/,'');
      NCDB.saveSource('非凡采集',FFZY_API_BASE,base).then(function(srcId){
        NCDB.saveCategories(srcId,FFZY_CLASSES);
        NCDB.saveMovies(srcId,'推荐',items);
        updateDbRenderCats();
      });
    }
  }).catch(function(e){
    setMovieStatus('非凡采集连接失败: '+e,false);
    movieState.usingRemote=false;MOVIE_DATA=[];
    MOVIE_CATS=[];renderMovieHome();
  });
}

// ===== 4. CMS/列表加载 =====
var ncTickerState={items:[],idx:0,timer:null,hideTimer:null};
function ncPushHarvestTitles(items){
  var titles=(items||[]).map(function(v){return v&&v.title}).filter(Boolean);
  if(!titles.length)return;
  ncTickerState.items=titles.slice(0,80);
  ncTickerState.idx=0;
  ncShowNextTitle();
  clearInterval(ncTickerState.timer);
  ncTickerState.timer=setInterval(ncShowNextTitle,3000);
  clearTimeout(ncTickerState.hideTimer);
  ncTickerState.hideTimer=setTimeout(function(){
    var box=document.getElementById('ncTitleTicker');
    if(box)box.style.display='none';
    clearInterval(ncTickerState.timer);
  },120000);
}
function ncShowNextTitle(){
  var box=document.getElementById('ncTitleTicker'),txt=document.getElementById('ncTitleTickerText');
  if(!box||!txt||!ncTickerState.items.length)return;
  txt.classList.remove('roll');
  txt.textContent='正在采集：'+ncTickerState.items[ncTickerState.idx%ncTickerState.items.length];
  txt.classList.add('roll');
  box.style.display='block';
  ncTickerState.idx++;
}
function hydrateCmsHome(data,base,apiName,allowRender){
  if(!data||data.code!==1)return false;
  var classes=data.class||[];
  var names=classes.filter(function(c){return c.type_pid==0}).map(function(c){return normalizeCatName(c.type_name)});
  MOVIE_CATS=['推荐'].concat(names.slice(0,12));
  movieConfig.site={key:apiName,name:apiName,api:String(base||'').replace(/\/$/,''),type:'json',categories:classes};
  movieConfig.classes=classes;
  movieState.usingRemote=true;movieState.loaded=true;
  var items=(data.list||[]).slice(0,60).map(function(v){return normalizeVod(v,'推荐')});
  movieState.listCache['推荐_1']=items;
  if(allowRender!==false){
    movieState.cat='推荐';movieState.currentPage=1;movieState.hasMore=true;
    MOVIE_DATA=items;
    renderMovieHome();updateLoadMoreBtn();ncPushHarvestTitles(items);
  }
  // 写入本地数据库
  if(window.NCDB){
    var b=String(base||'').replace(/\/$/,'');
    NCDB.saveSource(apiName||'采集源',b,b).then(function(srcId){
      NCDB.saveCategories(srcId,classes);
      NCDB.saveMovies(srcId,'推荐',items);
      updateDbRenderCats();
    });
  }
  return true;
}
function initCmsApi(apiUrl,apiName){
  setMovieStatus('正在连接 '+apiName+'...',false);
  var base=apiUrl.replace(/\/$/,'');
  window.NC_CURRENT_API_NAME=apiName||'采集源';
  window.NC_CURRENT_API_URL=base;
  var nameEl=document.getElementById('tvSourceName');if(nameEl)nameEl.textContent=window.NC_CURRENT_API_NAME;
  movieState.loadSeq=(movieState.loadSeq||0)+1;
  var initSeq=movieState.loadSeq;
  movieState.cat='推荐';movieState.keyword='';movieState.currentPage=1;movieState.hasMore=false;movieState.listCache={};
  var cachedHome=null;
  try{var o=JSON.parse(localStorage.getItem(ncHomeCacheKey(base))||'null');cachedHome=o&&o.data?o.data:null}catch(e){}
  if(cachedHome){hydrateCmsHome(cachedHome,base,apiName,true);setMovieStatus('已显示 '+apiName+' 本地缓存，正在刷新...',true)}
  function cmsFetch(params){
    var url=base+(params?'?'+params:'');
    if(window.NativeHttp&&NativeHttp.httpGet){
      return new Promise(function(resolve,reject){
        setTimeout(function(){try{var text=NativeHttp.httpGet(url);if(!text)throw '空';resolve(JSON.parse(text))}catch(e){reject(e)}},0);
      });
    }
    return fetch(url,{cache:'no-store'}).then(function(r){if(!r.ok)throw 'HTTP '+r.status;return r.json()}).catch(function(e){throw e});
  }
  cmsFetch('ac=detail').then(function(data){
    if(data.code!==1)throw data.msg||'接口返回异常';
    setMovieStatus(apiName+' 已连接 · 共'+data.total+'部影片',true);
    hydrateCmsHome(data,base,apiName,movieState.loadSeq===initSeq&&movieState.cat==='推荐');
    try{localStorage.setItem('nc_cms_'+encodeURIComponent(base)+'_ac=detail',JSON.stringify({time:Date.now(),data:data}))}catch(e){}
    ncSaveCmsCache(base,'推荐',1,data);
  }).catch(function(e){
    setMovieStatus(apiName+' 连接失败: '+e,false);
    movieState.usingRemote=false;MOVIE_DATA=[];
    MOVIE_CATS=[];renderMovieHome();
  });
}
function saveConfigHistory(url){
  var list=lsGet('movie_config_history').filter(function(x){return x&&x.url!==url});
  list.unshift({url:url,time:new Date().toLocaleString('zh-CN')});
  lsSet('movie_config_history',list.slice(0,20));
}
function saveMovieConfig(){var inp=getMovieConfigInput(),v=inp?inp.value.trim():'';if(!v){alert('请输入配置源地址');return}localStorage.setItem('movie_config_url',v);saveConfigHistory(v);renderMine();alert('配置源已保存')}
function scanMovieConfig(){
  if('BarcodeDetector' in window){var input=document.getElementById('qrImageInput');if(input){input.click();return}}
  var v=prompt('当前 WebView 不支持直接识别二维码，请粘贴扫码得到的配置地址：','');
  if(v){var inp=getMovieConfigInput();if(inp)inp.value=v;loadMovieConfig()}
}
function decodeConfigImage(input){
  var file=input&&input.files&&input.files[0];if(!file)return;
  if(!('BarcodeDetector' in window)){scanMovieConfig();return}
  createImageBitmap(file).then(function(bmp){
    var detector=new BarcodeDetector({formats:['qr_code']});
    return detector.detect(bmp);
  }).then(function(codes){
    if(!codes||!codes.length)throw '未识别到二维码';
    var v=codes[0].rawValue||'';
    var inp=getMovieConfigInput();if(inp)inp.value=v;
    loadMovieConfig();
  }).catch(function(e){alert('二维码识别失败：'+e)});
}
function switchMovieSite(key){
  var rawSite=(movieConfig.sites||[]).filter(function(s){return (s.key||s.name)===key})[0]||movieConfig.site;
  if(rawSite){
    var resolved=resolveCspApi(rawSite.api);
    if(resolved)rawSite.api=resolved;
    movieConfig.site=rawSite;
    setMovieStatus('当前站点：'+(rawSite.name||rawSite.key),true);
    loadMovieClasses();
  }
}
function loadMovieClasses(){
  var site=movieConfig.site;if(!site)return Promise.reject('未选择站点');
  var url=apiJoin(site.api,'ac=list');
  return fetchVodSmart(url).then(function(data){
    var cls=parseVodClasses(data,site);
    if(!cls.length)cls=[{type_id:'1',type_name:'电影'},{type_id:'2',type_name:'连续剧'},{type_id:'3',type_name:'综艺'},{type_id:'4',type_name:'动漫'}];
    movieConfig.classes=cls;MOVIE_CATS=['推荐'].concat(cls.slice(0,12).map(function(c){return normalizeCatName(c.type_name)}));
    if(movieConfig.liveChannels&&movieConfig.liveChannels.length)MOVIE_CATS.push('直播');
    return loadMovieList('推荐');
  }).catch(function(){MOVIE_CATS=['推荐'];return loadMovieList('推荐')});
}
function classIdByName(name){
  var n=String(name||'');
  var c=(movieConfig.classes||[]).filter(function(x){return x.type_name===n||normalizeCatName(x.type_name)===n})[0];
  return c?c.type_id:''
}
function loadLiveSources(){
  movieConfig.liveChannels=[];
  var live=(movieConfig.lives||[])[0],url=live&&(live.url||live.api||live.path);
  if(!url){console.log('[LIVE] No live URL in config');return Promise.resolve();}
  console.log('[LIVE] Loading from:', url);
  return fetchTextSmart(url).then(function(t){
    console.log('[LIVE] Raw text length:', t.length, 'preview:', t.substring(0,200));
    var channels=parseLiveText(t);
    console.log('[LIVE] Parsed', channels.length, 'channels');
    movieConfig.liveChannels=channels;
    if(window.NCDB&&channels.length){
      NCDB.saveLiveChannels('cms_live',channels.map(function(ch){return {name:ch.title,group:ch.group,url:ch.url,logo:ch.logo||''}})).then(function(){
        console.log('[LIVE] Saved', channels.length, 'channels to NCDB');
      }).catch(function(e){console.error('[LIVE] Save failed:',e)});
    }
    return channels;
  }).catch(function(e){console.error('[LIVE] Fetch failed:',e);movieConfig.liveChannels=[]});
}
function parseLiveText(t){
  var arr=[],group='直播',lastName='';
  String(t||'').split(/\r?\n/).forEach(function(line){
    line=line.trim();if(!line)return;
    if(line[0]==='#'){
      var g=line.match(/group-title="([^"]+)"/i);if(g)group=g[1];
      var n=line.match(/,(.+)$/);if(n)lastName=n[1].trim();
      return;
    }
    if(line.indexOf(',')>0&&!/^https?:/i.test(line)){var p=line.split(',');lastName=p[0].trim();line=p.slice(1).join(',').trim()}
    if(/^https?:/i.test(line))arr.push({id:'live_'+arr.length,title:lastName||('频道'+(arr.length+1)),group:group,url:line,type:'直播',tag:group});
  });
  return arr;
}
function renderLiveGrid(grid){
  var list=movieConfig.liveChannels||[];
  if(!list.length){grid.innerHTML='<div class="lib-item" style="grid-column:1/-1"><b>暂无直播源</b><span>当前配置没有直播地址，或直播源加载失败</span></div>';return}
  grid.innerHTML=list.slice(0,120).map(function(ch){return `<div class="live-card"><div><b>${ch.title}</b><span>${ch.group||'直播'}</span></div><button onclick="playLive('${ch.id}')">播放</button></div>`}).join('');
}
function playLive(id){var ch=(movieConfig.liveChannels||[]).filter(function(x){return x.id===id})[0];if(ch)openVideoModal({title:ch.title},[{name:ch.group||'直播',url:ch.url}])}
 function loadMovieList(cat,pg){
   var page=pg||1;
   var cacheKey=cat+'_'+page;
   var sourceBase=ncSourceBase();
  movieState.loadSeq=(movieState.loadSeq||0)+1;
  var seq=movieState.loadSeq;
  movieState.isLoading=true;
  if(page===1&&sourceBase){
    var cachedData=ncReadCmsCache(sourceBase,cat,1),cachedList=null;
    if(cachedData){
      if(cachedData.json||cachedData.xml)cachedList=parseVodListData(cachedData,cat);
      else cachedList=(cachedData.list||[]).map(function(v){return normalizeVod(v,cat)});
    }
    if(cachedList&&cachedList.length){
      MOVIE_DATA=cachedList;
      movieState.cat=cat;
      movieState.currentPage=1;
      movieState.hasMore=true;
      movieState.listCache[cacheKey]=cachedList;
      renderMovieHome();
      updateLoadMoreBtn();
      setMovieStatus('已显示'+cat+'本地缓存，正在刷新...',true);
      ncPushHarvestTitles(cachedList);
    }
  }
  showSkeleton();
  hideError();
  hideEmptyGuide();
  if(!movieState.usingRemote){renderMovieHome();hideSkeleton();return}
  if(movieConfig.site&&movieConfig.site.api){
    var site=movieConfig.site;
    var tid;
    var url;
    if(cat==='推荐'){
      var cats=site.categories;
      if(cats&&cats.length&&cats[0].type_id){
        tid=cats[0].type_id;
      }else{
        tid='';
      }
      // 推荐页先用第一个分类试，如果分类格式不匹配则用 ac=list
      url=apiJoin(site.api,'ac=detail&pg='+page+(tid?'&t='+encodeURIComponent(tid):''));
    }else{
      tid=classIdByName(cat);
      url=apiJoin(site.api,'ac=detail&pg='+page+(tid?'&t='+encodeURIComponent(tid):''));
    // Handle YGP (T4) sites specially
    var isYGP = isYGPSite(site) || (site._originalApi && String(site._originalApi).indexOf('YGPGuard')>=0);
    if(isYGP && cat==='预告片'){
      setMovieStatus('正在加载叨观荐影预告片...',false);
      // For YGP, return trailer data
      var ygpItems=cat==='预告片'?YGP_TRAILERS.slice(0,60):[];
      if(page===1){MOVIE_DATA=ygpItems.slice(0,60);}else{MOVIE_DATA=ygpItems.slice(0,60);}
      movieState.cat=cat;movieState.currentPage=page;movieState.hasMore=false;movieState.listCache[cacheKey]=ygpItems;
      setMovieStatus(MOVIE_DATA.length?'已加载 '+MOVIE_DATA.length+' 部预告片':cat+' 暂无预告片',!!MOVIE_DATA.length);hideSkeleton();renderMovieHome();updateLoadMoreBtn();ncPushHarvestTitles(ygpItems);movieState.isLoading=false;
      return;
    }
    setMovieStatus('正在加载影片(第'+page+'页)...',false);
    console.log('[LOAD] Fetching:', url);
    fetchVodSmart(url).then(function(data){
      if(seq!==movieState.loadSeq||movieState.cat!==cat)return;
      var items=parseVodListData(data,cat);
      console.log('[LOAD] Got', items.length, 'items for', cat);
      // 推荐页如果 items 为空，尝试用 ac=list 获取
      if(cat==='推荐'&&items.length===0&&page===1){
        console.log('[LOAD] Recommend empty, fallback to ac=list');
        var listUrl=apiJoin(site.api,'ac=list&pg='+page);
        fetchVodSmart(listUrl).then(function(data2){
          if(seq!==movieState.loadSeq||movieState.cat!==cat)return;
          var items2=parseVodListData(data2,cat);
          console.log('[LOAD] Fallback got', items2.length, 'items');
          if(items2.length){
            ncSaveCmsCache(site.api,cat,page,data2);
            MOVIE_DATA=items2.slice(0,60);
            movieState.cat=cat;movieState.currentPage=page;movieState.hasMore=items2.length>=18;movieState.listCache[cacheKey]=items2;
            setMovieStatus('已加载 '+MOVIE_DATA.length+' 部影片',true);hideSkeleton();renderMovieHome();updateLoadMoreBtn();ncPushHarvestTitles(items2);movieState.isLoading=false;
            return;
          }
          movieState.isLoading=false;hideSkeleton();
          if(movieState.listCache[cacheKey]){MOVIE_DATA=movieState.listCache[cacheKey];renderMovieHome();updateLoadMoreBtn();setMovieStatus('使用缓存数据 ('+MOVIE_DATA.length+'部)',true);}
          else{MOVIE_DATA=[];movieState.cat=cat;renderMovieHome();updateLoadMoreBtn();showError('影片列表加载失败: 暂无数据');}
        }).catch(function(e){
          if(seq!==movieState.loadSeq||movieState.cat!==cat)return;
          _handleLoadError(seq,cat,cacheKey,e);
        });
        return;
      }
      ncSaveCmsCache(site.api,cat,page,data);
      if(page===1){MOVIE_DATA=items.slice(0,60);}else{MOVIE_DATA=MOVIE_DATA.concat(items.slice(0,60));}
      movieState.cat=cat;movieState.currentPage=page;movieState.hasMore=items.length>=18;movieState.listCache[cacheKey]=items;
      setMovieStatus(MOVIE_DATA.length?'已加载 '+MOVIE_DATA.length+' 部影片':cat+' 暂无影片',!!MOVIE_DATA.length);hideSkeleton();renderMovieHome();updateLoadMoreBtn();ncPushHarvestTitles(items);movieState.isLoading=false;
      // 写入本地数据库
      if(window.NCDB&&page===1){
        var b=String(site.api||'').replace(/\/$/,'');
        var expectedCat=cat;
        NCDB.getSourceByBase(b).then(function(src){
          var srcId=src?src.id:null;
          if(!srcId){
            return NCDB.saveSource(movieConfig.site.name||'采集源',b,b).then(function(id){return id;});
          }
          return srcId;
        }).then(function(srcId){
          if(srcId&&movieState.cat===expectedCat)NCDB.saveMovies(srcId,cat,items);
        }).catch(function(e){console.error('saveMovies error:',e)});
      }
    }).catch(function(e){
      if(seq!==movieState.loadSeq||movieState.cat!==cat)return;
      console.error('[LOAD] Failed for', cat, ':', e);
      movieState.isLoading=false;hideSkeleton();
      if(movieState.listCache[cacheKey]){MOVIE_DATA=movieState.listCache[cacheKey];renderMovieHome();updateLoadMoreBtn();setMovieStatus('使用缓存数据 ('+MOVIE_DATA.length+'部)',true);}
      else{if(page===1){MOVIE_DATA=[];movieState.cat=cat;renderMovieHome();updateLoadMoreBtn()}showError('影片列表加载失败: '+e);}
    });
  }
  var tid=cat==='推荐'?'':ffzyClassId(cat);
  var params='ac=detail&page='+page+(tid?'&t='+tid:'');
  setMovieStatus('正在加载影片(第'+page+'页)...',false);
  ffzyFetch(params).then(function(data){
    if(seq!==movieState.loadSeq||movieState.cat!==cat)return;
    if(data.code!==1)throw data.msg||'返回异常';
    if(window.NCCache&&NCCache.saveCat)NCCache.saveCat(cat,page,data);
    ncSaveCmsCache(sourceBase,cat,page,data);
    var items=(data.list||[]).map(function(v){return normalizeVod(v,cat)});
    if(page===1){MOVIE_DATA=items.slice(0,60);}else{MOVIE_DATA=MOVIE_DATA.concat(items.slice(0,60));}
    movieState.cat=cat;movieState.currentPage=page;movieState.hasMore=(data.list||[]).length>=18;movieState.listCache[cacheKey]=items;
    setMovieStatus(MOVIE_DATA.length?'已加载 '+MOVIE_DATA.length+' 部影片':cat+' 暂无影片',!!MOVIE_DATA.length);hideSkeleton();renderMovieHome();updateLoadMoreBtn();ncPushHarvestTitles(items);movieState.isLoading=false;
    // 写入本地数据库
    if(window.NCDB&&page===1){
      var base=FFZY_API_BASE.replace(/\/$/,'');
      var expectedCat=cat;
      NCDB.getSourceByBase(base).then(function(src){
        var srcId=src?src.id:null;
        if(!srcId){return NCDB.saveSource('非凡采集',FFZY_API_BASE,base).then(function(id){return id;});}
        return srcId;
      }).then(function(srcId){if(srcId&&movieState.cat===expectedCat)NCDB.saveMovies(srcId,cat,items);}).catch(function(e){console.error('saveMovies error:',e)});
    }
  }).catch(function(e){
    if(seq!==movieState.loadSeq||movieState.cat!==cat)return;
    movieState.isLoading=false;hideSkeleton();
    if(movieState.listCache[cacheKey]){MOVIE_DATA=movieState.listCache[cacheKey];renderMovieHome();updateLoadMoreBtn();setMovieStatus('使用缓存数据 ('+MOVIE_DATA.length+'部)',true);}
    else{if(page===1){MOVIE_DATA=[];movieState.cat=cat;renderMovieHome();updateLoadMoreBtn()}showError('影片列表加载失败: '+e);}
  });
}
function loadMovieNextPage(){
  if(movieState.isLoading||!movieState.hasMore)return;
  loadMovieList(movieState.cat,movieState.currentPage+1);
}
function updateLoadMoreBtn(){
  var wrap=document.getElementById('tvLoadMoreWrap')||document.getElementById('movieLoadMoreWrap');
  var info=document.getElementById('tvPageInfo')||document.getElementById('moviePageInfo');
  var btn=document.getElementById('tvLoadMoreBtn')||document.getElementById('movieLoadMoreBtn');
  if(!wrap)return;
  if(movieState.hasMore&&movieState.usingRemote){
    wrap.style.display='flex';
    if(btn)btn.disabled=false;
    if(info)info.textContent='第'+movieState.currentPage+'页 · 共'+MOVIE_DATA.length+'部';
  }else{
    wrap.style.display=movieState.usingRemote?'flex':'none';
    if(info)info.textContent=movieState.usingRemote?'已加载全部':'';
    if(btn)btn.disabled=true;
  }
}
function searchMovieRemote(kw){
  saveSearchHistory(kw);
  setMovieStatus('正在搜索：'+kw,false);
  showSkeleton();hideError();hideEmptyGuide();
  if(movieConfig.site&&movieConfig.site.api){
    fetchVodSmart(apiJoin(movieConfig.site.api,'ac=detail&wd='+encodeURIComponent(kw))).then(function(data){
      MOVIE_DATA=parseVodListData(data,'搜索').slice(0,60);movieState.hasMore=false;movieState.currentPage=1;
      hideSkeleton();setMovieStatus('搜索到 '+MOVIE_DATA.length+' 条结果',true);renderMovieHome();updateLoadMoreBtn();
    }).catch(function(){hideSkeleton();renderMovieHome();setMovieStatus('远程搜索失败，显示当前列表',false)});
    return;
  }
  ffzyFetch('ac=detail&wd='+encodeURIComponent(kw)).then(function(data){
    if(data.code!==1)throw data.msg||'返回异常';
    MOVIE_DATA=(data.list||[]).map(function(v){return normalizeVod(v,'搜索')}).slice(0,60);movieState.hasMore=false;movieState.currentPage=1;
    hideSkeleton();setMovieStatus('搜索到 '+MOVIE_DATA.length+' 条结果',true);renderMovieHome();updateLoadMoreBtn();
  }).catch(function(){hideSkeleton();renderMovieHome();setMovieStatus('搜索失败，显示当前列表',false)});
}

// ===== 5. 播放相关 =====
function getVodEpisodes(v){
  if(!v)return [];
  if(v.raw&&v.raw.vod_play_url){
    var eps=ffzyParsePlayUrl(v.raw);
    if(eps&&eps.length)return eps;
  }
  return parseEpisodes(v.play);
}
function moviePlay(id){
  movieState.moviePlaySeq=(movieState.moviePlaySeq||0)+1;
  var playSeq=movieState.moviePlaySeq;
  if(movieState.lastPlayTap&&movieState.lastPlayTap.id===String(id)&&Date.now()-movieState.lastPlayTap.ts<500)return;
  movieState.lastPlayTap={id:String(id),ts:Date.now()};
  var v=movieById(id);if(!v)return;
  // 保存到历史
  if(window.NCDB){
    var item={movieId:String(v.id),title:v.title,type:v.type||v.cat||'影视',tag:v.tag||'',pic:v.pic||'',playUrl:v.play||'',timestamp:Date.now()};
    NCDB.saveHistory(item);
  }
  var eps=getVodEpisodes(v);
  if(eps.length){
    // 尝试使用EXO Player
    if(window.EXOPlayer&&EXOPlayer.isAvailable()){
      EXOPlayer.playEpisodes(v.title, eps.map(function(e){return {name:e.name,url:e.url}}));
    }else{
      openVideoModal(v,eps);
    }
    setMovieStatus('准备播放',true);
    return;
  }
  setMovieStatus('正在加载播放地址：'+v.title,false);
  // 没有集数数据，尝试加载详情
  loadMovieDetail(v,function(detail){
    if(movieState.moviePlaySeq!==playSeq)return;
    var eps2=getVodEpisodes(detail||v);
    if(window.EXOPlayer&&EXOPlayer.isAvailable()&&eps2.length){
      EXOPlayer.playEpisodes((detail||v).title||v.title, eps2.map(function(e){return {name:e.name,url:e.url}}));
    }else{
      openVideoModal(detail||v,eps2.length?eps2:parseEpisodes((detail||v).play||v.play));
    }
    setMovieStatus('播放地址已加载',true);
  });
}

function loadMovieDetail(v,callback){
  if(!movieConfig.site||!movieConfig.site.api){
    ffzyFetch('ac=detail&id='+encodeURIComponent(v.id)).then(function(data){
      if(data.code!==1)throw data.msg||'返回异常';
      var detail=(data.list||[])[0];
      callback(detail?normalizeVod(detail,v.cat):null);
    }).catch(function(){callback(null)});
    return;
  }
  var url=apiJoin(movieConfig.site.api,'ac=detail&ids='+encodeURIComponent(v.id));
  fetchVodSmart(url).then(function(data){
    var d=parseVodListData(data,v.cat)[0]||null;
    callback(d);
  }).catch(function(){callback(null)});
}
function parseEpisodes(play){
  if(!play)return [];
  return String(play).split('#').map(function(x,i){var p=x.split('$');return {name:p[0]||('第'+(i+1)+'集'),url:p[1]||p[0]||''}}).filter(function(x){return x.url});
}
function openMovieDetail(v){
  setMovieStatus('正在加载详情：'+v.title,false);
  if(!movieConfig.site||!movieConfig.site.api){
    ffzyFetch('ac=detail&id='+encodeURIComponent(v.id)).then(function(data){
      if(data.code!==1)throw data.msg||'返回异常';
      var detail=(data.list||[])[0];
      if(detail){var d=normalizeVod(detail,v.cat);var eps=ffzyParsePlayUrl(detail);openVideoModal(d,eps.length?eps:parseEpisodes(d.play));setMovieStatus('详情已加载',true)}
      else{openVideoModal(v,parseEpisodes(v.play));setMovieStatus('详情加载失败，尝试直接播放',false)}
    }).catch(function(){openVideoModal(v,parseEpisodes(v.play));setMovieStatus('详情加载失败，尝试直接播放',false)});
    return;
  }
  var url=apiJoin(movieConfig.site.api,'ac=detail&ids='+encodeURIComponent(v.id));
  fetchVodSmart(url).then(function(data){var d=parseVodListData(data,v.cat)[0]||v;openVideoModal(d,parseEpisodes(d.play||v.play));setMovieStatus('详情已加载',true)}).catch(function(){openVideoModal(v,parseEpisodes(v.play));setMovieStatus('详情加载失败，尝试直接播放',false)});
}
function resolvePlayUrl(url,parserIdx,flag){
  if(!url)return Promise.reject('空播放地址');
  if(isDirectVideoUrl(url))return Promise.resolve(url);
  var parserBase=((movieConfig.parses||[])[parserIdx||0]||{}).url||((movieConfig.parses||[])[parserIdx||0]||{}).api||'direct';
  var cacheKey='parse_cache_'+encodeURIComponent(ncSourceBase()+'|'+parserBase+'|'+url);
  try{var cached=localStorage.getItem(cacheKey);if(cached)return Promise.resolve(cached)}catch(e){}
  var ps=movieConfig.parses||[];
  var startIdx=parserIdx||0;
  if(typeof flag!=='undefined'&&flag){
    for(var fi=0;fi<ps.length;fi++){
      if(ps[fi].flag&&ps[fi].flag===flag){startIdx=fi;break}
      if(ps[fi].type&&String(ps[fi].type)===String(flag)){startIdx=fi;break}
    }
  }
  function tryParser(idx){
    if(idx>=ps.length)return Promise.reject('所有解析器均失败');
    var p=ps[idx],base=p.url||p.api||p.parse;
    if(!base)return tryParser(idx+1);
    var finalUrl=base+encodeURIComponent(url);
    return new Promise(function(resolve,reject){
      var testVid=document.createElement('video');
      testVid.preload='metadata';
      var timer=setTimeout(function(){
        testVid.removeAttribute('src');
        testVid.load();
        testVid=null;
        tryParser(idx+1).then(resolve).catch(reject)
      },8000);
      testVid.onloadedmetadata=function(){
        clearTimeout(timer);
        try{localStorage.setItem(cacheKey,finalUrl)}catch(e){}
        testVid=null;
        resolve(finalUrl)
      };
      testVid.onerror=function(){
        clearTimeout(timer);
        testVid.removeAttribute('src');
        testVid.load();
        testVid=null;
        tryParser(idx+1).then(resolve).catch(reject)
      };
      testVid.src=finalUrl;
      testVid.load();
    });
  }
  if(!ps.length)return Promise.resolve(url);
  return tryParser(startIdx);
}
function formatTime(sec){
  if(!sec||isNaN(sec))return '00:00';
  var m=Math.floor(sec/60),s=Math.floor(sec%60);
  return (m<10?'0':'')+m+':'+(s<10?'0':'')+s;
}
function savePlayProgress(videoId,time){
  try{
    var key='play_progress_'+encodeURIComponent(ncSourceBase()+'|'+(typeof videoId==='string'?videoId:(videoId.name||'')+'_'+(videoId.url||'')));
    var data={t:time,ts:Date.now()};
    localStorage.setItem(key,JSON.stringify(data));
  }catch(e){}
}
function loadPlayProgress(videoId){
  try{
    var key='play_progress_'+encodeURIComponent(ncSourceBase()+'|'+(typeof videoId==='string'?videoId:(videoId.name||'')+'_'+(videoId.url||'')));
    var raw=localStorage.getItem(key);
    if(!raw)return 0;
    var d=JSON.parse(raw);
    if(Date.now()-d.ts>7*24*3600*1000){localStorage.removeItem(key);return 0}
    return d.t||0;
  }catch(e){return 0}
}
function setupVideoTimeUpdate(){
  var player=(window.NCPlayerPlugin&&NCPlayerPlugin.video&&NCPlayerPlugin.video())||document.getElementById('videoPlayer');
  var curEl=document.getElementById('videoCurrentTime');
  var totEl=document.getElementById('videoTotalTime');
  if(!player)return;
  function updateTime(){
    curEl.textContent=formatTime(player.currentTime);
    totEl.textContent=formatTime(player.duration);
    if(player.currentTime>2&&movieState.currentEpisode){
      savePlayProgress(movieState.currentEpisode,player.currentTime);
    }
  }
  // 使用 addEventListener 避免覆盖其他模块的事件处理器
  player.addEventListener('timeupdate', updateTime);
  player.addEventListener('loadedmetadata', updateTime);
  player.addEventListener('error', function(){
    var overlay=document.getElementById('videoErrorOverlay');
    if(overlay)overlay.style.display='flex';
  });
  player.addEventListener('playing', function(){
    var overlay=document.getElementById('videoErrorOverlay');
    if(overlay)overlay.style.display='none';
  });
}
function setPlaySpeed(speed){
  var player=(window.NCPlayerPlugin&&NCPlayerPlugin.video&&NCPlayerPlugin.video())||document.getElementById('videoPlayer');
  if(player)player.playbackRate=speed;
  var btns=document.querySelectorAll('.vc-speed-btn');
  for(var i=0;i<btns.length;i++){
    var s=parseFloat(btns[i].getAttribute('data-speed'));
    if(s===speed)btns[i].classList.add('active');
    else btns[i].classList.remove('active');
  }
}
function toggleFullscreen(){
  var modal=document.getElementById('videoModal');
  var target=document.getElementById('videoPlayerWrap')||modal;
  if(!modal)return;
  if(modal.classList.contains('nc-video-fullscreen')){
    exitNcFullscreen();
  }else{
    modal.classList.add('nc-video-fullscreen');
    applyNcLandscapeMode();
    var userGesture=!navigator.userActivation||navigator.userActivation.isActive;
    if(userGesture){
      try{
        var fn=target.requestFullscreen||target.webkitRequestFullscreen||target.mozRequestFullScreen||target.msRequestFullscreen;
        if(fn)fn.call(target);
      }catch(e){}
      try{
        if(screen.orientation&&screen.orientation.lock)screen.orientation.lock('landscape').catch(function(){});
      }catch(e){}
    }
  }
  refreshNcPlayerSize();
}
function refreshNcPlayerSize(){
  setTimeout(function(){try{if(window.NCPlayerPlugin&&NCPlayerPlugin.refresh)NCPlayerPlugin.refresh();var art=window.NCPlayerPlugin&&NCPlayerPlugin.get&&NCPlayerPlugin.get();if(art&&art.resize)art.resize()}catch(e){}},60);
  setTimeout(function(){try{if(window.NCPlayerPlugin&&NCPlayerPlugin.refresh)NCPlayerPlugin.refresh();var art=window.NCPlayerPlugin&&NCPlayerPlugin.get&&NCPlayerPlugin.get();if(art&&art.resize)art.resize()}catch(e){}},320);
}
function applyNcLandscapeMode(){
  var modal=document.getElementById('videoModal');
  if(!modal)return;
  if(window.innerHeight>window.innerWidth)modal.classList.add('nc-landscape-sim');
  else modal.classList.remove('nc-landscape-sim');
}
function exitNcFullscreen(){
  var modal=document.getElementById('videoModal');
  if(modal)modal.classList.remove('nc-video-fullscreen','nc-landscape-sim');
  try{
    if(document.fullscreenElement||document.webkitFullscreenElement){
      if(document.exitFullscreen)document.exitFullscreen();
      else if(document.webkitExitFullscreen)document.webkitExitFullscreen();
    }
  }catch(e){}
  try{
    if(screen.orientation&&screen.orientation.unlock)screen.orientation.unlock();
  }catch(e){}
  refreshNcPlayerSize();
}
function retryCurrentEpisode(){
  if(movieState.currentEpisode){
    playEpisodeByIndex(movieState.currentEpisode.idx);
  }
}
function buildParserBtns(eps){
  var row=document.getElementById('parserSelectRow');
  var box=document.getElementById('parserBtns');
  if(!row||!box)return;
  var ps=movieConfig.parses||[];
  if(ps.length<1){row.style.display='none';return}
  row.style.display='flex';
  box.innerHTML='';
  for(var i=0;i<ps.length;i++){
    (function(idx){
      var b=document.createElement('button');
      b.className='parser-btn'+(idx===movieState.currentParserIdx?' active':'');
      b.textContent=ps[idx].name||('解析'+(idx+1));
      b.onclick=function(){
        movieState.currentParserIdx=idx;
        box.querySelectorAll('.parser-btn').forEach(function(x){x.classList.remove('active')});
        b.classList.add('active');
        if(movieState.currentEpisode)playEpisodeByIndex(movieState.currentEpisode.idx);
      };
      box.appendChild(b);
    })(i);
  }
}
function playEpisodeByIndex(idx){
  var eps=movieState._currentEps;
  if(!eps||!eps[idx])return;
  movieState.episodePlaySeq=(movieState.episodePlaySeq||0)+1;
  var seq=movieState.episodePlaySeq;
  var e=eps[idx];
  movieState.currentEpisode={idx:idx,name:e.name,url:e.url};
  var player=document.getElementById('videoPlayer');
  var mount=document.getElementById('artPlayerMount');
  var overlay=document.getElementById('videoErrorOverlay');
  if(overlay)overlay.style.display='none';
  if(player)player.poster='';
  resolvePlayUrl(e.url,movieState.currentParserIdx).then(function(u){
    if(seq!==movieState.episodePlaySeq)return;
    movieState.currentVideoUrl=u;
    var savedTime=loadPlayProgress(e.name+'_'+e.url);
    if(window.NCPlayerPlugin&&mount){
      try{
        if(player){player.pause();player.removeAttribute('src');player.style.display='none'}
        var art=NCPlayerPlugin.mount({container:mount,url:u,title:e.name,autoplay:true});
        setTimeout(function(){
          var v=NCPlayerPlugin.video&&NCPlayerPlugin.video();
          if(v){
            if(savedTime>5)try{v.currentTime=savedTime}catch(ex){}
            setupVideoTimeUpdate();
            setPlaySpeed(1);
          }
        },300);
      }catch(ex){
        if(player){
          player.style.display='block';
          player.src=u;
          player.load();
          if(savedTime>5)player.currentTime=savedTime;
          player.play().catch(function(){});
        }
      }
    }else if(player){
      player.style.display='block';
      player.src=u;
      player.load();
      if(savedTime>5){
        player.currentTime=savedTime;
      }
      player.play().catch(function(){});
    }
    document.getElementById('videoCurrentTime').textContent=formatTime(savedTime);
  }).catch(function(err){
    if(seq!==movieState.episodePlaySeq)return;
    var overlay=document.getElementById('videoErrorOverlay');
    if(overlay)overlay.style.display='flex';
  });
}
function openVideoModal(v,eps){
  // 如果EXO Player可用，不打开本地播放器
  if(window.EXOPlayer&&EXOPlayer.isAvailable()){
    EXOPlayer.playEpisodes(v.title, eps.map(function(e){return {name:e.name,url:e.url}}));
    return;
  }
  var modal=document.getElementById('videoModal'),title=document.getElementById('videoTitle'),player=document.getElementById('videoPlayer'),list=document.getElementById('episodeList');
  if(!modal||!player)return;
  if(window.NCPlayerPlugin)NCPlayerPlugin.destroy();
  var mount=document.getElementById('artPlayerMount');if(mount)mount.innerHTML='';
  player.style.display='none';
  title.textContent=v.title||'播放';list.innerHTML='';
  movieState._currentEps=eps||[];
  movieState.currentParserIdx=0;
  movieState.currentEpisode=null;
  movieState.currentVideoUrl='';
  movieState.episodePlaySeq=(movieState.episodePlaySeq||0)+1;
  var openSeq=movieState.episodePlaySeq;
  movieState.userSelectedEpisode=false;
  buildParserBtns(eps);
  setupVideoTimeUpdate();
  setPlaySpeed(1);
  if(!eps.length){list.innerHTML='<span style="color:#8792ad">未获取到可播放地址，该源可能需要解析器或插件。</span>';player.removeAttribute('src');modal.classList.add('show');return}
  for(var i=0;i<eps.length;i++){
    (function(idx){
      var e=eps[idx];
      var b=document.createElement('button');
      b.textContent=e.name;
      b.onclick=function(){
        movieState.userSelectedEpisode=true;
        list.querySelectorAll('button').forEach(function(x){x.classList.remove('active')});
        b.classList.add('active');
        playEpisodeByIndex(idx);
      };
      list.appendChild(b);
    })(i);
  }
  modal.classList.add('show');
  setTimeout(function(){
    if(movieState.userSelectedEpisode||movieState.episodePlaySeq!==openSeq)return;
    var firstBtn=list.querySelector('button');if(firstBtn)firstBtn.classList.add('active');
    playEpisodeByIndex(0);
  },50);
}

function closeVideoModal(){
  movieState.episodePlaySeq=(movieState.episodePlaySeq||0)+1;
  var m=document.getElementById('videoModal'),p=document.getElementById('videoPlayer');
  exitNcFullscreen();
  if(window.NCPlayerPlugin)NCPlayerPlugin.destroy();
  var mount=document.getElementById('artPlayerMount');if(mount)mount.innerHTML='';
  if(p){p.pause();p.removeAttribute('src');p.style.display='none'}
  if(m)m.classList.remove('show','nc-video-fullscreen','nc-landscape-sim');
}

// ===== 6. 收藏/历史/我的 =====
function toggleMovieFav(id){
  var fav=lsGet('movie_favs'),i=fav.indexOf(id),v=movieById(id);
  if(i>=0)fav.splice(i,1);else fav.unshift(id);
  lsSet('movie_favs',fav);
  if(v){var meta=lsGet('movie_fav_meta').filter(function(x){return x.id!==id});if(i<0){meta.unshift({id:id,title:v.title,type:v.type,tag:v.tag,pic:v.pic})}lsSet('movie_fav_meta',meta)}
  renderMovieHome();renderLibrary();
}
function switchLibraryTab(tab){localStorage.setItem('library_tab',tab);renderLibrary()}
function renderLibrary(){
  var box=document.getElementById('libraryContent');if(!box)return;
  var tab=localStorage.getItem('library_tab')||'fav',html=[];
  if(tab==='fav'){
    var meta=lsGet('movie_fav_meta'),fav=lsGet('movie_favs');
    html=fav.map(function(id){var v=movieById(id)||meta.filter(function(x){return x.id===id})[0];return v?`<div class="lib-item"><div><b>${v.title}</b><br><span>${v.type||'影视'} · ${v.tag||''}</span></div><button class="copy-btn" onclick="toggleMovieFav('${id}')">取消收藏</button></div>`:''});
    if(!html.length)html=['<div class="lib-item"><b>暂无收藏</b><span>在主页点击收藏加入</span></div>'];
  }else if(tab==='history'){
    var his=lsGet('movie_history');
    html=his.map(function(x){return `<div class="lib-item"><div><b>${x.title||x.id}</b><br><span>${x.time||''}</span></div><button class="copy-btn" onclick="switchMainPage('home')">去首页</button></div>`});
    if(!html.length)html=['<div class="lib-item"><b>暂无观看历史</b><span>点击影片播放后自动记录</span></div>'];
  }else{
    html=['<div class="lib-item"><b>暂无数据</b><span>暂无历史记录</span></div>'];
  }
  box.innerHTML='<div class="lib-list">'+html.join('')+'</div>';
  document.querySelectorAll('.lib-tab').forEach(function(b){b.classList.remove('active')});
  var idx={fav:0,history:1}[tab]||0,bs=document.querySelectorAll('.lib-tab');if(bs[idx])bs[idx].classList.add('active');
}
function renderMine(){
  var fav=lsGet('movie_favs'),his=lsGet('movie_history'),cfg=localStorage.getItem('movie_config_url')||'';
  var a=document.getElementById('mineFavCount'),b=document.getElementById('mineHistoryCount'),inp=getMovieConfigInput();
  if(a)a.textContent=fav.length;
  if(b){
    // 优先从数据库读取
    if(window.NCDB){
      NCDB.getHistoryCount().then(function(count){b.textContent=count}).catch(function(){b.textContent=his.length});
    }else{
      b.textContent=his.length;
    }
  }
  if(inp&&cfg&&!movieState.loaded)inp.value=cfg;
}

function updateMineLiveCount(){
  var el=document.getElementById('mineLiveCount');
  if(!el||!window.NCDB)return;
  NCDB.getLiveChannels().then(function(channels){
    el.textContent=channels?channels.length:0;
  }).catch(function(){el.textContent=0});
}

window.showAddLiveChannel=function(){
  var name=prompt('直播频道名称：','央视一套');
  if(!name)return;
  var url=prompt('直播地址（M3U8/TXT/M3U）：','http://');
  if(!url)return;
  var group=prompt('分组名称（如：央视、卫视）','央视');
  if(window.NCDB){
    NCDB.saveLiveChannel({name:name,url:url,group:group||'默认'}).then(function(){
      renderLiveSourceManager&&renderLiveSourceManager();
      updateMineLiveCount();
      alert('直播源添加成功');
    }).catch(function(e){alert('添加失败：'+e)});
  }
};

window.refreshLiveChannels=function(){
  if(window.initLivePage)initLivePage();
};
function showConfigHistory(){
  var list=lsGet('movie_config_history');
  if(!list.length){alert('暂无配置历史');return}
  var msg=list.map(function(x,i){return (i+1)+'. '+x.url+'\\n   '+x.time}).join('\\n');
  var n=prompt('输入序号切换配置：\\n'+msg,'1');
  var idx=parseInt(n,10)-1;
  if(list[idx]){var inp=getMovieConfigInput();if(inp)inp.value=list[idx].url;switchMainPage('home');loadMovieConfig()}
}
function clearMovieData(){
  if(window.NCDB){
    NCDB.getSources().then(function(sources){
      var promises=sources.map(function(s){return NCDB.clearSource(s.id)});
      promises.push(NCDB.clearHistory());
      promises.push(NCDB.clearFavorites());
      Promise.all(promises).then(function(){
        MOVIE_DATA=[];movieState.history=[];movieState.favorites=[];
        renderMovieHome();renderLibrary();renderMine();alert('已清空所有影视数据');
      });
    }).catch(function(e){alert('清空失败：'+e)});
  }else{
    lsSet('movie_favs',[]);lsSet('movie_fav_meta',[]);lsSet('movie_history',[]);
    renderMovieHome();renderLibrary();renderMine();alert('已清空影视历史和收藏');
  }
}

// ===== 导出到全局作用域（file:// WebView 不支持 ES modules）=====
 window.MOVIE_CATS=MOVIE_CATS;
 window.MOVIE_DATA=MOVIE_DATA;
 window.movieState=movieState;
 window.FFZY_API_BASE=FFZY_API_BASE;
 window.FFZY_CLASSES=FFZY_CLASSES;
 window.FFZY_NAME_MAP=FFZY_NAME_MAP;
 window.ffzyClassId=ffzyClassId;
 window.ffzyClassName=ffzyClassName;
window.ffzyFetch=ffzyFetch;
window.ffzyParsePlayUrl=ffzyParsePlayUrl;
window.ffzyDirectM3u8=ffzyDirectM3u8;
window.movieConfig=movieConfig;
window.lsGet=lsGet;
window.lsSet=lsSet;
window.setMovieStatus=setMovieStatus;
window.ncSourceBase=ncSourceBase;
window.ncCmsCacheKey=ncCmsCacheKey;
window.ncHomeCacheKey=ncHomeCacheKey;
window.ncSaveCmsCache=ncSaveCmsCache;
window.ncReadCmsCache=ncReadCmsCache;
window.ncNormalizePic=ncNormalizePic;
window.fetchTextSmart=fetchTextSmart;
window.fetchJsonSmart=fetchJsonSmart;
window.fetchVodSmart=fetchVodSmart;
window.apiJoin=apiJoin;
window.isDirectVideoUrl=isDirectVideoUrl;
window.textOf=textOf;
window.parseVodClasses=parseVodClasses;
window.parseVodListData=parseVodListData;
window.normalizeVod=normalizeVod;
window.updateSiteSelect=updateSiteSelect;
window.switchMainPage=switchMainPage;
window.movieById=movieById;
window.renderMovieHome=renderMovieHome;
window.movieSetCat=movieSetCat;
window.movieSearch=movieSearch;
window.movieShowAll=movieShowAll;
window.movieRefresh=movieRefresh;
window.chooseUsableSite=chooseUsableSite;
window.configCandidates=configCandidates;
window.loadConfigCandidate=loadConfigCandidate;
window.getMovieConfigInput=getMovieConfigInput;
window.applyMovieConfig=applyMovieConfig;
window.loadMovieConfig=loadMovieConfig;
window.ffzyInit=ffzyInit;
window.ncTickerState=ncTickerState;
window.ncPushHarvestTitles=ncPushHarvestTitles;
window.ncShowNextTitle=ncShowNextTitle;
window.hydrateCmsHome=hydrateCmsHome;
window.initCmsApi=initCmsApi;
window.saveConfigHistory=saveConfigHistory;
window.saveMovieConfig=saveMovieConfig;
window.scanMovieConfig=scanMovieConfig;
window.decodeConfigImage=decodeConfigImage;
window.switchMovieSite=switchMovieSite;
window.loadMovieClasses=loadMovieClasses;
window.classIdByName=classIdByName;
window.loadLiveSources=loadLiveSources;
window.parseLiveText=parseLiveText;
window.renderLiveGrid=renderLiveGrid;
window.playLive=playLive;
window.loadMovieList=loadMovieList;
window.loadMovieNextPage=loadMovieNextPage;
window.updateLoadMoreBtn=updateLoadMoreBtn;
window.searchMovieRemote=searchMovieRemote;
window.getVodEpisodes=getVodEpisodes;
window.moviePlay=moviePlay;
window.parseEpisodes=parseEpisodes;
window.openMovieDetail=openMovieDetail;
window.resolvePlayUrl=resolvePlayUrl;
window.formatTime=formatTime;
window.savePlayProgress=savePlayProgress;
window.loadPlayProgress=loadPlayProgress;
window.setupVideoTimeUpdate=setupVideoTimeUpdate;
window.setPlaySpeed=setPlaySpeed;
window.toggleFullscreen=toggleFullscreen;
window.refreshNcPlayerSize=refreshNcPlayerSize;
window.applyNcLandscapeMode=applyNcLandscapeMode;
window.exitNcFullscreen=exitNcFullscreen;
window.retryCurrentEpisode=retryCurrentEpisode;
window.buildParserBtns=buildParserBtns;
window.playEpisodeByIndex=playEpisodeByIndex;
window.openVideoModal=openVideoModal;
window.closeVideoModal=closeVideoModal;
window.toggleMovieFav=toggleMovieFav;
window.switchLibraryTab=switchLibraryTab;
window.renderLibrary=renderLibrary;
window.renderMine=renderMine;
window.showConfigHistory=showConfigHistory;
window.clearMovieData=clearMovieData;
console.log('[ENGINE] CSP_API_MAP registered, keys:', Object.keys(CSP_API_MAP).length);
window.normalizeCatName=normalizeCatName;

}})();