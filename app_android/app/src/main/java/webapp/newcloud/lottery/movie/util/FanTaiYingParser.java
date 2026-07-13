package webapp.newcloud.lottery.movie.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FanTaiYingParser {
    private static final String TAG = "FanTaiYingParser";
    private static final int MIN_PRINTABLE_SEQUENCE_LENGTH = 200;

    /**
     * Parse the raw bytes from FanTaiYing /tv endpoint.
     * The response is a JPEG image containing embedded Base64-encoded JSON.
     * Algorithm: extract longest printable ASCII sequence -> base64 decode -> parse JSON.
     */
    public static JSONObject parse(byte[] rawData) throws Exception {
        if (rawData == null || rawData.length == 0) {
            throw new Exception("Empty response data");
        }

        // Step 1: Convert binary data to text stream (replace non-printable with newlines)
        StringBuilder textStream = new StringBuilder(rawData.length);
        for (byte b : rawData) {
            if (b >= 32 && b <= 126) {
                textStream.append((char) b);
            } else {
                textStream.append('\n');
            }
        }

        // Step 2: Find the longest printable ASCII sequence (potential Base64)
        String longestSeq = "";
        StringBuilder currentSeq = new StringBuilder();
        for (int i = 0; i < textStream.length(); i++) {
            char c = textStream.charAt(i);
            if (c == '\n') {
                if (currentSeq.length() > longestSeq.length()) {
                    longestSeq = currentSeq.toString();
                }
                currentSeq.setLength(0);
            } else {
                currentSeq.append(c);
            }
        }
        // Don't forget the last sequence
        if (currentSeq.length() > longestSeq.length()) {
            longestSeq = currentSeq.toString();
        }

        if (longestSeq.length() < MIN_PRINTABLE_SEQUENCE_LENGTH) {
            throw new Exception("No valid Base64 data found in response (longest sequence: " + longestSeq.length() + ")");
        }

        Log.d(TAG, "Found Base64 sequence length: " + longestSeq.length());

        // Step 3: Base64 decode
        String cleanBase64 = longestSeq.trim().replaceAll("[^A-Za-z0-9+/=]", "");
        if (cleanBase64.length() % 4 != 0) {
            cleanBase64 = cleanBase64.substring(0, cleanBase64.length() - (cleanBase64.length() % 4));
        }
        byte[] decoded;
        try {
            decoded = java.util.Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid Base64 data: " + e.getMessage());
        }

        // Step 4: Try to parse as JSON directly (most common case)
        try {
            String jsonStr = new String(decoded, StandardCharsets.UTF_8);
            return new JSONObject(jsonStr);
        } catch (JSONException e) {
            Log.d(TAG, "Not direct UTF-8 JSON, trying gzip...");
        }

        // Step 5: Try gzip decompression
        try {
            InputStream is = new java.io.ByteArrayInputStream(decoded);
            GZIPInputStream gzis = new GZIPInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            gzis.close();
            String jsonStr = baos.toString("UTF-8");
            return new JSONObject(jsonStr);
        } catch (Exception e) {
            Log.w(TAG, "Neither direct JSON nor gzip decompression worked: " + e.getMessage());
            // Last resort: try treating decoded bytes as UTF-8 string directly
            String maybeJson = new String(decoded, StandardCharsets.UTF_8);
            // Find the first { and last } to extract JSON
            int start = maybeJson.indexOf('{');
            int end = maybeJson.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return new JSONObject(maybeJson.substring(start, end + 1));
            }
            throw new Exception("Unable to parse response as JSON. Decoded " + decoded.length + " bytes.");
        }
    }

    /**
     * Parse sites array from config.
     */
    public static List<SiteInfo> parseSites(JSONObject config) throws JSONException {
        List<SiteInfo> sites = new ArrayList<>();
        JSONArray sitesArray = config.optJSONArray("sites");
        if (sitesArray == null) {
            return sites;
        }
        for (int i = 0; i < sitesArray.length(); i++) {
            JSONObject siteObj = sitesArray.getJSONObject(i);
            SiteInfo info = new SiteInfo();
            info.key = siteObj.optString("key", "");
            info.name = siteObj.optString("name", "");
            info.type = siteObj.optInt("type", 0);
            info.api = siteObj.optString("api", "");
            info.searchable = siteObj.optInt("searchable", 0);
            info.quickSearch = siteObj.optInt("quickSearch", 0);
            info.filterable = siteObj.optInt("changeable", 0);
            
            // Parse ext field (can be string or object)
            Object extObj = siteObj.opt("ext");
            if (extObj instanceof JSONObject) {
                info.ext = extObj.toString();
            } else if (extObj != null) {
                info.ext = extObj.toString();
            }
            
            sites.add(info);
        }
        return sites;
    }

    /**
     * Get spider URL from config.
     */
    public static String getSpiderUrl(JSONObject config) {
        return config.optString("spider", "");
    }

    /**
     * Get logo URL from config.
     */
    public static String getLogoUrl(JSONObject config) {
        return config.optString("logo", "");
    }

    /**
     * Get wallpaper URL from config.
     */
    public static String getWallpaperUrl(JSONObject config) {
        return config.optString("wallpaper", "");
    }
}
