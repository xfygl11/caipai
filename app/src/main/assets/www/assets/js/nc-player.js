// NewCloud 播放器增强模块：全屏绑定
(function(){
  function wrap(){return document.getElementById('videoPlayerWrap')}
  function panel(){return document.getElementById('videoModal')}

  function bindFullscreenExit(){
    if(document.documentElement.dataset.ncFsBind)return;
    document.documentElement.dataset.ncFsBind='1';
    document.addEventListener('fullscreenchange',function(){
      var modal=panel();
      if(modal&&!document.fullscreenElement){
        modal.classList.remove('nc-video-fullscreen','nc-landscape-sim');
      }
    });
    document.addEventListener('webkitfullscreenchange',function(){
      var modal=panel();
      if(modal&&!document.webkitFullscreenElement){
        modal.classList.remove('nc-video-fullscreen','nc-landscape-sim');
      }
    });
  }

  bindFullscreenExit();
  window.bindNcFullscreenExit=bindFullscreenExit;
})();
