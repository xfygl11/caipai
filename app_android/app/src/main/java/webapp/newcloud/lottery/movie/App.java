package webapp.newcloud.lottery.movie;

import androidx.annotation.NonNull;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import webapp.newcloud.lottery.movie.util.DbHelper;
import webapp.newcloud.lottery.movie.util.FanTaiYingParser;
import webapp.newcloud.lottery.movie.util.HttpClient;
import webapp.newcloud.lottery.movie.util.LogManager;
import webapp.newcloud.lottery.movie.util.SiteInfo;
import webapp.newcloud.lottery.movie.model.SiteConfig;
import webapp.newcloud.lottery.movie.model.Warehouse;
import webapp.newcloud.lottery.movie.db.AppDatabase;

import org.json.JSONObject;

public class App extends Application {
    private static final String TAG = "App";
    private static final String PREFS_NAME = "spider_prefs";
    private static final String KEY_FAN_TAI_YING_LOADED = "fan_tai_ying_loaded";
    private static final String KEY_SITE_NAME = "current_site_name";
    private static final String FAN_TAI_YING_URL = "http://www.饭太硬.net/tv";
    private static final String FAN_TAI_YING_NAME = "饭太硬";
    private static App instance;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        DbHelper.getInstance(this);
        SpiderEngine.getInstance().init(this);
        
        // 设置全局异常捕获
        setupGlobalExceptionHandler();
        
        // Pre-load FanTaiYing config on first launch
        loadFanTaiYingIfNeeded();
    }
    
    /**
     * 设置全局异常处理器 - 捕获所有未处理的异常并记录到日志管理器
     */
    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
                try {
                    // 记录崩溃信息
                    StringBuilder sb = new StringBuilder();
                    sb.append("应用崩溃\n");
                    sb.append("线程: ").append(thread.getName()).append("\n");
                    sb.append("异常: ").append(ex.toString()).append("\n");
                    sb.append("消息: ").append(ex.getMessage()).append("\n");
                    
                    // 添加堆栈信息
                    for (StackTraceElement element : ex.getStackTrace()) {
                        sb.append("    at ").append(element.toString()).append("\n");
                    }
                    
                    // 如果有 cause，也记录
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        sb.append("Cause: ").append(cause.toString()).append("\n");
                        for (StackTraceElement element : cause.getStackTrace()) {
                            sb.append("    at ").append(element.toString()).append("\n");
                        }
                    }
                    
                    // 记录到 LogManager
                    LogManager.addCrash(sb.toString());
                    
                    // 保存到文件
                    saveCrashToFile(ex);
                    
                } catch (Exception e) {
                    // 忽略异常记录过程中的错误
                    e.printStackTrace();
                } finally {
                    // 调用默认的异常处理器，让应用正常退出
                    Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
                    if (defaultHandler != null && defaultHandler != this) {
                        defaultHandler.uncaughtException(thread, ex);
                    }
                    // 如果默认处理器为 null 或就是自己，强制退出
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                }
            }
        });
        
        // 记录应用启动
        LogManager.addLog("应用启动");
    }
    
    /**
     * 保存崩溃信息到文件
     * @param ex 异常对象
     */
    private void saveCrashToFile(Throwable ex) {
        try {
            java.io.File crashFile = new java.io.File(getFilesDir(), "crash_log.txt");
            java.io.FileWriter writer = new java.io.FileWriter(crashFile, true); // 追加模式
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            writer.write("=== 崩溃时间: " + dateFormat.format(new Date()) + " ===\n");
            writer.write(ex.toString() + "\n");
            writer.write(ex.getMessage() + "\n");
            
            // 写入堆栈信息
            for (StackTraceElement element : ex.getStackTrace()) {
                writer.write("    at " + element.toString() + "\n");
            }
            
            writer.write("\n");
            writer.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFanTaiYingIfNeeded() {
        if (prefs.getBoolean(KEY_FAN_TAI_YING_LOADED, false)) {
            return;
        }
        
        new Thread(() -> {
            try {
                HttpClient httpClient = new HttpClient();
                byte[] rawData = httpClient.httpGetRaw(FAN_TAI_YING_URL);
                if (rawData == null || rawData.length == 0) {
                    Log.w(TAG, "FanTaiYing returned empty data");
                    return;
                }
                
                JSONObject config = FanTaiYingParser.parse(rawData);
                List<SiteInfo> siteInfos = FanTaiYingParser.parseSites(config);
                
                // Create warehouse
                String warehouseId = java.util.UUID.randomUUID().toString();
                Warehouse warehouse = new Warehouse();
                warehouse.id = warehouseId;
                warehouse.name = FAN_TAI_YING_NAME;
                warehouse.url = FAN_TAI_YING_URL;
                warehouse.spider = FanTaiYingParser.getSpiderUrl(config);
                warehouse.logo = FanTaiYingParser.getLogoUrl(config);
                warehouse.wallpaper = FanTaiYingParser.getWallpaperUrl(config);
                warehouse.createdAt = System.currentTimeMillis();
                DbHelper.getInstance(App.this).warehouseDao().insert(warehouse);
                
                // Save sites
                int saved = 0;
                for (SiteInfo si : siteInfos) {
                    SiteConfig site = new SiteConfig();
                    site.id = java.util.UUID.randomUUID().toString();
                    site.warehouseId = warehouseId;
                    site.key = si.key;
                    site.name = si.name;
                    site.type = si.type;
                    site.api = si.api;
                    site.searchable = si.searchable;
                    site.quickSearch = si.quickSearch;
                    site.filterable = si.filterable;
                    site.ext = si.ext != null ? si.ext : "{}";
                    site.sourceType = "warehouse";
                    site.createdAt = System.currentTimeMillis();
                    DbHelper.getInstance(App.this).siteConfigDao().insert(site);
                    saved++;
                }
                
                Log.d(TAG, "FanTaiYing loaded: " + saved + " sites");

                // Load spider engine with all sites
                String spiderUrl = FanTaiYingParser.getSpiderUrl(config);
                if (!spiderUrl.isEmpty()) {
                    SpiderEngine.getInstance().setSpiderJarUrl(spiderUrl.split(";")[0]);
                }
                
                List<SiteConfig> allSites = DbHelper.getInstance(App.this).siteConfigDao().getAll();
                SpiderEngine.getInstance().loadSpiders(allSites);
                
                // Mark as loaded
                prefs.edit().putBoolean(KEY_FAN_TAI_YING_LOADED, true).apply();
                
            } catch (Exception e) {
                Log.w(TAG, "Failed to load FanTaiYing config: " + e.getMessage());
            }
        }).start();
    }

    public static App getInstance() {
        return instance;
    }

    public static String getCurrentSiteName() {
        if (instance != null) {
            return instance.prefs.getString(KEY_SITE_NAME, null);
        }
        return null;
    }

    public static void setCurrentSiteName(String name) {
        if (instance != null) {
            instance.prefs.edit().putString(KEY_SITE_NAME, name).apply();
        }
    }
}
