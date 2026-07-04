package com.personalassistant.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SiteInfo {
    public String name;
    public String api;
    public int type;
    public String typeLabel;
    public boolean isLive;
    public String ext;
    public String jar;
    public int searchable;
    public String category;
    public int status;

    public static class Builder {
        private SiteInfo info = new SiteInfo();

        public Builder name(String name) { info.name = name; return this; }
        public Builder api(String api) { info.api = api; return this; }
        public Builder type(int type) { info.type = type; return this; }
        public Builder typeLabel(String typeLabel) { info.typeLabel = typeLabel; return this; }
        public Builder isLive(boolean isLive) { info.isLive = isLive; return this; }
        public Builder ext(String ext) { info.ext = ext; return this; }
        public Builder jar(String jar) { info.jar = jar; return this; }
        public Builder searchable(int searchable) { info.searchable = searchable; return this; }
        public Builder category(String category) { info.category = category; return this; }
        public Builder status(int status) { info.status = status; return this; }

        public SiteInfo build() { return info; }
    }
}
