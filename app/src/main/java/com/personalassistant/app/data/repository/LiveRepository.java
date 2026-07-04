package com.personalassistant.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.personalassistant.app.data.model.LiveChannel;
import com.personalassistant.app.data.model.LiveSource;
import com.personalassistant.app.data.network.OkHttpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiveRepository {
    private static final String TAG = "LiveRepository";
    private static final String PREFS_NAME = "live_prefs";
    private static final String KEY_LIVE_SOURCES = "live_sources";
    private static final Gson gson = new Gson();

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveLiveSources(Context context, List<LiveSource> sources) {
        try {
            String json = gson.toJson(sources);
            getPrefs(context).edit().putString(KEY_LIVE_SOURCES, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "saveLiveSources error", e);
        }
    }

    public static List<LiveSource> loadLiveSources(Context context) {
        try {
            String json = getPrefs(context).getString(KEY_LIVE_SOURCES, null);
            if (json == null) return new ArrayList<>();

            JsonArray array = gson.fromJson(json, JsonArray.class);
            List<LiveSource> result = new ArrayList<>();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                LiveSource source = new LiveSource();
                source.name = obj.has("name") ? obj.get("name").getAsString() : "";
                source.url = obj.has("url") ? obj.get("url").getAsString() : "";
                source.epg = obj.has("epg") ? obj.get("epg").getAsString() : null;
                source.logo = obj.has("logo") ? obj.get("logo").getAsString() : null;
                result.add(source);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "loadLiveSources error", e);
            return new ArrayList<>();
        }
    }

    public static void addLiveSource(Context context, LiveSource source) {
        List<LiveSource> sources = loadLiveSources(context);
        for (LiveSource existing : sources) {
            if (existing.name.equals(source.name)) {
                existing.url = source.url;
                existing.epg = source.epg;
                existing.logo = source.logo;
                saveLiveSources(context, sources);
                return;
            }
        }
        sources.add(source);
        saveLiveSources(context, sources);
    }

    public static void removeLiveSource(Context context, String name) {
        List<LiveSource> sources = loadLiveSources(context);
        sources.removeIf(s -> s.name.equals(name));
        saveLiveSources(context, sources);
    }

    public static String resolveLiveUrl(String baseUrl, String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return null;
        if (videoUrl.startsWith("http")) return videoUrl;
        if (videoUrl.startsWith("//")) return "https:" + videoUrl;
        try {
            return baseUrl + videoUrl;
        } catch (Exception e) {
            return videoUrl;
        }
    }

    public static String buildEpgUrl(String epgTemplate, String channelName) {
        if (epgTemplate == null || epgTemplate.isEmpty()) return null;
        return epgTemplate.replace("{name}", channelName).replace("{date}", "");
    }

    public static String buildLogoUrl(String logoTemplate, String channelName) {
        if (logoTemplate == null || logoTemplate.isEmpty()) return null;
        return logoTemplate.replace("{name}", channelName);
    }

    public static List<List<LiveChannel>> groupChannelsByGroup(List<LiveChannel> channels) {
        Map<String, List<LiveChannel>> grouped = new HashMap<>();
        for (LiveChannel ch : channels) {
            String group = ch.group != null && !ch.group.isEmpty() ? ch.group : "其他";
            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(ch);
        }
        return new ArrayList<>(grouped.values());
    }
}
