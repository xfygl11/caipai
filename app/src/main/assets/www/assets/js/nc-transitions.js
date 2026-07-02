// NewCloud 页面切换动效模块
(function(){
  if(window._ncAppRenderMovieHome)return; // app.js 统一管理，不覆盖
  var oldSwitch=window.switchMainPage;
  window.switchMainPage=function(page){
    if(typeof oldSwitch==='function')oldSwitch(page);
    var target=document.querySelector('.main-page[data-main="'+page+'"]');
    if(target&&page!=='lottery'){
      target.classList.remove('nc-page-anim');
      void target.offsetWidth;
      target.classList.add('nc-page-anim');
      setTimeout(function(){target.classList.remove('nc-page-anim')},260);
    }
  };
})();
