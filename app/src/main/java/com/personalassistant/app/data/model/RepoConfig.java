package com.personalassistant.app.data.model;

import java.util.List;

public class RepoConfig {
    public String spider;
    public String logo;
    public String wallpaper;
    public List<LiveSource> lives;
    public List<SiteInfo> sites;
    public List<ParseConfig> parses;
    public List<String> flags;
    public List<DohConfig> doh;
    public List<RuleConfig> rules;
}
