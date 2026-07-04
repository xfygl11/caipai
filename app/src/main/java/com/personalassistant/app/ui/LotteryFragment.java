package com.personalassistant.app.ui;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebViewAssetLoader;

import com.personalassistant.app.bridge.AndroidJSBridge;

public class LotteryFragment extends Fragment {
    private static final String LOCAL_DOMAIN = "personalassistant.app";
    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private AndroidJSBridge jsBridge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        webView = new WebView(requireContext());
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return webView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        requireActivity();
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(requireContext()))
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(requireContext()))
                .setDomain(LOCAL_DOMAIN)
                .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        jsBridge = new AndroidJSBridge(requireContext());
        jsBridge.setWebView(webView);
        webView.addJavascriptInterface(jsBridge, "AndroidSync");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                try {
                    return assetLoader.shouldInterceptRequest(request.getUrl());
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectNativeHttp();
            }
        });

        loadContent();
    }

    private void injectNativeHttp() {
        String js = "(function(){if(!window.NativeHttp){window.NativeHttp={};" +
                "window.NativeHttp.httpGet=function(u){try{return AndroidSync.httpGet(u);}" +
                "catch(e){return'__ERROR__'+e.message;}};}})();";
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    private void loadContent() {
        webView.loadUrl("https://" + LOCAL_DOMAIN + "/www/lottery.html");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }
}
