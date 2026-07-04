package com.personalassistant.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.personalassistant.app.R;
import com.personalassistant.app.data.model.CategoryInfo;
import com.personalassistant.app.data.model.MovieItem;
import com.personalassistant.app.data.model.ParseConfig;
import com.personalassistant.app.data.model.RepoConfig;
import com.personalassistant.app.data.model.SiteInfo;
import com.personalassistant.app.data.repository.MovieRepository;
import com.personalassistant.app.data.repository.SiteRepository;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.CategoryEntity;
import com.personalassistant.app.db.entity.MovieEntity;
import com.personalassistant.app.db.entity.ParseConfigEntity;
import com.personalassistant.app.db.entity.SourceEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsFragment extends Fragment {

    // v9.9 constants
    static final String STORE_KEY = "nc_api_sources_v1";
    private static final String PREF_CONFIG_HISTORY = "config_history_v9";
    private static final int MAX_CONFIG_HISTORY = 20;

    // v9.9 preset sources
    private static final String[][] PRESET_SOURCES = {
            {"饭太硬", "https://raw.liueu.cn/main/jk.json"},
            {"欧歌", "https://mu.ofov.eu.org/JS/js0.json"},
            {"肥猫", "http://我不是.肥猫.live/TVBOX/interfaces/config23.json"},
            {"100km", "https://raw.liueu.cn/main/100km.json"},
            {"多多", "https://yydsys.pro/jsonconfig"},
            {"运输车", "https://codeberg.org/jade020/box-json/raw/branch/main/api.json"},
            {"小米", "https://raw.liueu.cn/main/xiao.json"},
            {"月光", "https://codeberg.org/mooyang123/getconfig/raw/branch/main/APIJSON/api.json"}
    };

    private LinearLayout settingsList;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    // Collection state
    private volatile boolean isCollecting = false;
    private Thread collectionThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        prefs = requireContext().getSharedPreferences(STORE_KEY, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF05070D);

        ScrollView scroll = new ScrollView(requireContext());
        settingsList = new LinearLayout(requireContext());
        settingsList.setOrientation(LinearLayout.VERTICAL);
        settingsList.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(settingsList);
        root.addView(scroll);

        // Header
        TextView header = new TextView(requireContext());
        header.setText("设置");
        header.setTextColor(0xFFFFFFFF);
        header.setTextSize(22);
        header.setPadding(0, dp(8), 0, dp(24));
        settingsList.addView(header);

        // Section 1: 仓库管理
        addSection("仓库管理");
        addSettingsItem("添加仓库", v -> showAddRepoDialog());
        addSettingsItem("管理仓库", v -> showRepoManageDialog());
        addSettingsItem("仓库信息", v -> showRepoInfo());

        addDivider();

        // Section 2: 影视源 (v9.9 style)
        addSection("影视源");
        addSettingsItem("配置影视源", v -> showConfigSourceDialog());
        addSettingsItem("配置历史", v -> showConfigHistoryDialog());
        addSettingsItem("预设源列表", v -> showPresetSourcesDialog());
        addSettingsItem("扫码", v -> showScanPlaceholder());

        addDivider();

        // Section 3: 数据采集 (v9.9 style)
        addSection("数据采集");
        addSettingsItem("打开采集窗口", v -> showCollectionWindow());
        addSettingsItem("手动采集", v -> startManualCollection());

        addDivider();

        // Section 4: 数据管理
        addSection("数据管理");
        addSettingsItem("数据库查看", v -> showDatabaseViewer());
        addSettingsItem("清空数据库", v -> showClearDatabaseDialog());

        addDivider();

        // Section 5: 设置中心
        addSection("设置中心");
        addSettingsItem("清理缓存", v -> showClearCacheDialog());
        addSettingsItem("导出配置", v -> exportConfig());
        addSettingsItem("关于", v -> showAboutDialog());

        return root;
    }

    // --- Layout Helpers ---

    private void addSection(String title) {
        TextView sectionTitle = new TextView(requireContext());
        sectionTitle.setText(title);
        sectionTitle.setTextColor(0xFF7C3AED);
        sectionTitle.setTextSize(13);
        sectionTitle.setPadding(0, dp(12), 0, dp(4));
        settingsList.addView(sectionTitle);
    }

    private void addDivider() {
        View divider = new View(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(0, dp(12), 0, dp(12));
        divider.setLayoutParams(params);
        divider.setBackgroundColor(0x1AFFFFFF);
        settingsList.addView(divider);
    }

    private void addSettingsItem(String text, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setBackgroundColor(0xFF101D2B);
        row.setOnClickListener(onClick);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFFE8EDFF);
        tv.setTextSize(15);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tv.setLayoutParams(tvParams);

        row.addView(tv);
        settingsList.addView(row);
    }

    private void addSettingsItemWithRightText(String text, String rightText, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setBackgroundColor(0xFF101D2B);
        row.setOnClickListener(onClick);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFFE8EDFF);
        tv.setTextSize(15);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tv.setLayoutParams(tvParams);

        TextView rightTv = new TextView(requireContext());
        rightTv.setText(rightText);
        rightTv.setTextColor(0xFF6F7890);
        rightTv.setTextSize(12);

        row.addView(tv);
        row.addView(rightTv);
        settingsList.addView(row);
    }

    private static int dp(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // --- Section 1: 仓库管理 ---

    private void showAddRepoDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("仓库地址...");
        input.setHintTextColor(0xFF6F7890);
        input.setTextColor(0xFFE8EDFF);
        input.setText("https://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json");

        new AlertDialog.Builder(requireContext())
                .setTitle("添加仓库")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) loadAndSaveRepo(url);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadAndSaveRepo(String repoUrl) {
        executor.execute(() -> {
            try {
                RepoConfig config = SiteRepository.loadRepoConfig(repoUrl);
                if (config == null) {
                    handler.post(() -> Toast.makeText(requireContext(), "仓库加载失败", Toast.LENGTH_SHORT).show());
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(requireContext());

                // Save movie sites
                for (SiteInfo site : config.sites) {
                    if (!"MV".equals(site.typeLabel)) continue;
                    if (site.api.isEmpty()) continue;

                    String base = site.api.replace("/api.php/provide/vod/at/xml/", "")
                            .replace("/api.php/provide/vod/", "");

                    SourceEntity entity = new SourceEntity();
                    entity.name = site.name;
                    entity.url = site.api;
                    entity.base = base;
                    entity.isBuiltin = 0;
                    entity.addedTime = System.currentTimeMillis();
                    entity.lastSyncTime = System.currentTimeMillis();
                    entity.status = 1;

                    SourceEntity existing = db.sourceDao().getByBaseBlocking(base);
                    if (existing != null) {
                        entity.id = existing.id;
                        db.sourceDao().updateLastSync(entity.id, System.currentTimeMillis());
                    } else {
                        db.sourceDao().insert(entity);
                    }
                }

                // Save parses (type=0 only)
                for (ParseConfig parse : config.parses) {
                    if (parse.type != 0) continue;
                    if (parse.url.isEmpty() || "Demo".equalsIgnoreCase(parse.url)) continue;

                    ParseConfigEntity entity = new ParseConfigEntity();
                    entity.name = parse.name;
                    entity.url = parse.url;
                    entity.type = parse.type;
                    entity.sortOrder = 0;
                    if (parse.flags != null && !parse.flags.isEmpty()) {
                        entity.flags = String.join(",", parse.flags);
                    }
                    db.parseConfigDao().insert(entity);
                }

                handler.post(() -> Toast.makeText(requireContext(), "仓库已保存", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(),
                        "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showRepoManageDialog() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();

                handler.post(() -> {
                    if (sources == null || sources.isEmpty()) {
                        Toast.makeText(requireContext(), "暂无仓库", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] items = new String[sources.size()];
                    for (int i = 0; i < sources.size(); i++) {
                        items[i] = sources.get(i).name + "\n" + sources.get(i).base;
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("仓库列表")
                            .setItems(items, (dialog, which) -> {
                                // Show repo detail
                            })
                            .setPositiveButton("添加", (d, w) -> showAddRepoDialog())
                            .setNeutralButton("删除", (dialog, which) -> {
                                SourceEntity src = sources.get(which);
                                db.sourceDao().deleteById(src.id);
                                Toast.makeText(requireContext(), "已删除: " + src.name, Toast.LENGTH_SHORT).show();
                                showRepoManageDialog();
                            })
                            .show();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showRepoInfo() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
                List<ParseConfigEntity> parses = db.parseConfigDao().getAllConfigsBlocking();

                int movieCount = 0;
                if (sources != null && !sources.isEmpty()) {
                    for (SourceEntity s : sources) {
                        movieCount += db.movieDao().countBySourceId(s.id);
                    }
                }

                StringBuilder info = new StringBuilder();
                info.append("仓库数量: ").append(sources != null ? sources.size() : 0).append("\n");
                info.append("解析器数量: ").append(parses != null ? parses.size() : 0).append("\n");
                info.append("影视总数: ").append(movieCount);

                handler.post(() -> new AlertDialog.Builder(requireContext())
                        .setTitle("仓库信息")
                        .setMessage(info.toString())
                        .setPositiveButton("确定", null)
                        .show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // --- Section 2: 影视源 (v9.9 style) ---

    private void showConfigSourceDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("影视源配置URL...");
        input.setHintTextColor(0xFF6F7890);
        input.setTextColor(0xFFE8EDFF);

        new AlertDialog.Builder(requireContext())
                .setTitle("配置影视源")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(requireContext(), "URL不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveConfigHistory(url);
                    loadAndSaveRepo(url);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveConfigHistory(String url) {
        executor.execute(() -> {
            List<String[]> history = loadConfigHistory();
            // Remove duplicate if exists
            for (int i = history.size() - 1; i >= 0; i--) {
                if (url.equals(history.get(i)[0])) {
                    history.remove(i);
                    break;
                }
            }
            // Add to front
            String[] entry = new String[]{url, String.valueOf(System.currentTimeMillis())};
            history.add(0, entry);
            // Trim to MAX_CONFIG_HISTORY
            if (history.size() > MAX_CONFIG_HISTORY) {
                history = history.subList(0, MAX_CONFIG_HISTORY);
            }
            // Save
            StringBuilder sb = new StringBuilder();
            for (String[] e : history) {
                sb.append(e[0]).append("|||").append(e[1]).append("\n");
            }
            prefsEditor.putString("config_history_list", sb.toString()).apply();
        });
    }

    private List<String[]> loadConfigHistory() {
        String data = prefs.getString("config_history_list", "");
        List<String[]> result = new ArrayList<>();
        if (data.isEmpty()) return result;
        for (String line : data.split("\n")) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|\\|\\|", 2);
            if (parts.length == 2) {
                result.add(parts);
            }
        }
        return result;
    }

    private void showConfigHistoryDialog() {
        executor.execute(() -> {
            List<String[]> history = loadConfigHistory();

            handler.post(() -> {
                if (history.isEmpty()) {
                    Toast.makeText(requireContext(), "暂无配置历史", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] items = new String[history.size()];
                for (int i = 0; i < history.size(); i++) {
                    items[i] = history.get(i)[0] + "\n" + formatDate(Long.parseLong(history.get(i)[1]));
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("配置历史 (最近" + history.size() + ")")
                        .setItems(items, (dialog, which) -> {
                            String url = history.get(which)[0];
                            showConfigSourceDialog();
                            // Pre-fill
                        })
                        .setNeutralButton("清空", (d, w) -> {
                            prefsEditor.remove("config_history_list").apply();
                            showConfigHistoryDialog();
                        })
                        .show();
            });
        });
    }

    private void showPresetSourcesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("预设源列表");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(8), dp(12), dp(8));

        for (String[] preset : PRESET_SOURCES) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(10), 0, dp(10));
            row.setBackgroundColor(0xFF101D2B);
            row.setOnClickListener(v -> {
                saveConfigHistory(preset[1]);
                loadAndSaveRepo(preset[1]);
            });

            TextView nameTv = new TextView(requireContext());
            nameTv.setText(preset[0]);
            nameTv.setTextColor(0xFFE8EDFF);
            nameTv.setTextSize(14);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            nameTv.setLayoutParams(nameParams);

            Button loadBtn = new Button(requireContext());
            loadBtn.setText("加载");
            loadBtn.setBackgroundColor(0xFF7C3AED);
            loadBtn.setTextColor(0xFFFFFFFF);
            loadBtn.setOnClickListener(ev -> {
                saveConfigHistory(preset[1]);
                loadAndSaveRepo(preset[1]);
            });

            row.addView(nameTv);
            row.addView(loadBtn);
            layout.addView(row);
        }

        builder.setView(layout);
        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    private void showScanPlaceholder() {
        Toast.makeText(requireContext(), "扫码功能开发中", Toast.LENGTH_SHORT).show();
    }

    // --- Section 3: 数据采集 (v9.9 style) ---

    private void showCollectionWindow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("数据采集窗口");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Category spinner
        TextView catLabel = new TextView(requireContext());
        catLabel.setText("选择分类:");
        catLabel.setTextColor(0xFFE8EDFF);
        catLabel.setTextSize(14);
        layout.addView(catLabel);

        Spinner categorySpinner = new Spinner(requireContext());
        layout.addView(categorySpinner);

        // Progress bar
        ProgressBar progressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        layout.addView(progressBar);

        // Status text
        TextView statusText = new TextView(requireContext());
        statusText.setText("就绪");
        statusText.setTextColor(0xFF6F7890);
        statusText.setTextSize(12);
        layout.addView(statusText);

        // Buttons
        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button startBtn = new Button(requireContext());
        startBtn.setText("开始采集");
        startBtn.setBackgroundColor(0xFF7C3AED);
        startBtn.setTextColor(0xFFFFFFFF);

        Button stopBtn = new Button(requireContext());
        stopBtn.setText("停止");
        stopBtn.setBackgroundColor(0xFFDC2626);
        stopBtn.setTextColor(0xFFFFFFFF);
        stopBtn.setEnabled(false);

        btnRow.addView(startBtn);
        btnRow.addView(stopBtn);
        layout.addView(btnRow);

        builder.setView(layout);
        final AlertDialog dialog = builder.show();

        // Load categories from all sources
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
            List<String> allCategories = new ArrayList<>();

            if (sources != null) {
                for (SourceEntity src : sources) {
                    try {
                        List<CategoryEntity> cats = db.categoryDao().getBySourceIdBlocking(src.id);
                        if (cats != null) {
                            for (CategoryEntity ce : cats) {
                                if (!allCategories.contains(ce.name)) {
                                    allCategories.add(ce.name);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (allCategories.isEmpty()) {
                allCategories.add("全部");
                allCategories.add("电影");
                allCategories.add("电视剧");
                allCategories.add("综艺");
                allCategories.add("动漫");
                allCategories.add("短剧");
            }

            final List<String> categories = allCategories;

            handler.post(() -> {
                String[] catArray = categories.toArray(new String[0]);
                android.widget.ArrayAdapter<String> adapter =
                        new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, catArray);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);

                startBtn.setOnClickListener(v -> {
                    isCollecting = true;
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    progressBar.setVisibility(View.VISIBLE);
                    statusText.setText("采集中...");
                    progressBar.setProgress(0);

                    collectionThread = new Thread(() -> {
                        String selectedCat = categories.get(categorySpinner.getSelectedItemPosition());
                        collectByCategory(sources, selectedCat, progressBar, statusText, () -> {
                            handler.post(() -> {
                                isCollecting = false;
                                startBtn.setEnabled(true);
                                stopBtn.setEnabled(false);
                                progressBar.setProgress(100);
                                statusText.setText("采集完成");
                            });
                        });
                    });
                    collectionThread.start();
                });

                stopBtn.setOnClickListener(v -> {
                    isCollecting = false;
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    statusText.setText("已停止");
                });
            });
        });
    }

    private void collectByCategory(List<SourceEntity> sources, String category,
                                    ProgressBar progressBar, TextView statusText, Runnable onComplete) {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        int totalSources = sources != null ? sources.size() : 0;
        int processed = 0;

        if (sources == null || sources.isEmpty()) {
            handler.post(() -> Toast.makeText(requireContext(), "暂无仓库，请先添加仓库", Toast.LENGTH_SHORT).show());
            if (onComplete != null) onComplete.run();
            return;
        }

        for (SourceEntity src : sources) {
            if (!isCollecting) break;

            try {
                String baseUrl = src.url;
                if (!baseUrl.contains("/api.php/provide/vod")) {
                    baseUrl = src.base + "/api.php/provide/vod";
                }

                // Load categories first
                List<CategoryInfo> catInfos = MovieRepository.loadCategories(baseUrl);
                if (catInfos == null || catInfos.isEmpty()) continue;

                int targetTypeId = -1;
                if (!"全部".equals(category)) {
                    for (CategoryInfo ci : catInfos) {
                        if (ci.typeName != null && ci.typeName.contains(category)) {
                            targetTypeId = Integer.parseInt(ci.typeId);
                            break;
                        }
                    }
                    if (targetTypeId == -1) continue;
                }

                // Save categories to DB
                for (CategoryInfo ci : catInfos) {
                    try {
                        CategoryEntity ce = new CategoryEntity();
                        ce.sourceId = src.id;
                        ce.name = ci.typeName;
                        ce.rawName = ci.typeName;
                        ce.typeId = ci.typeId;
                        ce.parentId = String.valueOf(ci.typePid);
                        ce.updateTime = System.currentTimeMillis();
                        db.categoryDao().insert(ce);
                    } catch (Exception ignored) {}
                }

                // Collect movies for each category
                for (CategoryInfo ci : catInfos) {
                    if (!isCollecting) break;

                    if ("全部".equals(category) || (ci.typeName != null && ci.typeName.contains(category))) {
                        collectMoviesFromCategory(src, baseUrl, ci, db);
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("Settings", "Collection error for source " + src.name, e);
            }

            processed++;
            final int p = processed;
            final int total = totalSources;
            handler.post(() -> {
                int pct = total > 0 ? (p * 100 / total) : 0;
                progressBar.setProgress(pct);
                statusText.setText("已处理 " + p + "/" + total + ": " + src.name);
            });
        }

        if (onComplete != null) onComplete.run();
    }

    private void collectMoviesFromCategory(SourceEntity src, String baseUrl,
                                            CategoryInfo catInfo, AppDatabase db) {
        try {
            // Page 1 first
            List<MovieItem> movies = MovieRepository.loadMovies(baseUrl, catInfo.typeId, 1);
            if (movies == null || movies.isEmpty()) return;

            for (MovieItem item : movies) {
                if (!isCollecting) return;

                MovieEntity entity = new MovieEntity();
                entity.sourceId = src.id;
                entity.category = catInfo.typeName != null ? catInfo.typeName : catInfo.typeId;
                entity.vodId = item.vodId;
                entity.title = item.title;
                entity.pic = item.pic;
                entity.tag = item.tag;
                entity.type = item.type;
                entity.year = item.year;
                entity.area = item.area;
                entity.actor = item.actor;
                entity.director = item.director;
                entity.score = item.score;
                entity.quality = "";
                entity.play = "";
                entity.desc = item.desc;
                entity.updateTime = System.currentTimeMillis();

                db.movieDao().insert(entity);
            }

            // Try page 2
            List<MovieItem> page2 = MovieRepository.loadMovies(baseUrl, catInfo.typeId, 2);
            if (page2 != null) {
                for (MovieItem item : page2) {
                    if (!isCollecting) return;

                    MovieEntity entity = new MovieEntity();
                    entity.sourceId = src.id;
                    entity.category = catInfo.typeName != null ? catInfo.typeName : catInfo.typeId;
                    entity.vodId = item.vodId;
                    entity.title = item.title;
                    entity.pic = item.pic;
                    entity.tag = item.tag;
                    entity.type = item.type;
                    entity.year = item.year;
                    entity.area = item.area;
                    entity.actor = item.actor;
                    entity.director = item.director;
                    entity.score = item.score;
                    entity.quality = "";
                    entity.play = "";
                    entity.desc = item.desc;
                    entity.updateTime = System.currentTimeMillis();

                    db.movieDao().insert(entity);
                }
            }
        } catch (Exception ignored) {}
    }

    private void startManualCollection() {
        new AlertDialog.Builder(requireContext())
                .setTitle("手动采集")
                .setMessage("将遍历所有仓库的所有分类进行数据采集。这可能需要较长时间，是否继续？")
                .setPositiveButton("开始", (d, w) -> {
                    executor.execute(() -> {
                        try {
                            AppDatabase db = AppDatabase.getInstance(requireContext());
                            List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();

                            if (sources == null || sources.isEmpty()) {
                                handler.post(() -> Toast.makeText(requireContext(), "暂无仓库，请先添加仓库", Toast.LENGTH_SHORT).show());
                                return;
                            }

                            handler.post(() -> Toast.makeText(requireContext(),
                                    "开始采集 " + sources.size() + " 个仓库", Toast.LENGTH_LONG).show());

                            for (SourceEntity src : sources) {
                                if (!isCollecting) break;
                                try {
                                    String baseUrl = src.url;
                                    if (!baseUrl.contains("/api.php/provide/vod")) {
                                        baseUrl = src.base + "/api.php/provide/vod";
                                    }

                                    List<CategoryInfo> catInfos = MovieRepository.loadCategories(baseUrl);
                                    if (catInfos == null || catInfos.isEmpty()) continue;

                                    for (CategoryInfo ci : catInfos) {
                                        if (!isCollecting) break;
                                        collectMoviesFromCategory(src, baseUrl, ci, db);
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("Settings", "Manual collect error", e);
                                }
                            }

                            handler.post(() -> Toast.makeText(requireContext(), "采集完成", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            handler.post(() -> Toast.makeText(requireContext(),
                                    "采集失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // --- Section 4: 数据管理 ---

    private void showDatabaseViewer() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
                List<String> categories = db.categoryDao().getDistinctNames();

                handler.post(() -> {
                    if (sources == null || sources.isEmpty()) {
                        Toast.makeText(requireContext(), "数据库为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Build filter options
                    String[] sourceNames = new String[sources.size()];
                    int[] sourceIds = new int[sources.size()];
                    for (int i = 0; i < sources.size(); i++) {
                        sourceNames[i] = sources.get(i).name;
                        sourceIds[i] = sources.get(i).id;
                    }

                    String[] catNames = categories != null ? categories.toArray(new String[0]) : new String[]{"全部"};
                    int[] catIds = categories != null ? new int[categories.size()] : new int[]{0};
                    for (int i = 0; i < (categories != null ? categories.size() : 0); i++) {
                        catIds[i] = i;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("数据库查看");

                    LinearLayout layout = new LinearLayout(requireContext());
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(dp(16), dp(16), dp(16), dp(16));

                    // Source filter
                    TextView srcLabel = new TextView(requireContext());
                    srcLabel.setText("数据源:");
                    srcLabel.setTextColor(0xFFE8EDFF);
                    layout.addView(srcLabel);

                    Spinner srcSpinner = new Spinner(requireContext());
                    android.widget.ArrayAdapter<String> srcAdapter =
                            new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, sourceNames);
                    srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    srcSpinner.setAdapter(srcAdapter);
                    layout.addView(srcSpinner);

                    // Category filter
                    TextView catLabel = new TextView(requireContext());
                    catLabel.setText("分类:");
                    catLabel.setTextColor(0xFFE8EDFF);
                    layout.addView(catLabel);

                    Spinner catSpinner = new Spinner(requireContext());
                    android.widget.ArrayAdapter<String> catAdapter =
                            new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, catNames);
                    catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    catSpinner.setAdapter(catAdapter);
                    layout.addView(catSpinner);

                    // Pagination controls
                    LinearLayout btnLayout = new LinearLayout(requireContext());
                    btnLayout.setOrientation(LinearLayout.HORIZONTAL);

                    Button prevBtn = new Button(requireContext());
                    prevBtn.setText("< 上一页");
                    prevBtn.setBackgroundColor(0xFF374151);
                    prevBtn.setTextColor(0xFFE8EDFF);

                    Button nextBtn = new Button(requireContext());
                    nextBtn.setText("下一页 >");
                    nextBtn.setBackgroundColor(0xFF374151);
                    nextBtn.setTextColor(0xFFE8EDFF);

                    TextView pageText = new TextView(requireContext());
                    pageText.setText("第 1 页");
                    pageText.setTextColor(0xFFE8EDFF);
                    pageText.setTextSize(12);

                    btnLayout.addView(prevBtn);
                    btnLayout.addView(pageText);
                    btnLayout.addView(nextBtn);
                    layout.addView(btnLayout);

                    // Results area
                    TextView resultsArea = new TextView(requireContext());
                    resultsArea.setTextColor(0xFF9CA3AF);
                    resultsArea.setTextSize(11);
                    resultsArea.setPadding(0, dp(8), 0, 0);
                    layout.addView(resultsArea);

                    builder.setView(layout);
                    final AlertDialog dialog = builder.show();

                    // Load page 1
                    final int[] currentPage = {1};
                    final int pageSize = 20;

                    Runnable loadPage = () -> {
                        int selectedSrc = srcSpinner.getSelectedItemPosition();
                        int selectedCat = catSpinner.getSelectedItemPosition();
                        int sid = sourceIds[selectedSrc];
                        String catFilter = "全部".equals(catNames[selectedCat]) ? "" : catNames[selectedCat];

                        int offset = (currentPage[0] - 1) * pageSize;
                        List<MovieEntity> movies = db.movieDao().getBySourceAndCategory(sid, catFilter, pageSize, offset);
                        int totalCount = db.movieDao().countBySourceAndCategory(sid, catFilter);
                        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

                        StringBuilder sb = new StringBuilder();
                        sb.append("共 ").append(totalCount).append(" 条记录").append("\n\n");
                        if (movies != null) {
                            for (MovieEntity m : movies) {
                                sb.append(m.title).append(" [").append(m.category).append("]\n");
                                sb.append("  ID: ").append(m.vodId).append("  评分: ").append(m.score).append("\n\n");
                            }
                        }

                        pageText.setText("第 " + currentPage[0] + "/" + totalPages + " 页");
                        resultsArea.setText(sb.toString());

                        prevBtn.setEnabled(currentPage[0] > 1);
                        nextBtn.setEnabled(currentPage[0] < totalPages);
                    };

                    prevBtn.setOnClickListener(v -> {
                        if (currentPage[0] > 1) { currentPage[0]--; loadPage.run(); }
                    });

                    nextBtn.setOnClickListener(v -> {
                        int selectedSrc = srcSpinner.getSelectedItemPosition();
                        int selectedCat = catSpinner.getSelectedItemPosition();
                        int sid = sourceIds[selectedSrc];
                        String catFilter = "全部".equals(catNames[selectedCat]) ? "" : catNames[selectedCat];
                        int totalCount = db.movieDao().countBySourceAndCategory(sid, catFilter);
                        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
                        if (currentPage[0] < totalPages) { currentPage[0]++; loadPage.run(); }
                    });

                    srcSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) { currentPage[0] = 1; loadPage.run(); }
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });
                    catSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) { currentPage[0] = 1; loadPage.run(); }
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });

                    loadPage.run();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showClearDatabaseDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清空数据库")
                .setMessage("确定要清空所有采集的影视数据吗？此操作不可恢复。")
                .setPositiveButton("确定", (d, w) -> {
                    executor.execute(() -> {
                        try {
                            AppDatabase db = AppDatabase.getInstance(requireContext());
                            List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
                            if (sources != null) {
                                for (SourceEntity src : sources) {
                                    db.movieDao().deleteBySourceId(src.id);
                                    db.categoryDao().deleteBySourceId(src.id);
                                }
                            }
                            handler.post(() -> Toast.makeText(requireContext(), "数据库已清空", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            handler.post(() -> Toast.makeText(requireContext(), "清空失败", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // --- Section 5: 设置中心 ---

    private void showClearCacheDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清理缓存")
                .setMessage("将清理以下内容:\n- 解析缓存 (parse_cache_*)\n- 播放进度 (play_progress_*)\n- 影视列表缓存 (movie_list_cache_*)\n\n是否继续？")
                .setPositiveButton("清理", (d, w) -> {
                    executor.execute(() -> {
                        try {
                            // Clear shared pref cache keys
                            SharedPreferences cachePrefs = requireContext().getSharedPreferences("movie_list_cache", Context.MODE_PRIVATE);
                            cachePrefs.edit().clear().apply();

                            // Clear parse cache
                            SharedPreferences parseCache = requireContext().getSharedPreferences("parse_cache", Context.MODE_PRIVATE);
                            parseCache.edit().clear().apply();

                            // Clear play progress
                            SharedPreferences playProgress = requireContext().getSharedPreferences("play_progress", Context.MODE_PRIVATE);
                            playProgress.edit().clear().apply();

                            handler.post(() -> Toast.makeText(requireContext(), "缓存已清理", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            handler.post(() -> Toast.makeText(requireContext(), "清理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void exportConfig() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
                List<ParseConfigEntity> parses = db.parseConfigDao().getAllConfigsBlocking();

                JsonObject exportObj = new JsonObject();
                exportObj.addProperty("version", "9.9");
                exportObj.addProperty("exportTime", String.valueOf(System.currentTimeMillis()));

                JsonArray sourcesArr = new JsonArray();
                if (sources != null) {
                    for (SourceEntity src : sources) {
                        JsonObject sObj = new JsonObject();
                        sObj.addProperty("name", src.name);
                        sObj.addProperty("url", src.url);
                        sObj.addProperty("base", src.base);
                        sObj.addProperty("status", src.status);
                        sourcesArr.add(sObj);
                    }
                }
                exportObj.add("sources", sourcesArr);

                JsonArray parsesArr = new JsonArray();
                if (parses != null) {
                    for (ParseConfigEntity p : parses) {
                        JsonObject pObj = new JsonObject();
                        pObj.addProperty("name", p.name);
                        pObj.addProperty("url", p.url);
                        pObj.addProperty("type", p.type);
                        parsesArr.add(pObj);
                    }
                }
                exportObj.add("parses", parsesArr);

                String jsonStr = gson.toJson(exportObj);

                ClipboardManager clipboard =
                        (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("settings_export", jsonStr);
                clipboard.setPrimaryClip(clip);

                handler.post(() -> Toast.makeText(requireContext(), "配置已复制到剪贴板", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(),
                        "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showAboutDialog() {
        String versionName = "v9.9-native";
        try {
            versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception ignored) {}

        String aboutText =
                "个人助理 App\n" +
                "版本: " + versionName + "\n" +
                "架构: Native Android\n" +
                "\n" +
                "功能特性:\n" +
                "- 仓库管理 (支持JSON配置)\n" +
                "- 影视源配置 (8个预设源)\n" +
                "- 数据采集 (按分类抓取)\n" +
                "- Room 数据库存储\n" +
                "- 解析器管理\n" +
                "- 直播源管理\n";

        new AlertDialog.Builder(requireContext())
                .setTitle("关于")
                .setMessage(aboutText)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isCollecting = false;
        if (collectionThread != null) {
            collectionThread.interrupt();
        }
        executor.shutdownNow();
    }
}
