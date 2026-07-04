package com.personalassistant.app.movie;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MovieApi {
    private static String BASE_URL = "https://bfzyapi.com/api.php/provide/vod/";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    public static void setBaseUrl(String url) {
        BASE_URL = url;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static List<MovieItem> getMovieList(String category, String page) throws IOException {
        String url = BASE_URL + "?ac=detail&t=" + category + "&pg=" + (page != null ? page : "1");
        JsonObject json = fetchJson(url);
        List<MovieItem> items = new ArrayList<>();
        if (json == null || !json.has("list")) return items;

        JsonArray list = json.getAsJsonArray("list");
        for (JsonElement e : list) {
            JsonObject obj = e.getAsJsonObject();
            MovieItem item = new MovieItem();
            item.vodId = jsonStr(obj, "vod_id");
            item.title = jsonStr(obj, "vod_name");
            item.pic = jsonStr(obj, "vod_pic");
            item.tag = jsonStr(obj, "vod_remarks");
            item.type = jsonStr(obj, "type_name");
            item.year = jsonStr(obj, "vod_year");
            item.area = jsonStr(obj, "vod_area");
            item.actor = jsonStr(obj, "vod_actor");
            item.director = jsonStr(obj, "vod_director");
            item.desc = jsonStr(obj, "vod_content");
            item.score = jsonStr(obj, "vod_score");
            items.add(item);
        }
        return items;
    }

    public static List<MovieItem> searchMovies(String keyword, String page) throws IOException {
        String url = BASE_URL + "?ac=detail&wd=" + keyword + "&pg=" + (page != null ? page : "1");
        return getMovieList(keyword, page);
    }

    public static MovieDetail getMovieDetail(String vodId) throws IOException {
        String url = BASE_URL + "?ac=videolist&ids=" + vodId;
        JsonObject json = fetchJson(url);
        if (json == null || !json.has("list")) return null;

        JsonArray list = json.getAsJsonArray("list");
        if (list.size() == 0) return null;

        JsonObject obj = list.get(0).getAsJsonObject();
        MovieDetail detail = new MovieDetail();
        detail.vodId = jsonStr(obj, "vod_id");
        detail.title = jsonStr(obj, "vod_name");
        detail.pic = jsonStr(obj, "vod_pic");
        detail.type = jsonStr(obj, "type_name");
        detail.year = jsonStr(obj, "vod_year");
        detail.area = jsonStr(obj, "vod_area");
        detail.actor = jsonStr(obj, "vod_actor");
        detail.director = jsonStr(obj, "vod_director");
        detail.desc = jsonStr(obj, "vod_content");
        detail.score = jsonStr(obj, "vod_score");

        String playUrl = jsonStr(obj, "vod_play_url");
        if (playUrl != null && !playUrl.isEmpty()) {
            detail.episodes = parseEpisodes(playUrl);
        }

        return detail;
    }

    private static List<Episode> parseEpisodes(String playUrl) {
        List<Episode> episodes = new ArrayList<>();
        if (playUrl == null || playUrl.isEmpty()) return episodes;

        String[] sources = playUrl.split("\\$\\$\\$");
        for (String sourceBlock : sources) {
            if (sourceBlock.isEmpty()) continue;
            String[] parts = sourceBlock.split("#");
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) continue;
                String[] kv = part.split("\\$");
                if (kv.length >= 2) {
                    Episode ep = new Episode();
                    ep.title = kv[0];
                    ep.url = kv[1];
                    episodes.add(ep);
                }
            }
        }
        return episodes;
    }

    private static JsonObject fetchJson(String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            String body = response.body().string();
            return gson.fromJson(body, JsonObject.class);
        }
    }

    private static String jsonStr(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }

    public static class MovieItem {
        public String vodId;
        public String title;
        public String pic;
        public String tag;
        public String type;
        public String year;
        public String area;
        public String actor;
        public String director;
        public String desc;
        public String score;
    }

    public static class MovieDetail {
        public String vodId;
        public String title;
        public String pic;
        public String type;
        public String year;
        public String area;
        public String actor;
        public String director;
        public String desc;
        public String score;
        public List<Episode> episodes = new ArrayList<>();
    }

    public static class Episode {
        public String title;
        public String url;
    }
}
