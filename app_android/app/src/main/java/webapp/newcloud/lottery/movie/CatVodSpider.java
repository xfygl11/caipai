package webapp.newcloud.lottery.movie;

import android.content.Context;
import android.util.Log;

import com.github.catvod.crawler.Spider;

import java.util.Map;

class CatVodSpider implements SpiderBase {

    private static final String TAG = "CatVodSpider";
    private final Spider spider;
    private final Context context;

    CatVodSpider(Spider spider, Context context) {
        this.spider = spider;
        this.context = context;
    }

    public Spider getSpider() {
        return spider;
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            return spider.homeContent(filter);
        } catch (Exception e) {
            Log.e(TAG, "homeContent error: " + e.getMessage());
            return "{}";
        }
    }

    @Override
    public String homeVideoContent() {
        try {
            return spider.homeVideoContent();
        } catch (Exception e) {
            Log.e(TAG, "homeVideoContent error: " + e.getMessage());
            return "[]";
        }
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, Map<String, String> extend) {
        try {
            return spider.categoryContent(tid, pg, filter, new java.util.HashMap<>(extend));
        } catch (Exception e) {
            Log.e(TAG, "categoryContent error: " + e.getMessage());
            return "{}";
        }
    }

    @Override
    public String detailContent(java.util.List<String> ids) {
        try {
            return spider.detailContent(ids);
        } catch (Exception e) {
            Log.e(TAG, "detailContent error: " + e.getMessage());
            return "[]";
        }
    }

    @Override
    public String playerContent(String flag, String url, java.util.List<String> vipFlags) {
        try {
            return spider.playerContent(flag, url, vipFlags);
        } catch (Exception e) {
            Log.e(TAG, "playerContent error: " + e.getMessage());
            return url;
        }
    }

    @Override
    public String searchContent(String key, boolean quick) {
        try {
            return spider.searchContent(key, quick);
        } catch (Exception e) {
            Log.e(TAG, "searchContent error: " + e.getMessage());
            return "{}";
        }
    }
}
