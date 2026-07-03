package com.personalassistant.app.bridge;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.CategoryEntity;
import com.personalassistant.app.db.entity.MovieEntity;
import com.personalassistant.app.db.entity.SourceEntity;
import com.personalassistant.app.util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AndroidJSBridge {
    private static final String TAG = "AndroidJSBridge";
    private static final String[] LOTTERY_IDS = {"dlt", "ssq", "qlc", "fc3d", "pl3", "pl5", "qxc", "kl8"};

    private final Context context;
    private final AppDatabase database;
    private final Gson gson = new Gson();
    private volatile android.webkit.WebView webView;

    public AndroidJSBridge(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
    }

    public void setWebView(android.webkit.WebView wv) {
        this.webView = wv;
    }

    @JavascriptInterface
    public String fetchLatest(String lotteryId) {
        Log.d(TAG, "fetchLatest called for: " + lotteryId);
        try {
            String key = lotteryId + "_draws";
            String raw = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(key, "");
            if (!raw.isEmpty()) {
                org.json.JSONArray arr = new org.json.JSONArray(raw);
                if (arr.length() > 0) {
                    org.json.JSONObject latest = arr.getJSONObject(0);
                    org.json.JSONObject result = new org.json.JSONObject();
                    result.put("period", latest.has("p") ? latest.getString("p") : "");
                    result.put("date", latest.has("d") ? latest.getString("d") : "");
                    
                    org.json.JSONArray front = new org.json.JSONArray();
                    if (latest.has("f") && latest.get("f") instanceof org.json.JSONArray) {
                        front = latest.getJSONArray("f");
                    }
                    result.put("front", front);
                    
                    org.json.JSONArray back = new org.json.JSONArray();
                    if (latest.has("b") && latest.get("b") instanceof org.json.JSONArray) {
                        back = latest.getJSONArray("b");
                    }
                    result.put("back", back);
                    
                    if (latest.has("sales")) result.put("sales", latest.getString("sales"));
                    if (latest.has("pool")) result.put("pool", latest.getString("pool"));
                    if (latest.has("grades")) result.put("grades", latest.getJSONArray("grades"));
                    
                    return result.toString();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading lottery data for " + lotteryId, e);
        }
        return "[]";
    }

    @JavascriptInterface
    public void saveLotteryDraw(String lotteryId, String jsonDraw) {
        try {
            String key = lotteryId + "_draws";
            android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, jsonDraw)
                .apply();
            Log.d(TAG, "Saved lottery draw for " + lotteryId);
        } catch (Exception e) {
            Log.e(TAG, "Error saving lottery draw", e);
        }
    }

    @JavascriptInterface
    public String httpGet(String url) {
        try {
            String result = HttpUtil.getSync(url, 15000);
            if (result == null) {
                return "__ERROR__Connection timeout";
            }
            return result;
        } catch (Exception e) {
            return "__ERROR__" + e.getMessage();
        }
    }

    @JavascriptInterface
    public void syncToLocalDb(String jsonData) {
        try {
            JSONObject json = new JSONObject(jsonData);
            int sourceId = json.getInt("sourceId");
            String category = json.getString("category");
            JSONArray moviesArr = json.getJSONArray("movies");

            List<MovieEntity> movies = new ArrayList<>();
            for (int i = 0; i < moviesArr.length(); i++) {
                JSONObject m = moviesArr.getJSONObject(i);
                MovieEntity entity = new MovieEntity();
                entity.sourceId = sourceId;
                entity.category = category;
                entity.vodId = m.optString("vod_id", m.optString("id", ""));
                entity.title = m.optString("vod_name", m.optString("title", ""));
                entity.pic = m.optString("vod_pic", m.optString("pic", ""));
                entity.tag = m.optString("vod_remarks", m.optString("tag", ""));
                entity.type = m.optString("type_name", m.optString("type", ""));
                entity.year = m.optString("vod_year", m.optString("year", ""));
                entity.area = m.optString("vod_area", "");
                entity.actor = m.optString("vod_actor", "");
                entity.director = m.optString("vod_director", "");
                entity.score = m.optString("vod_score", "");
                entity.quality = m.optString("vod_remarks", "");
                entity.play = m.optString("vod_play_url", "");
                entity.desc = m.optString("vod_content", "");
                entity.rawData = m.toString();
                entity.updateTime = System.currentTimeMillis();
                movies.add(entity);
            }

            new Thread(() -> {
                try {
                    database.movieDao().insertAll(movies);
                    Log.d(TAG, "Synced " + movies.size() + " movies to Room");
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing to Room", e);
                }
            }).start();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing sync JSON", e);
        }
    }

    @JavascriptInterface
    public String getLocalMovies(String jsonParams) {
        try {
            JSONObject params = new JSONObject(jsonParams);
            int sourceId = params.getInt("sourceId");
            String category = params.getString("category");
            int limit = params.optInt("limit", 100);
            int offset = params.optInt("offset", 0);

            List<MovieEntity> movies = database.movieDao()
                .getBySourceAndCategory(sourceId, category, limit, offset);

            JSONArray result = new JSONArray();
            for (MovieEntity m : movies) {
                JSONObject obj = new JSONObject();
                obj.put("id", m.vodId);
                obj.put("title", m.title);
                obj.put("pic", m.pic);
                obj.put("tag", m.tag);
                obj.put("type", m.type);
                obj.put("year", m.year);
                obj.put("cat", m.category);
                result.put(obj);
            }
            return result.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Error getting local movies", e);
            return "[]";
        }
    }

    @JavascriptInterface
    public void initBuiltInSources() {
        new Thread(() -> {
            try {
                String[] names = {"非凡采集", "暴风采集", "虎牙采集", "金鹰采集", "新浪影视", "红牛影视", "闪电影视", "光速影视", "茶杯狐"};
                String[] urls = {
                    "http://cj.ffzyapi.com/api.php/provide/vod",
                    "https://bfzyapi.com/api.php/provide/vod/",
                    "https://www.huyaapi.com/api.php/provide/vod/",
                    "https://jyzyapi.com/provide/vod/",
                    "http://api.xinlangapi.com/xinlangapi.php/provide/vod/",
                    "https://www.hongniuzy2.com/api.php/provide/vod/",
                    "http://sdzyapi.com/api.php/provide/vod/",
                    "https://api.guangsuapi.com/api.php/provide/vod/",
                    "https://hhzyapi.com/api.php/provide/vod"
                };

                for (int i = 0; i < names.length; i++) {
                    String base = urls[i].replaceAll("/$", "");
                    SourceEntity existing = database.sourceDao().getByBase(base);
                    if (existing != null) continue;

                    SourceEntity source = new SourceEntity();
                    source.name = names[i];
                    source.url = urls[i];
                    source.base = base;
                    source.isBuiltin = 1;
                    source.addedTime = System.currentTimeMillis();
                    source.lastSyncTime = 0;
                    source.status = 1;
                    database.sourceDao().insert(source);
                    Log.d(TAG, "Initialized builtin source: " + names[i]);
                }

                notifyWebView("onBuiltInSourcesReady");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing built-in sources", e);
            }
        }).start();
    }

    @JavascriptInterface
    public String getSourcesJson() {
        try {
            List<SourceEntity> sources = database.sourceDao().getAll();
            JSONArray arr = new JSONArray();
            for (SourceEntity s : sources) {
                JSONObject obj = new JSONObject();
                obj.put("id", s.id);
                obj.put("name", s.name);
                obj.put("url", s.url);
                obj.put("base", s.base);
                obj.put("is_builtin", s.isBuiltin);
                obj.put("last_sync_time", s.lastSyncTime);
                arr.put(obj);
            }
            return arr.toString();
        } catch (JSONException e) {
            return "[]";
        }
    }

    @JavascriptInterface
    public String getCategoriesJson(int sourceId) {
        try {
            List<CategoryEntity> cats = database.categoryDao().getBySourceId(sourceId);
            JSONArray arr = new JSONArray();
            for (CategoryEntity c : cats) {
                JSONObject obj = new JSONObject();
                obj.put("name", c.name);
                obj.put("rawName", c.rawName);
                obj.put("typeId", c.typeId);
                obj.put("parentId", c.parentId);
                arr.put(obj);
            }
            return arr.toString();
        } catch (JSONException e) {
            return "[]";
        }
    }

    @JavascriptInterface
    public String getStatsJson() {
        try {
            int sources = database.sourceDao().getAll().size();
            int movies = 0;
            List<SourceEntity> allSources = database.sourceDao().getAll();
            for (SourceEntity s : allSources) {
                movies += database.movieDao().countBySourceId(s.id);
            }
            JSONObject obj = new JSONObject();
            obj.put("sources", sources);
            obj.put("movies", movies);
            return obj.toString();
        } catch (JSONException e) {
            return "{\"sources\":0,\"movies\":0}";
        }
    }

    private void notifyWebView(String eventName) {
        if (webView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            String js = "window.dispatchEvent(new CustomEvent('" + eventName + "'))";
            webView.evaluateJavascript(js, null);
        });
    }

    public void notifyWebViewDirect(String eventName) {
        notifyWebView(eventName);
    }
}
