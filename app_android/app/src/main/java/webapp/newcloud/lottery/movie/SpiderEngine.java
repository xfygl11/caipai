package webapp.newcloud.lottery.movie;

import android.content.Context;
import android.util.Log;

import com.fongmi.quickjs.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import webapp.newcloud.lottery.movie.model.SiteConfig;

public class SpiderEngine {

    private static final String TAG = "SpiderEngine";
    private static volatile SpiderEngine instance;
    private final ConcurrentHashMap<String, SpiderBase> spiderMap = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private com.whl.quickjs.wrapper.QuickJSContext ctx;
    private Context appContext;
    private volatile String currentSpiderName;
    private String spiderJarUrl;
    private String cachedJarPath;

    private SpiderEngine(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
    }

    public static SpiderEngine getInstance() {
        if (instance == null) {
            synchronized (SpiderEngine.class) {
                if (instance == null) {
                    instance = new SpiderEngine(null);
                }
            }
        }
        return instance;
    }

    public synchronized void init(Context context) {
        if (context != null) {
            this.appContext = context.getApplicationContext();
        }
        if (ctx == null) {
            // Initialize QuickJS native library first
            com.whl.quickjs.android.QuickJSLoader.init();
            ctx = com.whl.quickjs.wrapper.QuickJSContext.create();
            ctx.setMaxStackSize(1 << 22);
            ctx.setMemoryLimit(32 << 20);
            registerSpider("push", new SpiderNull());
            Log.i(TAG, "Spider engine initialized");
        }
    }

    private void registerSpider(String name, com.github.catvod.crawler.Spider spider) {
        spiderMap.put(name, new CatVodSpider(spider, appContext));
        Log.i(TAG, "Registered spider: " + name);
    }

    /**
     * Load all spiders from a site config list.
     */
    public void loadSpiders(List<SiteConfig> sites) {
        for (SiteConfig site : sites) {
            String pluginKey = !site.key.isEmpty() ? site.key : site.name;
            loadSpider(pluginKey, site.api, site.ext, site.type);
        }
    }

    /**
     * Load a single spider plugin.
     */
    public void loadSpider(String key, String api, String ext, int type) {
        if (api == null || api.isEmpty()) {
            Log.w(TAG, "Cannot load spider with empty api: " + key);
            return;
        }

        try {
            if (type == 3 && (api.startsWith("http://") || api.startsWith("https://"))) {
                // Remote JS spider (drpy2 style)
                loadRemoteJsSpider(key, api, ext);
            } else if (type == 3 && api.startsWith("csp_")) {
                // Java spider class - needs DexClassLoader from JAR
                loadJavaSpider(key, api, ext);
            } else {
                Log.w(TAG, "Unsupported spider type/api for " + key + ": type=" + type + " api=" + api);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load spider " + key + ": " + e.getMessage());
        }
    }

    /**
     * Load a remote JS spider using quickjs/crawler/Spider.
     */
    private void loadRemoteJsSpider(String key, String jsUrl, String ext) {
        executor.submit(() -> {
            try {
                Spider spider = new Spider(jsUrl, null);
                String extend = ext != null && !ext.isEmpty() ? ext : "{}";
                spider.init(appContext, extend);
                registerSpider(key, spider);
                Log.i(TAG, "Loaded remote JS spider: " + key);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load remote JS spider " + key + ": " + e.getMessage());
            }
        });
    }

    /**
     * Load a Java spider class from the downloaded spider JAR.
     */
    private void loadJavaSpider(String key, String className, String ext) {
        executor.submit(() -> {
            try {
                String jarPath = ensureSpiderJar();
                if (jarPath == null) {
                    Log.w(TAG, "No spider JAR available for " + key);
                    return;
                }

                File jarFile = new File(jarPath);
                if (!jarFile.exists()) {
                    Log.w(TAG, "Spider JAR not found: " + jarPath);
                    return;
                }

                // Create DexClassLoader
                String dexOutputDir = appContext.getCacheDir().getAbsolutePath() + "/dex";
                new File(dexOutputDir).mkdirs();

                dalvik.system.DexClassLoader dexClassLoader = new dalvik.system.DexClassLoader(
                        jarPath,
                        dexOutputDir,
                        null,
                        appContext.getClassLoader()
                );

                // Try multiple class name patterns
                String[] classNames = {
                    "com.github.catvod.spider." + className.replace("csp_", ""),
                    "com.github.catvod.parser." + className.replace("csp_", ""),
                    className.replace("csp_", ""),
                };

                for (String cn : classNames) {
                    try {
                        Class<?> clazz = dexClassLoader.loadClass(cn);
                        com.github.catvod.crawler.Spider spider =
                                (com.github.catvod.crawler.Spider) clazz.getDeclaredConstructor().newInstance();
                        String extend = ext != null && !ext.isEmpty() ? ext : "{}";
                        spider.init(appContext, extend);
                        registerSpider(key, spider);
                        Log.i(TAG, "Loaded Java spider: " + key + " (" + cn + ")");
                        return;
                    } catch (ClassNotFoundException e) {
                        // Try next pattern
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to instantiate " + cn + ": " + e.getMessage());
                    }
                }

                Log.e(TAG, "Could not find spider class for: " + key);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load Java spider " + key + ": " + e.getMessage());
            }
        });
    }

    /**
     * Ensure the spider JAR is downloaded and cached.
     */
    private String ensureSpiderJar() {
        if (cachedJarPath != null) return cachedJarPath;
        if (spiderJarUrl == null || spiderJarUrl.isEmpty()) return null;

        try {
            File jarFile = new File(appContext.getCacheDir(), "spider.jar");
            if (jarFile.exists() && jarFile.length() > 0) {
                Log.d(TAG, "Using cached spider JAR: " + jarFile.length() + " bytes");
                cachedJarPath = jarFile.getAbsolutePath();
                return cachedJarPath;
            }

            java.net.URL url = new java.net.URL(spiderJarUrl.split(";")[0]);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Log.w(TAG, "Spider JAR download failed: HTTP " + responseCode);
                conn.disconnect();
                return null;
            }

            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(jarFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }

            conn.disconnect();
            cachedJarPath = jarFile.getAbsolutePath();
            Log.i(TAG, "Downloaded spider JAR: " + jarFile.length() + " bytes");
            return cachedJarPath;
        } catch (Exception e) {
            Log.e(TAG, "Failed to download spider JAR: " + e.getMessage());
            return null;
        }
    }

