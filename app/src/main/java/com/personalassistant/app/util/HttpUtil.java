package com.personalassistant.app.util;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpUtil {
    private static final String TAG = "HttpUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .eventListener(new EventListener() {})
        .build();

    public static String get(String urlString) {
        return get(urlString, 15000);
    }

    public static String get(String urlString, int timeoutMs) {
        Request request = new Request.Builder()
            .url(urlString)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "*/*")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP error: " + response.code() + " for " + urlString);
                return null;
            }
            ResponseBody body = response.body();
            if (body == null) {
                Log.w(TAG, "Empty response for " + urlString);
                return null;
            }
            String content = body.string();
            if (isValidJson(content)) {
                return content;
            }
            Log.w(TAG, "Response is not valid JSON for " + urlString + ", length=" + content.length() + ", first100=" + content.substring(0, Math.min(100, content.length())));
            return null;
        } catch (IOException e) {
            Log.e(TAG, "HTTP get error for " + urlString, e);
            return null;
        }
    }

    private static boolean isValidJson(String content) {
        if (content == null || content.isEmpty()) return false;
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
            || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    public static String getSync(String urlString, int timeoutMs) {
        return get(urlString, timeoutMs);
    }
}

