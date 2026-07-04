package com.personalassistant.app.data.model;

import java.util.List;
import java.util.Map;

public class ParseConfig {
    public String name;
    public String url;
    public int type;
    public List<String> flags;
    public Map<String, String> headers;

    public static class Builder {
        private ParseConfig config = new ParseConfig();

        public Builder name(String name) { config.name = name; return this; }
        public Builder url(String url) { config.url = url; return this; }
        public Builder type(int type) { config.type = type; return this; }
        public Builder flags(List<String> flags) { config.flags = flags; return this; }
        public Builder headers(Map<String, String> headers) { config.headers = headers; return this; }

        public ParseConfig build() { return config; }
    }
}
