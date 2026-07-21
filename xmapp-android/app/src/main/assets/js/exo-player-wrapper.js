// EXO Player 封装模块 - JavaScript 接口
// 通过 WebView 的 addJavascriptInterface 与原生 Android ExoPlayerActivity 通信

(function(){
  window.EXOPlayer = {
    // 播放视频
    play: function(title, url, poster) {
      if (window.exoPlayer) {
        exoPlayer.play(JSON.stringify({
          title: title || '视频',
          url: url || '',
          poster: poster || ''
        }));
      } else {
        console.warn('EXO Player 未初始化，回退到默认播放器');
      }
    },
    
    // 播放集数列表
    playEpisodes: function(title, episodes) {
      if (window.exoPlayer) {
        exoPlayer.playEpisodes(JSON.stringify({
          title: title || '视频',
          episodes: episodes || []
        }));
      } else {
        console.warn('EXO Player 未初始化，回退到默认播放器');
      }
    },
    
    // 播放直播
    playLive: function(title, url) {
      if (window.exoPlayer) {
        exoPlayer.play(JSON.stringify({
          title: title || '直播',
          url: url || '',
          isLive: true
        }));
      } else {
        console.warn('EXO Player 未初始化，回退到默认播放器');
      }
    },
    
    // 内嵌播放（在直播页面播放）
    playInline: function(params) {
      console.log('[EXO] playInline called, window.exoPlayer=', !!window.exoPlayer, 'params=', params);
      if (window.exoPlayer) {
        console.log('[EXO] Calling window.exoPlayer.playInline...');
        exoPlayer.playInline(params);
        console.log('[EXO] window.exoPlayer.playInline called successfully');
      } else {
        console.warn('EXO Player 未初始化');
      }
    },
    
    // 检查是否可用
    isAvailable: function() {
      return !!(window.exoPlayer);
    }
  };
})();
