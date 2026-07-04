package com.personalassistant.app.data.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.personalassistant.app.data.model.CategoryInfo;
import com.personalassistant.app.data.model.DohConfig;
import com.personalassistant.app.data.model.LiveChannel;
import com.personalassistant.app.data.model.LiveSource;
import com.personalassistant.app.data.model.ParseConfig;
import com.personalassistant.app.data.model.RuleConfig;
import com.personalassistant.app.data.model.SiteInfo;
import com.personalassistant.app.data.model.RepoConfig;
import com.personalassistant.app.data.network.OkHttpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteRepository {
    private static final String TAG = "SiteRepository";
    private static final Gson gson = new Gson();

    public static RepoConfig loadRepoConfig(String repoUrl) {
        String response = OkHttpUtil.get(repoUrl);
        if (response == null) return null;

        try {
            JsonObject root = gson.fromJson(response, JsonObject.class);
            RepoConfig config = new RepoConfig();
            config.spider = root.has("spider") ? root.get("spider").getAsString() : null;
            config.logo = root.has("logo") ? root.get("logo").getAsString() : null;
            config.wallpaper = root.has("wallpaper") ? root.get("wallpaper").getAsString() : null;
            config.lives = parseLives(root);
            config.sites = parseSites(root);
            config.parses = parseParses(root);
            config.flags = parseFlags(root);
            config.doh = parseDoh(root);
            config.rules = parseRules(root);
            return config;
        } catch (Exception e) {
            Log.e(TAG, "loadRepoConfig error", e);
            return null;
        }
    }

    private static List<LiveSource> parseLives(JsonObject root) {
        List<LiveSource> result = new ArrayList<>();
        JsonArray jsonLives = root.getAsJsonArray("lives");
        if (jsonLives == null) return result;

        for (JsonElement el : jsonLives) {
            JsonObject live = el.getAsJsonObject();
            LiveSource info = new LiveSource();
            info.name = live.has("name") ? live.get("name").getAsString() : "";
            info.url = live.has("url") ? live.get("url").getAsString() : "";
            info.epg = live.has("epg") ? live.get("epg").getAsString() : null;
            info.logo = live.has("logo") ? live.get("logo").getAsString() : null;
            result.add(info);
        }
        return result;
    }

    private static List<SiteInfo> parseSites(JsonObject root) {
        List<SiteInfo> result = new ArrayList<>();
        JsonArray jsonSites = root.getAsJsonArray("sites");
        if (jsonSites == null) return result;

        for (JsonElement el : jsonSites) {
            JsonObject site = el.getAsJsonObject();
            String name = site.has("name") ? site.get("name").getAsString() : "";
            String api = site.has("api") ? site.get("api").getAsString() : "";
            int type = site.has("type") ? site.get("type").getAsInt() : -1;
            String ext = site.has("ext") ? site.get("ext").toString() : null;
            String jar = site.has("jar") ? site.get("jar").getAsString() : null;
            int searchable = site.has("searchable") ? site.get("searchable").getAsInt() : 1;
            String category = site.has("category") ? site.get("category").getAsString() : "";

            if (name.isEmpty() || api.isEmpty()) continue;

            SiteInfo info = new SiteInfo();
            info.name = name;
            info.api = api;
            info.type = type;
            info.ext = ext;
            info.jar = jar;
            info.searchable = searchable;
            info.category = category;

            // Determine type label
            if (type == 0 || type == 1) {
                info.typeLabel = "MV";
                info.isLive = false;
            } else if (type == 3) {
                if (api.startsWith("csp_")) {
                    info.typeLabel = "PLUGIN";
                    info.isLive = isPluginLive(api);
                } else if (api.startsWith("http") && (api.endsWith(".js") || api.contains("drpy"))) {
                    info.typeLabel = "DRPY";
                    info.isLive = false;
                } else {
                    info.typeLabel = "OTHER";
                    info.isLive = false;
                }
            } else {
                info.typeLabel = "UNKNOWN";
                info.isLive = false;
            }

            // Check if it's a live source site
            if (info.isLive) {
                info.isLive = true;
                info.typeLabel = "LIVE";
            }

            result.add(info);
        }
        return result;
    }

    private static boolean isPluginLive(String api) {
        String name = api.toLowerCase();
        return name.contains("live") || name.contains("alllive") ||
               name.contains("kanqiu") || name.contains("live");
    }

    private static List<ParseConfig> parseParses(JsonObject root) {
        List<ParseConfig> result = new ArrayList<>();
        JsonArray jsonParses = root.getAsJsonArray("parses");
        if (jsonParses == null) return result;

        for (JsonElement el : jsonParses) {
            JsonObject parseObj = el.getAsJsonObject();
            ParseConfig cfg = new ParseConfig();
            cfg.name = parseObj.has("name") ? parseObj.get("name").getAsString() : "";
            cfg.url = parseObj.has("url") ? parseObj.get("url").getAsString() : "";
            cfg.type = parseObj.has("type") ? parseObj.get("type").getAsInt() : 0;

            JsonObject ext = parseObj.has("ext") ? parseObj.getAsJsonObject("ext") : null;
            if (ext != null) {
                cfg.flags = new ArrayList<>();
                cfg.headers = new HashMap<>();

                if (ext.has("flag") && ext.get("flag").isJsonArray()) {
                    JsonArray flags = ext.getAsJsonArray("flag");
                    for (JsonElement f : flags) {
                        cfg.flags.add(f.getAsString());
                    }
                }
                if (ext.has("header") && ext.get("header").isJsonObject()) {
                    JsonObject headers = ext.getAsJsonObject("header");
                    for (Map.Entry<String, JsonElement> entry : headers.entrySet()) {
                        cfg.headers.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }

            result.add(cfg);
        }
        return result;
    }

    private static List<String> parseFlags(JsonObject root) {
        List<String> result = new ArrayList<>();
        JsonArray jsonFlags = root.getAsJsonArray("flags");
        if (jsonFlags == null) return result;

        for (JsonElement el : jsonFlags) {
            result.add(el.getAsString());
        }
        return result;
    }

    private static List<DohConfig> parseDoh(JsonObject root) {
        List<DohConfig> result = new ArrayList<>();
        JsonArray jsonDoh = root.getAsJsonArray("doh");
        if (jsonDoh == null) return result;

        for (JsonElement el : jsonDoh) {
            JsonObject doh = el.getAsJsonObject();
            DohConfig config = new DohConfig();
            config.name = doh.has("name") ? doh.get("name").getAsString() : "";
            config.url = doh.has("url") ? doh.get("url").getAsString() : "";
            result.add(config);
        }
        return result;
    }

    private static List<RuleConfig> parseRules(JsonObject root) {
        List<RuleConfig> result = new ArrayList<>();
        JsonArray jsonRules = root.getAsJsonArray("rules");
        if (jsonRules == null) return result;

        for (JsonElement el : jsonRules) {
            JsonObject rule = el.getAsJsonObject();
            RuleConfig config = new RuleConfig();
            config.name = rule.has("name") ? rule.get("name").getAsString() : "";

            if (rule.has("hosts") && rule.get("hosts").isJsonArray()) {
                config.hosts = new ArrayList<>();
                for (JsonElement h : rule.getAsJsonArray("hosts")) {
                    config.hosts.add(h.getAsString());
                }
            }

            if (rule.has("regex") && rule.get("regex").isJsonArray()) {
                config.regex = new ArrayList<>();
                for (JsonElement r : rule.getAsJsonArray("regex")) {
                    config.regex.add(r.getAsString());
                }
            }

            result.add(config);
        }
        return result;
    }

    public static List<SiteInfo> extractMovieSites(RepoConfig config) {
        List<SiteInfo> result = new ArrayList<>();
        if (config == null || config.sites == null) return result;

        for (SiteInfo site : config.sites) {
            if (site.typeLabel.equals("MV")) {
                result.add(site);
            }
        }
        return result;
    }

    public static List<LiveSource> extractLiveSources(RepoConfig config) {
        List<LiveSource> result = new ArrayList<>();
        if (config == null || config.lives == null) return result;

        for (LiveSource live : config.lives) {
            if (!live.url.isEmpty()) {
                result.add(live);
            }
        }
        return result;
    }

    public static List<ParseConfig> extractParses(RepoConfig config) {
        List<ParseConfig> result = new ArrayList<>();
        if (config == null || config.parses == null) return result;

        for (ParseConfig parse : config.parses) {
            if (!parse.url.isEmpty() && !"Demo".equalsIgnoreCase(parse.url)) {
                result.add(parse);
            }
        }
        return result;
    }

    public static List<String> extractFlags(RepoConfig config) {
        List<String> result = new ArrayList<>();
        if (config == null || config.flags == null) return result;
        return new ArrayList<>(config.flags);
    }

    public static List<RuleConfig> extractRules(RepoConfig config) {
        List<RuleConfig> result = new ArrayList<>();
        if (config == null || config.rules == null) return result;
        return new ArrayList<>(config.rules);
    }

    public static List<LiveChannel> parseM3u(String m3uText) {
        List<LiveChannel> channels = new ArrayList<>();
        Scanner scanner = new Scanner(m3uText);
        LiveChannel current = null;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("#EXTINF")) {
                current = new LiveChannel();
                current.name = extractExtinfName(line);
                current.logo = extractExtinfAttr(line, "tvg-logo");
                current.group = extractExtinfAttr(line, "group-title");
            } else if (line.startsWith("http") && current != null) {
                current.url = line;
                if (current.name != null && !current.name.isEmpty()) {
                    channels.add(current);
                }
                current = null;
            }
        }
        scanner.close();
        return channels;
    }

    public static List<LiveChannel> parseIptvTxt(String txt) {
        List<LiveChannel> channels = new ArrayList<>();
        String currentGroup = "默认";

        for (String line : txt.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.equals("#EXTM3U")) continue;
            if (line.contains("#genre#")) {
                currentGroup = line.replace("#genre#", "").trim();
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                LiveChannel ch = new LiveChannel();
                ch.name = parts[0].trim();
                ch.url = parts[1].trim();
                ch.group = currentGroup;
                channels.add(ch);
            }
        }
        return channels;
    }

    public static List<LiveChannel> parseLiveSource(String liveUrl, String liveText) {
        if (liveText == null || liveText.isEmpty()) return new ArrayList<>();

        if (liveText.startsWith("#EXTM3U")) {
            return parseM3u(liveText);
        } else if (liveText.contains("#genre#")) {
            return parseIptvTxt(liveText);
        }

        return new ArrayList<>();
    }

    private static String extractExtinfName(String line) {
        int commaIdx = line.lastIndexOf(",");
        if (commaIdx >= 0) {
            return line.substring(commaIdx + 1).trim();
        }
        return "";
    }

    private static String extractExtinfAttr(String line, String attr) {
        Pattern pattern = Pattern.compile(attr + "\"=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static boolean isM3uFormat(String text) {
        return text != null && text.startsWith("#EXTM3U");
    }

    public static boolean isIptvTxtFormat(String text) {
        return text != null && text.contains("#genre#");
    }

    public static boolean isTvBoxApi(String url) {
        return url != null && url.contains("/api.php/provide/vod");
    }

    public static boolean isLiveSourceUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.contains(".m3u8") || url.contains(".m3u") ||
               url.endsWith(".txt") || url.contains("/m3u/") ||
               url.contains("/live/") || url.contains("/tv/");
    }
}
