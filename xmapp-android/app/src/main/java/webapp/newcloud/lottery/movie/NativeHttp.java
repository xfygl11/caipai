package webapp.newcloud.lottery.movie;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeHttp {
    private static final String TAG = "NativeHttp";
    private static final int TIMEOUT_MS = 15000;
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static String httpGet(String urlString) {
        return httpGet(urlString, null);
    }

    public static String httpGet(String urlString, String userAgent) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("User-Agent", userAgent != null ? userAgent : "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36");
            connection.setRequestProperty("Connection", "close");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP error code: " + responseCode + " for " + urlString);
                return null;
            }

            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "httpGet failed: " + urlString, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public interface HttpCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void httpPostAsync(String urlString, String postData, HttpCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.getOutputStream().write(postData.getBytes(StandardCharsets.UTF_8));

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    final String errorMsg = "HTTP error: " + responseCode;
                    mainHandler.post(() -> callback.onError(errorMsg));
                    return;
                }

                StringBuilder result = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                }

                final String response = result.toString();
                mainHandler.post(() -> callback.onSuccess(response));
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                mainHandler.post(() -> callback.onError(errorMsg != null ? errorMsg : "Unknown error"));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static void shutdown() {
        executor.shutdown();
    }

    // 二进制安全下载，返回 byte[]
    public static byte[] httpGetBytes(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36");
            connection.setRequestProperty("Connection", "close");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP error code: " + responseCode + " for " + urlString);
                return null;
            }

            try (InputStream inputStream = connection.getInputStream();
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = inputStream.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                byte[] result = baos.toByteArray();
                Log.d(TAG, "httpGetBytes success: " + result.length + " bytes from " + urlString);
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "httpGetBytes failed: " + urlString, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
