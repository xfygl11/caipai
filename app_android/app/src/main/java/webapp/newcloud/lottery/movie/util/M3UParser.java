package webapp.newcloud.lottery.movie.util;

import java.util.ArrayList;
import java.util.List;

public class M3UParser {
    public static List<ChannelInfo> parseM3U(String text) {
        List<ChannelInfo> channels = new ArrayList<>();
        String[] lines = text.split("\r?\n");
        String currentGroup = "";
        ChannelInfo currentItem = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTGRP:")) {
                currentGroup = line.replace("#EXTGRP:", "").trim();
                continue;
            }
            if (line.startsWith("#genre#")) {
                currentGroup = line.replace("#genre#", "").trim();
                continue;
            }
            if (line.startsWith("#EXTINF:")) {
                String match = line.replaceAll("#EXTINF:-?\\d+(?:\\s*,\\s*([^,]*))?", "$1");
                String name = match.isEmpty() ? line.replace("#EXTINF:", "").replaceFirst("^-?\\d+,\\s*", "") : match;
                currentItem = new ChannelInfo();
                currentItem.name = name.replaceAll("[\"']", "");
                currentItem.group = currentGroup.isEmpty() ? "直播" : currentGroup;
                continue;
            }
            if (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("rtmp://") || line.startsWith("rtsp://")) {
                if (currentItem != null) {
                    currentItem.url = line;
                    channels.add(currentItem);
                    currentItem = null;
                }
            }
        }
        return channels;
    }

    public static List<ChannelInfo> parseTXT(String text) {
        List<ChannelInfo> channels = new ArrayList<>();
        String[] lines = text.split("\r?\n");
        String currentGroup = "";

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#genre#")) {
                currentGroup = line.replace("#genre#", "").trim();
                continue;
            }
            if (line.startsWith("#")) continue;
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                ChannelInfo info = new ChannelInfo();
                info.name = parts[0].trim();
                info.url = parts[1].trim();
                info.group = currentGroup.isEmpty() ? "直播" : currentGroup;
                channels.add(info);
            }
        }
        return channels;
    }

    public static List<ChannelInfo> parseLiveSource(String text) {
        if (text.contains("#EXTINF")) return parseM3U(text);
        if (text.contains("#genre#")) return parseTXT(text);
        return parseTXT(text);
    }

    public static class ChannelInfo {
        public String name;
        public String url;
        public String group;
        public String logo;
    }
}
