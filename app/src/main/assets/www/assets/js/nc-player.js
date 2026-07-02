// NewCloud 播放器增强模块：亮度/音量手势、长按2倍速
(function(){
  var longTimer=null,oldRate=1,longPressActive=false,startX=0,startY=0,gestureSide='',gestureMode='',baseVolume=.8,brightness=.0,tipTimer=null;

  function player(){
    if(window.NCPlayerPlugin&&NCPlayerPlugin.video&&NCPlayerPlugin.video())return NCPlayerPlugin.video();
    return document.getElementById('videoPlayer');
  }
  function wrap(){return document.getElementById('videoPlayerWrap')}
  function panel(){return document.getElementById('videoModal')}
  function ensureLayer(){
    var w=wrap();if(!w)return null;
    var layer=document.getElementById('ncBrightnessLayer');
    if(!layer){
      layer=document.createElement('div');
      layer.id='ncBrightnessLayer';
      layer.className='nc-brightness-layer';
      w.appendChild(layer);
    }
    return layer;
  }
  function ensureTip(){
    var w=wrap();if(!w)return null;
    var tip=document.getElementById('ncVideoTip');
    if(!tip){
      tip=document.createElement('div');
      tip.id='ncVideoTip';
      tip.className='nc-video-tip';
      w.appendChild(tip);
    }
    return tip;
  }
  function showTip(text){
    var tip=ensureTip();if(!tip)return;
    tip.textContent=text;
    tip.classList.add('show');
    clearTimeout(tipTimer);
    tipTimer=setTimeout(function(){tip.classList.remove('show')},700);
  }
  function applyBrightness(){
    var layer=ensureLayer();
    if(layer)layer.style.background='rgba(0,0,0,'+Math.max(0,Math.min(.7,brightness))+')';
  }

  function requestFull(el){
    el=el||wrap()||panel()||document.documentElement;
    var fn=el.requestFullscreen||el.webkitRequestFullscreen||el.mozRequestFullScreen||el.msRequestFullscreen;
    if(fn){
      try{return fn.call(el)}catch(e){}
    }
    return Promise.resolve();
  }
  function exitFull(){
    var fn=document.exitFullscreen||document.webkitExitFullscreen||document.mozCancelFullScreen||document.msExitFullscreen;
    if(fn){try{return fn.call(document)}catch(e){}}
  }
  function isFull(){
    return !!(document.fullscreenElement||document.webkitFullscreenElement||document.mozFullScreenElement||document.msFullscreenElement);
  }
  function enterPlayerFullscreen(){
    var w=wrap()||panel();
    if(!w||isFull())return Promise.resolve();
    return Promise.resolve(requestFull(w)).catch(function(){});
  }

  function bindFullscreenExit(){
    if(document.documentElement.dataset.ncFsBind)return;
    document.documentElement.dataset.ncFsBind='1';
    document.addEventListener('fullscreenchange',function(){
      var modal=panel();
      if(modal&&!document.fullscreenElement){
        modal.classList.remove('nc-video-fullscreen','nc-landscape-sim');
      }
      setTimeout(function(){try{if(window.NCPlayerPlugin&&NCPlayerPlugin.refresh)NCPlayerPlugin.refresh();var art=window.NCPlayerPlugin&&NCPlayerPlugin.get&&NCPlayerPlugin.get();if(art&&art.resize)art.resize()}catch(e){}},120);
    });
    document.addEventListener('webkitfullscreenchange',function(){
      var modal=panel();
      if(modal&&!document.webkitFullscreenElement){
        modal.classList.remove('nc-video-fullscreen','nc-landscape-sim');
      }
      setTimeout(function(){try{if(window.NCPlayerPlugin&&NCPlayerPlugin.refresh)NCPlayerPlugin.refresh();var art=window.NCPlayerPlugin&&NCPlayerPlugin.get&&NCPlayerPlugin.get();if(art&&art.resize)art.resize()}catch(e){}},120);
    });
    window.addEventListener('resize',function(){
      var modal=panel();
      if(modal&&modal.classList.contains('nc-video-fullscreen')&&window.applyNcLandscapeMode){
        window.applyNcLandscapeMode();
      }
      setTimeout(function(){try{if(window.NCPlayerPlugin&&NCPlayerPlugin.refresh)NCPlayerPlugin.refresh()}catch(e){}},160);
    });
  }

  function bindGestures(){
    var w=wrap();
    if(!w||w.dataset.ncGesture)return;
    w.dataset.ncGesture='1';
    function isControlTarget(el){
      return !!(el&&el.closest&&el.closest('button,.art-controls,.art-bottom,.art-progress,.art-setting,.art-volume,.art-fullscreen,.nc-exit-fullscreen-btn,.video-error-overlay,.episode-list,.parser-btns'));
    }
    w.addEventListener('touchstart',function(e){
      var p=player();if(!p)return;
      if(!e.touches||!e.touches[0])return;
      if(isControlTarget(e.target)){clearTimeout(longTimer);gestureMode='control';return}
      var t=e.touches[0],rect=w.getBoundingClientRect();
      startX=t.clientX;startY=t.clientY;
      gestureSide=startX<rect.left+rect.width/2?'left':'right';
      gestureMode='';
      baseVolume=p.volume;
      oldRate=p.playbackRate||1;
      longPressActive=false;
      longTimer=setTimeout(function(){
        var p=player();if(!p)return;
        longPressActive=true;
        p.playbackRate=2;
        showTip('2倍速快进');
      },450);
    },{passive:false});
    w.addEventListener('touchmove',function(e){
      var p=player();if(!p)return;
      if(!e.touches||!e.touches[0])return;
      if(gestureMode==='control')return;
      var t=e.touches[0],dy=startY-t.clientY,dx=t.clientX-startX,adx=Math.abs(dx),ady=Math.abs(dy);
      if(Math.hypot(adx,ady)>8)clearTimeout(longTimer);
      if(!gestureMode&&ady>18&&ady>adx*1.25)gestureMode='vertical';
      if(gestureMode!=='vertical')return;
      if(e.cancelable)e.preventDefault();
      e.stopPropagation();
      clearTimeout(longTimer);
      var delta=dy/260;
      if(gestureSide==='left'){
        brightness=Math.max(0,Math.min(.7,brightness-delta*.08));
        applyBrightness();
        showTip('亮度 '+Math.round((1-brightness/.7)*100)+'%');
      }else{
        baseVolume=Math.max(0,Math.min(1,baseVolume+delta*.08));
        p.volume=baseVolume;
        showTip('音量 '+Math.round(baseVolume*100)+'%');
      }
      startY=t.clientY;
    },{passive:false});
    function finishTouch(e){
      if(gestureMode==='vertical'||longPressActive){if(e&&e.cancelable)e.preventDefault();if(e)e.stopPropagation();}
      var p=player();if(!p)return;
      clearTimeout(longTimer);
      if(longPressActive){
        var targetRate=oldRate&&oldRate!==2?oldRate:1;
        p.playbackRate=targetRate;
        showTip(targetRate+'倍速');
        longPressActive=false;
      }
      gestureMode='';
    }
    w.addEventListener('touchend',finishTouch,{passive:false});
    w.addEventListener('touchcancel',finishTouch,{passive:false});
  }

  var oldOpen=window.openVideoModal;
  if(oldOpen){
    window.openVideoModal=function(v,eps){
      oldOpen.apply(this,arguments);
      setTimeout(function(){
        bindGestures();
      },80);
    };
  }

  var oldPlayEpisode=window.playEpisodeByIndex;
  if(oldPlayEpisode){
    window.playEpisodeByIndex=function(idx){
      return oldPlayEpisode.apply(this,arguments);
    };
  }

  var oldClose=window.closeVideoModal;
  if(oldClose){
    window.closeVideoModal=function(){
      if(isFull())exitFull();
      var modal=panel();
      if(modal)modal.classList.remove('nc-video-fullscreen','nc-landscape-sim');
      return oldClose.apply(this,arguments);
    };
  }

  setTimeout(function(){bindGestures()},800);
  bindFullscreenExit();
})();
