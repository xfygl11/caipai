package com.personalassistant.app.data.network;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpUtil {
    private static final String TAG = "OkHttpUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    private static final Headers DEFAULT_HEADERS = new Headers.Builder()
            .add("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .build();

    public static String get(String url) {
        return get(url, DEFAULT_HEADERS, 15000);
    }

    public static String get(String url, Headers headers, int timeoutMs) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();
        if (headers != null) {
            builder.headers(headers);
        }
        return executeGet(builder.build(), timeoutMs);
    }

    public static String post(String url, String body) {
        return post(url, body, DEFAULT_HEADERS, 15000);
    }

    public static String post(String url, String body, Headers headers, int timeoutMs) {
        RequestBody rb = RequestBody.create(body, okhttp3.MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(rb)
                .headers(headers != null ? headers : DEFAULT_HEADERS)
                .build();
        return executeRequest(request, timeoutMs);
    }

    public static String postForm(String url, String key, String value) {
        FormBody formBody = new FormBody.Builder()
                .add(key, value)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .headers(DEFAULT_HEADERS)
                .build();
        return executeRequest(request, 15000);
    }

    public static String head(String url, int timeoutMs) {
        Request request = new Request.Builder()
                .url(url)
                .head()
                .headers(DEFAULT_HEADERS)
                .build();
        return executeHead(request, timeoutMs);
    }

    private static String executeGet(Request request, int timeoutMs) {
        return executeRequest(request, timeoutMs);
    }

    private static String executeRequest(Request request, int timeoutMs) {
        Request.Builder builder = request.newBuilder();
        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "HTTP error: " + response.code() + " for " + request.url());
                return null;
            }
            return response.body().string();
        } catch (IOException e) {
            Log.e(TAG, "HTTP error for " + request.url(), e);
            return null;
        }
    }

    private static String executeHead(Request request, int timeoutMs) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HEAD error: " + response.code() + " for " + request.url());
                return null;
            }
            return response.header("Content-Type", "");
        } catch (IOException e) {
            Log.e(TAG, "HEAD error for " + request.url(), e);
            return null;
        }
    }
}
