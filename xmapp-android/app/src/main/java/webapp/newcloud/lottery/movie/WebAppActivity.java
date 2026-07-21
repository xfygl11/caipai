package webapp.newcloud.lottery.movie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import webapp.newcloud.lottery.movie.player.ExoPlayerActivity;
import webapp.newcloud.lottery.movie.player.InPagePlayerOverlay;

public class WebAppActivity extends Activity {

    private static final String TAG = "WebAppActivity";
    private WebView webView;
    private LocalStorageBridge localStorageBridge;
    private InPagePlayerOverlay currentInlineOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_webapp);

        webView = findViewById(R.id.webView);
        localStorageBridge = new LocalStorageBridge(this);

        configureWebView();

        webView.loadUrl("file:///android_asset/index.html");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "Page started: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished: " + url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Intercept URL: " + url);
                return false;
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        // 本应用从 file:///android_asset/ 加载，需要用 fetch() 访问外部 HTTP API
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setScrollbarFadingEnabled(false);

        webView.addJavascriptInterface(new AndroidBridge(), "androidBridge");
        webView.addJavascriptInterface(new NativeHttpBridge(), "NativeHttp");
        webView.addJavascriptInterface(new StorageBridge(), "NativeStorage");
        webView.addJavascriptInterface(new ExoPlayerBridge(), "exoPlayer");
    }

    public class AndroidBridge {
        @JavascriptInterface
        public String getVersion() {
            return "1.0";
        }

        @JavascriptInterface
        public String showToast(String msg) {
            final String message = msg != null ? msg : "";
            runOnUiThread(() -> Toast.makeText(WebAppActivity.this, message, Toast.LENGTH_SHORT).show());
            return "ok";
        }

        @JavascriptInterface
        public String log(String msg) {
            Log.d("TVBox", msg != null ? msg : "");
            return "ok";
        }
    }

    public class NativeHttpBridge {
        @JavascriptInterface
        public String httpGet(String url) {
            if (url == null || url.isEmpty()) {
                return null;
            }
            return NativeHttp.httpGet(url);
        }

        @JavascriptInterface
        public String httpGetBytes(String url) {
            if (url == null || url.isEmpty()) {
                return null;
            }
            byte[] bytes = NativeHttp.httpGetBytes(url);
            if (bytes == null) return null;
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        }

        @JavascriptInterface
        public void httpPost(String url, String postData, String callbackId) {
            NativeHttp.httpPostAsync(url, postData, new NativeHttp.HttpCallback() {
                @Override
                public void onSuccess(String response) {
                    if (callbackId != null && !callbackId.isEmpty()) {
                        webView.post(() -> {
                            String js = String.format("window.%s('%s');", callbackId, escapeJs(response != null ? response : ""));
                            webView.evaluateJavascript(js, null);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (callbackId != null && !callbackId.isEmpty()) {
                        webView.post(() -> {
                            String js = String.format("window.%s(null, '%s');", callbackId, escapeJs(error != null ? error : "Unknown error"));
                            webView.evaluateJavascript(js, null);
                        });
                    }
                }
            });
        }

        private String escapeJs(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("`", "\\`")
                    .replace("$", "\\$")
                    .replace("</script>", "<\\/script>");
        }
    }

    public class StorageBridge {
        @JavascriptInterface
        public String getItem(String key) {
            Object value = localStorageBridge.getItem(key);
            if (value == null) return null;
            if (value instanceof String) return (String) value;
            return value.toString();
        }

        @JavascriptInterface
        public void setItem(String key, String value) {
            if (key == null || value == null) return;
            localStorageBridge.setItem(key, value);
        }

        @JavascriptInterface
        public void removeItem(String key) {
            localStorageBridge.removeItem(key);
        }

        @JavascriptInterface
        public void clear() {
            localStorageBridge.clear();
        }

        @JavascriptInterface
        public int length() {
            return localStorageBridge.size();
        }

        @JavascriptInterface
        public String key(int index) {
            return localStorageBridge.key(index);
        }

        @JavascriptInterface
        public String exportAll() {
            return localStorageBridge.exportAll();
        }

        @JavascriptInterface
        public void importAll(String json) {
            localStorageBridge.importAll(json);
        }
    }

    public class ExoPlayerBridge {
        @JavascriptInterface
        public String play(String jsonParams) {
            Log.d(TAG, "EXOPlayer.play: " + jsonParams);
            try {
                android.os.Bundle extras = parseJsonToBundle(jsonParams);
                Intent intent = new Intent(WebAppActivity.this, ExoPlayerActivity.class);
                intent.putExtras(extras);
                startActivity(intent);
                return "ok";
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse ExoPlayer params", e);
                return "error: " + e.getMessage();
            }
        }

        @JavascriptInterface
        public String playInline(String jsonParams) {
            Log.d(TAG, "EXOPlayer.playInline: " + jsonParams);
            try {
                final android.os.Bundle extras = parseJsonToBundle(jsonParams);
                final String title = extras.getString("title", "");
                final String url = extras.getString("url", "");
                final boolean isLive = extras.getBoolean("isLive", false);
                Log.d(TAG, "Creating InPagePlayerOverlay: title=" + title + " url=" + url);

                // 同步保护 currentInlineOverlay 防止竞态创建
                synchronized (WebAppActivity.this) {
                    final InPagePlayerOverlay oldOverlay = currentInlineOverlay;
                    currentInlineOverlay = null;

                    runOnUiThread(() -> {
                        if (oldOverlay != null) {
                            Log.d(TAG, "Dismissing previous InPagePlayerOverlay");
                            oldOverlay.dismiss();
                        }
                        InPagePlayerOverlay overlay = new InPagePlayerOverlay(webView, title, url, isLive);
                        synchronized (WebAppActivity.this) {
                            if (currentInlineOverlay == null) {
                                currentInlineOverlay = overlay;
                            } else {
                                overlay.dismiss();
                                return;
                            }
                        }
                        Log.d(TAG, "InPagePlayerOverlay created, showing...");
                        overlay.show();
                    });
                }
                return "ok";
            } catch (Exception e) {
                Log.e(TAG, "Failed to create InPagePlayerOverlay", e);
                return "error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            }
        }

        @JavascriptInterface
        public String playEpisodes(String jsonParams) {
            Log.d(TAG, "EXOPlayer.playEpisodes: " + jsonParams);
            try {
                android.os.Bundle extras = parseJsonToBundle(jsonParams);
                Intent intent = new Intent(WebAppActivity.this, ExoPlayerActivity.class);
                intent.putExtras(extras);
                intent.putExtra("EPISODES_MODE", true);
                startActivity(intent);
                return "ok";
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse ExoPlayer episode params", e);
                return "error: " + e.getMessage();
            }
        }

        private android.os.Bundle parseJsonToBundle(String json) throws Exception {
            org.json.JSONObject obj = new org.json.JSONObject(json);
            android.os.Bundle bundle = new android.os.Bundle();
            if (obj.has("title")) bundle.putString("title", obj.getString("title"));
            if (obj.has("url")) bundle.putString("url", obj.getString("url"));
            if (obj.has("poster")) bundle.putString("poster", obj.getString("poster"));
            if (obj.has("isLive")) bundle.putBoolean("isLive", obj.getBoolean("isLive"));
            if (obj.has("episodes")) {
                org.json.JSONArray arr = obj.getJSONArray("episodes");
                String[] titles = new String[arr.length()];
                String[] urls = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject ep = arr.getJSONObject(i);
                    titles[i] = ep.optString("name", "");
                    urls[i] = ep.optString("url", "");
                }
                bundle.putStringArray("epTitles", titles);
                bundle.putStringArray("epUrls", urls);
            }
            return bundle;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                webView.requestFocus();
                return webView.dispatchKeyEvent(event);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (currentInlineOverlay != null) {
            currentInlineOverlay.dismiss();
            currentInlineOverlay = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface("android");
            webView.removeJavascriptInterface("nativeHttp");
            webView.removeJavascriptInterface("Storage");
            webView.removeJavascriptInterface("exoPlayer");
            webView.setWebViewClient(null);
            webView.stopLoading();
            webView.destroy();
        }
        NativeHttp.shutdown();
        super.onDestroy();
    }
}
