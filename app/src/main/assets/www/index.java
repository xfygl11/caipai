import android.content.*;
import android.webkit.*;
import android.os.*;

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

webview.loadUrl(webapp.getpath()+"main.html");