    /**
     * Set the spider JAR URL from warehouse config.
     */
    public void setSpiderJarUrl(String url) {
        this.spiderJarUrl = url;
    }

    public void setCurrentSpider(String name) {
        this.currentSpiderName = name;
    }

    public JSONObject home(boolean fastLoad, String extend) {
        SpiderBase spider = getCurrentSpider();
        if (spider == null) return createEmptyHome();
        try {
            return new JSONObject(spider.homeContent(fastLoad));
        } catch (Exception e) {
            Log.e(TAG, "home error", e);
            return createEmptyHome();
        }
    }

    private JSONObject createEmptyHome() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("class", new JSONArray());
            obj.put("list", new JSONArray());
            return obj;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public JSONObject homeVideo() {
        SpiderBase spider = getCurrentSpider();
        if (spider == null) return new JSONObject();
        try {
            return new JSONObject(spider.homeVideoContent());
        } catch (Exception e) {
            Log.e(TAG, "homeVideo error", e);
            return new JSONObject();
        }
    }

    public JSONObject category(String tid, int pg, boolean filter, Map<String, String> extend) {
        SpiderBase spider = getCurrentSpider();
        if (spider == null) return createEmptyCategory();
        try {
            return new JSONObject(spider.categoryContent(tid, String.valueOf(pg), filter, new HashMap<>(extend)));
        } catch (Exception e) {
            Log.e(TAG, "category error", e);
            return createEmptyCategory();
        }
    }

    private JSONObject createEmptyCategory() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("page", 0);
            obj.put("pagecount", 0);
            obj.put("list", new JSONArray());
            return obj;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public JSONObject detail(String id) {
        SpiderBase spider = getCurrentSpider();
        if (spider == null) return new JSONObject();
        try {
            return new JSONObject(spider.detailContent(List.of(id)));
        } catch (Exception e) {
            Log.e(TAG, "detail error", e);
            return new JSONObject();
        }
    }

    public String play(String flag, String url, Map<String, String> vipFlags) {
        SpiderBase spider = getCurrentSpider();
        if (spider == null) return url;
        try {
            return spider.playerContent(flag, url, List.of());
        } catch (Exception e) {
            Log.e(TAG, "play error", e);
            return url;
        }
    }

    public JSONObject search(String keyword, boolean quick) {
        SpiderBase spider = getCurrentSpider();
        if (spider == null) return createEmptySearch();
        try {
            return new JSONObject(spider.searchContent(keyword, quick));
        } catch (Exception e) {
            Log.e(TAG, "search error", e);
            return createEmptySearch();
        }
    }

    private JSONObject createEmptySearch() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("page", 0);
            obj.put("pagecount", 0);
            obj.put("list", new JSONArray());
            return obj;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private SpiderBase getCurrentSpider() {
        if (currentSpiderName == null || currentSpiderName.isEmpty()) return null;
        return spiderMap.get(currentSpiderName);
    }

    public void destroy() {
        for (SpiderBase spider : spiderMap.values()) {
            try {
                if (spider instanceof CatVodSpider) {
                    CatVodSpider cs = (CatVodSpider) spider;
                    com.github.catvod.crawler.Spider s = cs.getSpider();
                    if (s != null) s.destroy();
                }
            } catch (Exception ignored) {}
        }
        executor.shutdownNow();
        if (ctx != null) ctx.destroy();
    }
}
