package webapp.newcloud.lottery.movie.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteParser {
    public static List<SiteInfo> parseSitesFromJson(String jsonText) {
        List<Map<String, String>> rawSites = JsonUtil.parseSitesFromJson(jsonText);
        if (rawSites == null || rawSites.isEmpty()) return null;

        List<SiteInfo> sites = new ArrayList<>();
        for (Map<String, String> raw : rawSites) {
            SiteInfo info = new SiteInfo();
            info.key = raw.getOrDefault("key", "");
            info.name = raw.getOrDefault("name", "");
            info.type = parseInt(raw.get("type"), 0);
            info.api = raw.getOrDefault("api", "");
            info.searchable = parseInt(raw.get("searchable"), 0);
            info.quickSearch = parseInt(raw.get("quickSearch"), 0);
            info.filterable = parseInt(raw.get("filterable"), 0);
            info.ext = raw.getOrDefault("ext", "{}");
            sites.add(info);
        }
        return sites;
    }

    private static int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
