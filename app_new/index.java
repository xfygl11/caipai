import android.content.*;
import android.webkit.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

//已内置变量context和webview
//运行环境:BeanShell/私有接口通用
//使用setContentView方法可创建原生界面

webview.getSettings().setJavaScriptEnabled(true);
webview.getSettings().setDomStorageEnabled(true);
webview.getSettings().setDatabaseEnabled(true);
webview.getSettings().setAllowFileAccess(true);
webview.getSettings().setAllowContentAccess(true);
webview.getSettings().setAllowFileAccessFromFileURLs(true);
webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
if(Build.VERSION.SDK_INT>=21){
    webview.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
}
webview.getSettings().setMediaPlaybackRequiresUserGesture(false);

// 添加 JavaScript 接口供 EXO Player 调用
webview.addJavascriptInterface(new Object() {
    public void play(String jsonData) {
        try {
            Intent intent = new Intent(context, ExoPlayerActivity.class);
            intent.putExtra("play_data", jsonData);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("ExoPlayer", "play error: " + e.getMessage());
        }
    }
    
    public void playEpisodes(String jsonData) {
        try {
            Intent intent = new Intent(context, ExoPlayerActivity.class);
            intent.putExtra("play_data", jsonData);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("ExoPlayer", "playEpisodes error: " + e.getMessage());
        }
    }
}, "exoPlayer");

webview.loadUrl(webapp.getpath()+"main.html");
