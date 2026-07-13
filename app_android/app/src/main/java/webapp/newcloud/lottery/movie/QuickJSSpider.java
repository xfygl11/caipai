package webapp.newcloud.lottery.movie;

import android.util.Log;

import com.whl.quickjs.wrapper.JSObject;

import java.util.Map;

class QuickJSSpider implements SpiderBase {

    private static final String TAG = "QuickJSSpider";
    private final JSObject spiderObj;

    QuickJSSpider(com.whl.quickjs.wrapper.QuickJSContext ctx, JSObject spiderObj) {
        this.spiderObj = spiderObj;
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            Object result = spiderObj.getJSFunction("home").call(filter);
            return result != null ? result.toString() : "{}";
        } catch (Exception e) {
            Log.e(TAG, "homeContent error: " + e.getMessage());
            return "{}";
        } finally {
            spiderObj.getJSFunction("home").release();
        }
    }

    @Override
    public String homeVideoContent() {
        try {
            Object result = spiderObj.getJSFunction("homeVod").call();
            return result != null ? result.toString() : "[]";
        } catch (Exception e) {
            Log.e(TAG, "homeVideoContent error: " + e.getMessage());
            return "[]";
        } finally {
            spiderObj.getJSFunction("homeVod").release();
        }
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, Map<String, String> extend) {
        try {
            Object result = spiderObj.getJSFunction("cate").call(tid, pg, filter, extend);
            return result != null ? result.toString() : "[]";
        } catch (Exception e) {
            Log.e(TAG, "categoryContent error: " + e.getMessage());
            return "[]";
        } finally {
            spiderObj.getJSFunction("cate").release();
        }
    }

    @Override
    public String detailContent(java.util.List<String> ids) {
        try {
            Object result = spiderObj.getJSFunction("detail").call(ids.get(0));
            return result != null ? result.toString() : "[]";
        } catch (Exception e) {
            Log.e(TAG, "detailContent error: " + e.getMessage());
            return "[]";
        } finally {
            spiderObj.getJSFunction("detail").release();
        }
    }

    @Override
    public String playerContent(String flag, String url, java.util.List<String> vipFlags) {
        try {
            Object result = spiderObj.getJSFunction("play").call(flag, url);
            return result != null ? result.toString() : url;
        } catch (Exception e) {
            Log.e(TAG, "playerContent error: " + e.getMessage());
            return url;
        } finally {
            spiderObj.getJSFunction("play").release();
        }
    }

    @Override
    public String searchContent(String key, boolean quick) {
        try {
            Object result = spiderObj.getJSFunction("search").call(key, quick);
            return result != null ? result.toString() : "[]";
        } catch (Exception e) {
            Log.e(TAG, "searchContent error: " + e.getMessage());
            return "[]";
        } finally {
            spiderObj.getJSFunction("search").release();
        }
    }
}
