package webapp.newcloud.lottery.movie.util;

import android.util.Log;

import java.io.IOException;
import java.net.IDN;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpClient {
    private static final String TAG = "HttpClient";
    private static final int MAX_REDIRECTS = 5;
    private static final int TIMEOUT_MS = 15000;

    private final OkHttpClient okHttpClient;
    private final Map<String, String> cookieStore = new ConcurrentHashMap<>();

    public HttpClient() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .eventListener(new EventListener() {})
                .build();
    }

    public String httpGet(String urlStr) throws IOException {
        return httpGetWithHeaders(urlStr, "", new HashMap<>());
    }

    public String httpGetWithHeaders(String urlStr, String authHeader) throws IOException {
        return httpGetWithHeaders(urlStr, authHeader, new HashMap<>());
    }

    public String httpPost(String urlStr, String body) throws IOException {
        return httpPostWithHeaders(urlStr, body, new HashMap<>());
    }

    public String httpGetWithHeaders(String urlStr, String authHeader, Map<String, String> extraHeaders) throws IOException {
        return doRequest(urlStr, "GET", null, authHeader, extraHeaders);
    }

    public String httpPostWithHeaders(String urlStr, String body, Map<String, String> extraHeaders) throws IOException {
        return doRequest(urlStr, "POST", body, "", extraHeaders);
    }

    /**
     * Fetch raw bytes from URL (for FanTaiYing image-embedded data).
     */
    public byte[] httpGetRaw(String urlStr) throws IOException {
        String encodedUrl = encodeUrl(urlStr);
        int redirectCount = 0;

        while (redirectCount <= MAX_REDIRECTS) {
            Request.Builder requestBuilder = new Request.Builder().url(encodedUrl);
            requestBuilder.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36");
            requestBuilder.addHeader("Accept", "*/*");

            Call call = okHttpClient.newCall(requestBuilder.build());
            Response response = call.execute();

            int statusCode = response.code();
            if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) {
                String location = response.header("Location");
                response.close();
                if (location == null || location.isEmpty()) {
                    throw new IOException("Redirect without Location header");
                }
                encodedUrl = resolveRedirectUrl(urlStr, location);
                redirectCount++;
                continue;
            }

            if (statusCode != 200) {
                response.close();
                throw new IOException("HTTP " + statusCode);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                response.close();
                throw new IOException("Empty response body");
            }

            byte[] data = responseBody.bytes();
            response.close();
            return data;
        }

        throw new IOException("Too many redirects");
    }

    private String doRequest(String urlStr, String method, String body, String authHeader, Map<String, String> extraHeaders) throws IOException {
        String encodedUrl = encodeUrl(urlStr);
        int redirectCount = 0;

        while (redirectCount <= MAX_REDIRECTS) {
            Request.Builder requestBuilder = new Request.Builder().url(encodedUrl);

            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36");
            headers.put("Accept", "*/*");
            if (authHeader != null && !authHeader.isEmpty()) {
                headers.put("Authorization", authHeader);
            }
            headers.putAll(extraHeaders);

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            String host = extractHost(encodedUrl);
            String cookie = cookieStore.get(host);
            if (cookie != null && !cookie.isEmpty()) {
                requestBuilder.addHeader("Cookie", cookie);
            }

            if ("POST".equalsIgnoreCase(method) && body != null) {
                requestBuilder.post(okhttp3.RequestBody.create(
                        body.getBytes(Charset.forName("UTF-8")),
                        okhttp3.MediaType.parse("text/plain;charset=utf-8")));
            }

            Call call = okHttpClient.newCall(requestBuilder.build());
            Response response = call.execute();

            String setCookie = response.header("Set-Cookie");
            if (setCookie != null && !setCookie.isEmpty()) {
                int semiIdx = setCookie.indexOf(';');
                String cookieVal = semiIdx > 0 ? setCookie.substring(0, semiIdx) : setCookie;
                cookieStore.put(host, cookieVal);
            }

            int statusCode = response.code();
            if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) {
                String location = response.header("Location");
                response.close();
                if (location == null || location.isEmpty()) {
                    throw new IOException("Redirect without Location header");
                }
                encodedUrl = resolveRedirectUrl(urlStr, location);
                redirectCount++;
                continue;
            }

            if (statusCode != 200) {
                response.close();
                throw new IOException("HTTP " + statusCode);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response body");
            }

            String contentType = responseBody.contentType() != null ? responseBody.contentType().toString() : "";
            byte[] data = responseBody.bytes();

            if (contentType.contains("image") || contentType.contains("octet-stream")) {
                return new String(data, Charset.forName("ISO-8859-1"));
            }

            if (contentType.contains("charset")) {
                String[] parts = contentType.split("=");
                if (parts.length >= 2) {
                    String charset = parts[1].trim();
                    try {
                        return new String(data, Charset.forName(charset));
                    } catch (Exception e) {
                        return new String(data, Charset.forName("UTF-8"));
                    }
                }
            }

            return new String(data, Charset.forName("UTF-8"));
        }

        throw new IOException("Too many redirects");
    }

    private String extractHost(String urlStr) {
        try {
            URL url = new URL(urlStr);
            return url.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveRedirectUrl(String baseUrl, String location) {
        if (location.startsWith("//")) {
            return "https:" + location;
        } else if (location.startsWith("/")) {
            try {
                URL base = new URL(baseUrl);
                return base.getProtocol() + "://" + base.getHost()
                        + (base.getPort() != -1 ? ":" + base.getPort() : "") + location;
            } catch (Exception e) {
                return location;
            }
        } else if (!location.startsWith("http")) {
            try {
                URL base = new URL(baseUrl);
                String basePath = base.getPath();
                if (basePath != null && basePath.contains("/")) {
                    basePath = basePath.substring(0, basePath.lastIndexOf('/') + 1);
                } else {
                    basePath = "/";
                }
                return base.getProtocol() + "://" + base.getHost()
                        + (base.getPort() != -1 ? ":" + base.getPort() : "") + basePath + location;
            } catch (Exception e) {
                return location;
            }
        }
        return location;
    }

    private String encodeUrl(String url) {
        try {
            URL parsed = new URL(url);
            String host = parsed.getHost();
            if (host != null && host.matches(".*[\u4e00-\u9fa5].*")) {
                host = IDN.toASCII(host);
            }
            return parsed.getProtocol() + "://" + host
                    + (parsed.getPort() != -1 ? ":" + parsed.getPort() : "")
                    + parsed.getPath()
                    + (parsed.getQuery() != null ? "?" + parsed.getQuery() : "");
        } catch (Exception e) {
            Log.e(TAG, "encodeUrl error: " + e.getMessage());
            return url;
        }
    }
}
