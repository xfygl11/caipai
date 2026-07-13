package webapp.newcloud.lottery.movie;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        configureWebView();
        loadContent();
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this,
                            "页面加载错误: " + error.getDescription(),
                            Toast.LENGTH_SHORT).show();
                }
                super.onReceivedError(view, request, error);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void play(String jsonData) {
                launchPlayer(jsonData);
            }

            @android.webkit.JavascriptInterface
            public void playEpisodes(String jsonData) {
                launchPlayer(jsonData);
            }

            private void launchPlayer(final String jsonData) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        Intent intent = new Intent(MainActivity.this, ExoPlayerActivity.class);
                        intent.putExtra("play_data", jsonData);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("ExoPlayer", "launch error: " + e.getMessage());
                    }
                });
            }
        }, "exoPlayer");

        webView.addJavascriptInterface(new NativeHttpInterface(), "NativeHttp");
    }

    private void loadContent() {
        webView.loadUrl("file:///android_asset/main.html");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    // ===== Native HTTP 桥接 =====
    private static class NativeHttpInterface {
        @android.webkit.JavascriptInterface
        public String httpGet(String url) {
            try {
                String encodedUrl = encodeUrl(url);
                String result = doHttpGet(encodedUrl, 0);
                return result;
            } catch (Exception e) {
                return "__ERROR__" + e.getMessage();
            }
        }

        private String doHttpGet(String urlStr, int redirectCount) throws Exception {
            if (redirectCount > 5) throw new Exception("Too many redirects");
            
            java.net.URL uri = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            
            // 手动处理重定向（支持 HTTP→HTTPS 跨协议）
            if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location != null && !location.isEmpty()) {
                    if (location.startsWith("//")) {
                        location = "https:" + location;
                    } else if (location.startsWith("/")) {
                        java.net.URL base = new java.net.URL(urlStr);
                        location = base.getProtocol() + "://" + base.getHost() + 
                            (base.getPort() != -1 ? ":" + base.getPort() : "") + location;
                    } else if (!location.startsWith("http")) {
                        java.net.URL base = new java.net.URL(urlStr);
                        String basePath = base.getPath();
                        if (basePath != null && basePath.contains("/")) {
                            basePath = basePath.substring(0, basePath.lastIndexOf('/') + 1);
                        } else {
                            basePath = "/";
                        }
                        location = base.getProtocol() + "://" + base.getHost() + 
                            (base.getPort() != -1 ? ":" + base.getPort() : "") + basePath + location;
                    }
                    location = encodeUrl(location);
                    return doHttpGet(location, redirectCount + 1);
                }
                throw new Exception("Redirect without Location header");
            }
            
            if (responseCode != 200) {
                conn.disconnect();
                throw new Exception("HTTP " + responseCode);
            }
            
            String contentType = conn.getContentType();
            java.io.InputStream is = conn.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            is.close();
            bos.close();
            conn.disconnect();
            
            byte[] data = bos.toByteArray();
            
            // 如果是图片(JPEG等二进制)，用ISO-8859-1保留原始字节
            if (contentType != null && (contentType.contains("image") || contentType.contains("octet-stream"))) {
                return new String(data, "ISO-8859-1");
            }
            
            return new String(data, "UTF-8");
        }

        private String encodeUrl(String url) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "(https?://)([^/:?#\\s]+)(:\\d+)?(/[^?#]*)?(\\?[^#]*)?(#.*)?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
                );
                java.util.regex.Matcher m = p.matcher(url);
                if (m.find()) {
                    String scheme = m.group(1);
                    String host = m.group(2);
                    String port = m.group(3) != null ? m.group(3) : "";
                    String path = m.group(4) != null ? m.group(4) : "";
                    String query = m.group(5) != null ? m.group(5) : "";
                    String fragment = m.group(6) != null ? m.group(6) : "";
                    
                    if (host != null && host.matches(".*[\\u4e00-\\u9fa5].*")) {
                        host = java.net.IDN.toASCII(host);
                    }
                    
                    return scheme + host + port + path + query + fragment;
                }
                return url;
            } catch (Exception e) {
                return url;
            }
        }
    }
}
