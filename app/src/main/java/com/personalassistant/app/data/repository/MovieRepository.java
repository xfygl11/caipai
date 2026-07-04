package com.personalassistant.app.data.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.personalassistant.app.data.model.CategoryInfo;
import com.personalassistant.app.data.model.Episode;
import com.personalassistant.app.data.model.MovieItem;
import com.personalassistant.app.data.model.PlayLine;
import com.personalassistant.app.data.model.SiteInfo;
import com.personalassistant.app.data.network.OkHttpUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovieRepository {
    private static final String TAG = "MovieRepository";
    private static final Gson gson = new Gson();

    public static List<CategoryInfo> loadCategories(String baseUrl) {
        String url = baseUrl + "?ac=list";
        String response = OkHttpUtil.get(url);
        if (response == null) return new ArrayList<>();

        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            JsonArray classes = root.getAsJsonArray("class");
            if (classes == null) return new ArrayList<>();

            List<CategoryInfo> result = new ArrayList<>();
            for (JsonElement el : classes) {
                JsonObject obj = el.getAsJsonObject();
                CategoryInfo info = new CategoryInfo();
                info.typeId = obj.has("type_id") ? obj.get("type_id").getAsString() : "";
                info.typeName = obj.has("type_name") ? obj.get("type_name").getAsString() : "";
                info.typePid = obj.has("type_pid") ? obj.get("type_pid").getAsInt() : 0;
                result.add(info);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "loadCategories error", e);
            return new ArrayList<>();
        }
    }

    public static List<MovieItem> loadMovies(String baseUrl, String typeId, int page) {
        String url = baseUrl + "?ac=detail&t=" + typeId + "&pg=" + page;
        String response = OkHttpUtil.get(url);
        if (response == null) return new ArrayList<>();

        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            JsonArray list = root.getAsJsonArray("list");
            if (list == null) return new ArrayList<>();

            List<MovieItem> result = new ArrayList<>();
            for (JsonElement el : list) {
                MovieItem item = parseMovieItem(el.getAsJsonObject());
                if (item != null) result.add(item);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "loadMovies error", e);
            return new ArrayList<>();
        }
    }

    public static List<MovieItem> searchMovies(String baseUrl, String keyword, int page) {
        String encodedKeyword;
        try { encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8"); }
        catch (Exception e) { encodedKeyword = keyword; }
        String url = baseUrl + "?ac=detail&wd=" + encodedKeyword + "&pg=" + page;
        String response = OkHttpUtil.get(url);
        if (response == null) return new ArrayList<>();

        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            JsonArray list = root.getAsJsonArray("list");
            if (list == null) return new ArrayList<>();

            List<MovieItem> result = new ArrayList<>();
            for (JsonElement el : list) {
                MovieItem item = parseMovieItem(el.getAsJsonObject());
                if (item != null) result.add(item);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "searchMovies error", e);
            return new ArrayList<>();
        }
    }

    public static MovieItem getMovieById(String baseUrl, String vodId) {
        String url = baseUrl + "?ac=detail&ids=" + vodId;
        String response = OkHttpUtil.get(url);
        if (response == null) return null;

        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            JsonArray list = root.getAsJsonArray("list");
            if (list == null || list.size() == 0) return null;

            return parseMovieItem(list.get(0).getAsJsonObject());
        } catch (Exception e) {
            Log.e(TAG, "getMovieById error", e);
            return null;
        }
    }

    private static MovieItem parseMovieItem(JsonObject obj) {
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
        item.playFrom = jsonStr(obj, "vod_play_from");
        item.playUrl = jsonStr(obj, "vod_play_url");
        item.vod_time = jsonStr(obj, "vod_time");
        item.type_name = jsonStr(obj, "type_name");
        item.vod_pic_thumb = jsonStr(obj, "vod_pic_thumb");
        item.vod_pic_slide = jsonStr(obj, "vod_pic_slide");
        item.vod_sub = jsonStr(obj, "vod_sub");
        item.vod_en = jsonStr(obj, "vod_en");
        item.vod_class = jsonStr(obj, "vod_class");
        item.vod_status = jsonStr(obj, "vod_status");
        item.vod_lock = jsonStr(obj, "vod_lock");
        item.vod_level = jsonStr(obj, "vod_level");
        item.vod_duration = jsonStr(obj, "vod_duration");

        if (item.playUrl != null && !item.playUrl.isEmpty()) {
            item.playLines = parsePlayLines(item.playFrom, item.playUrl);
        }
        return item;
    }

    public static List<PlayLine> parsePlayLines(String playFrom, String playUrl) {
        List<PlayLine> lines = new ArrayList<>();
        if (playFrom == null || playUrl == null) return lines;

        String[] froms = playFrom.split("\\$\\$\\$");
        String[] urls = playUrl.split("\\$\\$\\$");

        for (int i = 0; i < froms.length && i < urls.length; i++) {
            PlayLine line = new PlayLine();
            line.lineName = froms[i].trim();
            line.episodes = parseEpisodes(urls[i]);
            lines.add(line);
        }
        return lines;
    }

    private static List<Episode> parseEpisodes(String playUrl) {
        List<Episode> episodes = new ArrayList<>();
        if (playUrl == null || playUrl.isEmpty()) return episodes;

        String[] parts = playUrl.split("#");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String[] kv = part.split("\\$", 2);
            if (kv.length >= 2) {
                Episode ep = new Episode();
                ep.name = kv[0].trim();
                ep.url = kv[1].trim();
                episodes.add(ep);
            }
        }
        return episodes;
    }

    public static String resolveVideoUrl(String baseUrl, String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return null;

        if (videoUrl.startsWith("http")) {
            return videoUrl;
        }

        try {
            return baseUrl + videoUrl;
        } catch (Exception e) {
            return videoUrl;
        }
    }

    public static String resolveM3u8Url(String baseUrl, String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return null;

        if (videoUrl.startsWith("http")) {
            return videoUrl;
        }

        if (videoUrl.startsWith("//")) {
            return "https:" + videoUrl;
        }

        try {
            return baseUrl + videoUrl;
        } catch (Exception e) {
            return videoUrl;
        }
    }

    public static String parseHtmlVideoUrl(String html) {
        if (html == null || html.isEmpty()) return null;

        Pattern pattern = Pattern.compile("(https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }

        pattern = Pattern.compile("var\\s+url\\s*=\\s*['\"]([^'\"]+)['\"]");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }

        pattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            try {
                return URLDecoder.decode(matcher.group(1), "UTF-8");
            } catch (Exception e) {
                return matcher.group(1);
            }
        }

        return null;
    }

    public static String parseHtmlVideoUrlFromJsoup(String html) {
        if (html == null || html.isEmpty()) return null;

        try {
            Document doc = Jsoup.parse(html);
            Element iframe = doc.selectFirst("iframe[src]");
            if (iframe != null) {
                return iframe.attr("src");
            }

            Element video = doc.selectFirst("video source[src]");
            if (video != null) {
                return video.attr("src");
            }

            Element script = doc.selectFirst("script[type=\"application/ld+json\"]");
            if (script != null) {
                String content = script.text();
                if (content.contains("\"contentUrl\"")) {
                    Pattern pattern = Pattern.compile("\"contentUrl\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseHtmlVideoUrlFromJsoup error", e);
        }
        return null;
    }

    private static String jsonStr(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }
}
