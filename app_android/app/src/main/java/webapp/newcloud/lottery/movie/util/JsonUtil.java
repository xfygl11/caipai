package webapp.newcloud.lottery.movie.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static JsonObject parseObject(String json) {
        try {
            return gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonArray parseArray(String json) {
        try {
            return gson.fromJson(json, JsonArray.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(JsonObject json, String fieldName, Class<T> clazz) {
        if (json == null || !json.has(fieldName)) return null;
        return gson.fromJson(json.get(fieldName), clazz);
    }

    public static List<Map<String, String>> parseSitesFromJson(String jsonText) {
        List<Map<String, String>> sites = new ArrayList<>();
        try {
            JsonObject root = parseObject(jsonText);
            if (root == null) return sites;

            JsonArray sitesArray = null;
            if (root.has("sites") && root.get("sites").isJsonArray()) {
                sitesArray = root.getAsJsonArray("sites");
            } else if (root.has("code") && root.get("code").getAsInt() == 1 && root.has("list") && root.get("list").isJsonArray()) {
                sitesArray = root.getAsJsonArray("list");
                for (int i = 0; i < sitesArray.size(); i++) {
                    Map<String, String> site = new HashMap<>();
                    JsonObject s = sitesArray.get(i).getAsJsonObject();
                    site.put("name", s.has("vod_name") ? s.get("vod_name").getAsString() : "");
                    site.put("api", s.has("vod_id") ? s.get("vod_id").getAsString() : "");
                    site.put("type", "0");
                    sites.add(site);
                }
                return sites;
            }

            if (sitesArray != null) {
                for (int i = 0; i < sitesArray.size(); i++) {
                    Map<String, String> site = new HashMap<>();
                    JsonObject s = sitesArray.get(i).getAsJsonObject();
                    site.put("key", s.has("key") ? s.get("key").getAsString() : "");
                    site.put("name", s.has("name") ? s.get("name").getAsString() : "");
                    site.put("type", s.has("type") ? String.valueOf(s.get("type").getAsInt()) : "0");
                    site.put("api", s.has("api") ? s.get("api").getAsString() : "");
                    site.put("searchable", s.has("searchable") ? String.valueOf(s.get("searchable").getAsInt()) : "0");
                    site.put("quickSearch", s.has("quickSearch") ? String.valueOf(s.get("quickSearch").getAsInt()) : "0");
                    site.put("filterable", s.has("filterable") ? String.valueOf(s.get("filterable").getAsInt()) : "0");
                    site.put("ext", s.has("ext") ? s.get("ext").isJsonPrimitive() ? s.get("ext").getAsString() : s.get("ext").toString() : "{}");
                    sites.add(site);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sites;
    }
}
