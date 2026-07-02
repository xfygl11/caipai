package com.personalassistant.app.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {
    private static final String TAG = "HttpUtil";

    public static String get(String urlString) {
        return get(urlString, 15000);
    }

    public static String get(String urlString, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error: " + code);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "HTTP get error", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static String getSync(String urlString, int timeoutMs) {
        return get(urlString, timeoutMs);
    }
}
