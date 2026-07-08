// NewCloud 播放器脚本插件：ArtPlayer + hls.js
// 用法：NCPlayerPlugin.mount({ container, url, title, autoplay })
(function(){
  var current=null;
  function isM3u8(url){return /\.m3u8(\?|#|$)/i.test(String(url||''));}
  function destroy(){
    if(current){
      try{current.destroy(false)}catch(e){}
      current=null;
    }
  }
  function playM3u8(video,url,art){
    if(window.Hls&&Hls.isSupported()){
      var hls=new Hls({
        enableWorker:true,
        lowLatencyMode:false,
        backBufferLength:30,
        maxBufferLength:25,
        maxMaxBufferLength:60
      });
      hls.loadSource(url);
      hls.attachMedia(video);
      art.on('destroy',function(){try{hls.destroy()}catch(e){}});
    }else if(video.canPlayType('application/vnd.apple.mpegurl')){
      video.src=url;
    }else{
      video.src=url;
    }
  }
  function refreshSize(){
    if(!current)return;
    try{current.resize()}catch(e){}
    var v=video();
    if(v){
      v.style.cssText='width:100%;height:100%;object-fit:contain;display:block;';
    }
    var ap=document.getElementById('artPlayerMount');
    if(ap){
      var artEl=ap.querySelector('.art-video-player');
      if(artEl){
        artEl.style.cssText='width:100%!important;height:100%!important;';
      }
      ap.style.cssText='width:100%;height:100%;min-height:100%;';
      var artVideo=ap.querySelector('.art-video');
      if(artVideo){
        artVideo.style.setProperty('max-height','none','important');
        artVideo.style.setProperty('height','100%','important');
        artVideo.style.setProperty('width','100%','important');
      }
    }
  }
  function mount(opt){
    opt=opt||{};
    var container=typeof opt.container==='string'?document.querySelector(opt.container):opt.container;
    if(!container)throw new Error('播放器容器不存在');
    destroy();
    container.innerHTML='';
    if(!container.style.height&&container.clientHeight<80){
      container.style.height='100%';
    }
    if(!window.Artplayer){
      throw new Error('ArtPlayer未加载');
    }
    current=new Artplayer({
      container:container,
      url:opt.url||'',
      title:opt.title||'',
      type:isM3u8(opt.url)?'m3u8':'auto',
      autoplay:opt.autoplay!==false,
      muted:false,
      pip:true,
      fullscreen:true,
      fullscreenWeb:true,
      setting:true,
      playbackRate:true,
      autoSize:false,
      autoMini:false,
      lock:true,
      fastForward:true,
      customType:{
        m3u8:function(video,url,art){
          playM3u8(video,url,art);
        }
      }
    });
    setTimeout(function(){
      try{if(current&&current.resize)current.resize()}catch(e){}
      var v=video();
      if(v){
        v.style.width='100%';
        v.style.height='100%';
        v.style.objectFit='contain';
      }
      refreshSize();
    },120);
    setTimeout(function(){
      try{if(current&&current.resize)current.resize()}catch(e){}
      refreshSize();
    },500);
    var _fsObs=null;
    try{
      _fsObs=new MutationObserver(function(){refreshSize()});
      var modal=document.getElementById('videoModal');
      if(modal)_fsObs.observe(modal,{attributes:true,attributeFilter:['class']});
      current.on('destroy',function(){if(_fsObs)_fsObs.disconnect()});
    }catch(e){}
    return current;
  }
  function video(){
    if(current&&current.video)return current.video;
    var box=document.getElementById('artPlayerMount');
    return box?box.querySelector('video'):null;
  }
  window.NCPlayerPlugin={mount:mount,destroy:destroy,refresh:refreshSize,get:function(){return current},video:video};
})();
