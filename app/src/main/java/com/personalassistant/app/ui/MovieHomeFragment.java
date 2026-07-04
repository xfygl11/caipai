package com.personalassistant.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.personalassistant.app.R;
import com.personalassistant.app.db.ParserSeeder;
import com.bumptech.glide.Glide;
import com.personalassistant.app.data.model.CategoryInfo;
import com.personalassistant.app.data.model.MovieItem;
import com.personalassistant.app.data.model.SiteInfo;
import com.personalassistant.app.data.repository.MovieRepository;
import com.personalassistant.app.data.repository.SiteRepository;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.SourceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MovieHomeFragment extends Fragment {
    private static final String PREFS_KEY_LAST_SITE_ID = "last_site_id";
    private static final String PREFS_KEY_LAST_CATEGORY = "last_category";

    private LinearLayout categoryBar;
    private RecyclerView movieGrid;
    private MovieGridAdapter adapter;
    private EditText searchInput;
    private TextView loadingText;
    private String currentCategory = "";
    private String currentCategoryId = "";
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private long currentSiteId = -1;
    private String currentSiteBase = "";
    private String currentWallpaperUrl = "";
    private android.widget.ImageView wallpaperView;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Disposable historyDisposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0xFF05070D);

        // Wallpaper background
        wallpaperView = new android.widget.ImageView(requireContext());
        wallpaperView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        wallpaperView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));
        wallpaperView.setAlpha(0.3f);
        root.addView(wallpaperView);

        // Search bar
        LinearLayout searchRow = createSearchRow();
        root.addView(searchRow);

        // Settings button row
        LinearLayout settingsRow = new LinearLayout(requireContext());
        settingsRow.setOrientation(LinearLayout.HORIZONTAL);
        settingsRow.setPadding(dp(12), dp(4), dp(12), dp(4));
        settingsRow.setGravity(android.view.Gravity.END);

        Button settingsBtn = new Button(requireContext());
        settingsBtn.setText("\u2699");
        settingsBtn.setTextSize(16);
        settingsBtn.setAllCaps(false);
        settingsBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        settingsBtn.setTextColor(0xFF8899AA);
        settingsBtn.setBackgroundColor(0x0FFFFFFF);
        settingsBtn.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });
        settingsRow.addView(settingsBtn);
        root.addView(settingsRow);

        // Category bar
        categoryBar = new LinearLayout(requireContext());
        categoryBar.setOrientation(LinearLayout.HORIZONTAL);
        categoryBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        categoryBar.setLayoutParams(catParams);
        root.addView(categoryBar);

        // Loading indicator
        loadingText = new TextView(requireContext());
        loadingText.setText("请选择站点和分类");
        loadingText.setTextColor(0xFF667788);
        loadingText.setGravity(android.view.Gravity.CENTER);
        loadingText.setPadding(0, dp(40), 0, 0);
        root.addView(loadingText);

        // Movie grid
        movieGrid = new RecyclerView(requireContext());
        movieGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new MovieGridAdapter();
        adapter.setOnItemClickListener(item -> openDetail(item));
        movieGrid.setAdapter(adapter);
        movieGrid.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        movieGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                GridLayoutManager lm = (GridLayoutManager) rv.getLayoutManager();
                if (lm != null && !isLoading && hasMore) {
                    int lastVisible = lm.findLastCompletelyVisibleItemPosition();
                    int total = lm.getItemCount();
                    if (lastVisible >= total - 4 && lastVisible >= 0) {
                        loadMovies();
                    }
                }
            }
        });
        root.addView(movieGrid);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ParserSeeder.seedIfEmpty(requireContext());
        loadLastUsedSite();
    }

    private void loadLastUsedSite() {
        long lastSiteId = requireContext().getSharedPreferences("app_prefs", 0)
                .getLong(PREFS_KEY_LAST_SITE_ID, -1);

        if (lastSiteId > 0) {
            restoreSite(lastSiteId);
        } else {
            showSitePicker();
        }
    }

    private void showSitePicker() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();

            handler.post(() -> {
                if (sources == null || sources.isEmpty()) {
                    showRepoInputDialog();
                    return;
                }

                String[] names = new String[sources.size() + 1];
                names[0] = "添加新仓库";
                for (int i = 0; i < sources.size(); i++) {
                    names[i + 1] = sources.get(i).name;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("选择站点")
                        .setItems(names, (dialog, which) -> {
                            if (which == 0) {
                                showRepoInputDialog();
                            } else {
                                currentSiteId = sources.get(which - 1).id;
                                currentSiteBase = sources.get(which - 1).base;
                                saveLastSite(currentSiteId);
                                loadCategories();
                            }
                        })
                        .setPositiveButton("管理仓库", (d, w) -> showRepoManageDialog())
                        .show();
            });
        });
    }

    private void showRepoInputDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("输入仓库地址...");
        input.setHintTextColor(0xFF6F7890);
        input.setTextColor(0xFFE8EDFF);
        input.setText("https://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json");
        new AlertDialog.Builder(requireContext())
                .setTitle("添加仓库")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        loadAndSaveRepo(url);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRepoManageDialog() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();

            handler.post(() -> {
                if (sources == null || sources.isEmpty()) {
                    showRepoInputDialog();
                    return;
                }

                String[] items = new String[sources.size()];
                for (int i = 0; i < sources.size(); i++) {
                    items[i] = sources.get(i).name + " (" + sources.get(i).base + ")";
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("仓库管理")
                        .setItems(items, (dialog, which) -> {
                            SourceEntity src = sources.get(which);
                            currentSiteId = src.id;
                            currentSiteBase = src.base;
                            saveLastSite(currentSiteId);
                            loadCategories();
                        })
                        .setPositiveButton("添加", (d, w) -> showRepoInputDialog())
                        .setNeutralButton("删除", (dialog, which) -> {
                            SourceEntity src = sources.get(which);
                            db.sourceDao().deleteById(src.id);
                            Toast.makeText(requireContext(), "已删除: " + src.name, Toast.LENGTH_SHORT).show();
                            showSitePicker();
                        })
                        .show();
            });
        });
    }

    private void loadAndSaveRepo(String repoUrl) {
        loadingText.setText("正在加载仓库...");
        loadingText.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                com.personalassistant.app.data.model.RepoConfig config =
                        SiteRepository.loadRepoConfig(repoUrl);

                if (config == null || config.sites == null || config.sites.isEmpty()) {
                    handler.post(() -> {
                        Toast.makeText(requireContext(), "仓库加载失败或无可用站点", Toast.LENGTH_SHORT).show();
                        loadingText.setVisibility(View.GONE);
                    });
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(requireContext());

                // Save movie sites
                for (SiteInfo site : config.sites) {
                    if (!"MV".equals(site.typeLabel)) continue;
                    if (site.api.isEmpty()) continue;

                    SourceEntity entity = new SourceEntity();
                    entity.name = site.name;
                    entity.url = site.api;
                    entity.base = site.api.replace("/api.php/provide/vod/at/xml/", "")
                            .replace("/api.php/provide/vod/", "");
                    entity.isBuiltin = 0;
                    entity.addedTime = System.currentTimeMillis();
                    entity.lastSyncTime = System.currentTimeMillis();
                    entity.status = 1;

                    // Check if already exists
                    SourceEntity existing = db.sourceDao().getByBaseBlocking(entity.base);
                    if (existing != null) {
                        entity.id = existing.id;
                        db.sourceDao().updateLastSync(entity.id, System.currentTimeMillis());
                    } else {
                        long newId = db.sourceDao().insert(entity);
                        entity.id = (int) newId;
                    }
                }

                // Save wallpaper
                if (config.wallpaper != null && !config.wallpaper.isEmpty()) {
                    currentWallpaperUrl = config.wallpaper;
                    Glide.with(requireContext())
                            .load(config.wallpaper)
                            .centerCrop()
                            .placeholder(new android.graphics.drawable.ColorDrawable(0xFF1a1a2e))
                            .error(new android.graphics.drawable.ColorDrawable(0xFF1a1a2e))
                            .into(wallpaperView);
                }

                // Load categories for first site
                List<SourceEntity> allSources = db.sourceDao().getAllSourcesBlocking();
                if (allSources != null && !allSources.isEmpty()) {
                    SourceEntity first = allSources.get(0);
                    currentSiteId = first.id;
                    currentSiteBase = first.base;
                    saveLastSite(currentSiteId);
                    handler.post(this::loadCategories);
                } else {
                    handler.post(() -> {
                        Toast.makeText(requireContext(), "无可用站点", Toast.LENGTH_SHORT).show();
                        loadingText.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(requireContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingText.setVisibility(View.GONE);
                });
            }
        });
    }

    private void restoreSite(long siteId) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                SourceEntity source = db.sourceDao().getByIdBlocking(siteId);

                if (source == null) {
                    handler.post(this::showSitePicker);
                    return;
                }

                currentSiteId = source.id;
                currentSiteBase = source.base;
                saveLastSite(currentSiteId);
                handler.post(() -> {
                    loadDefaultWallpaper();
                    loadCategories();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    loadDefaultWallpaper();
                    showSitePicker();
                });
            }
        });
    }

    private void loadDefaultWallpaper() {
        handler.post(() -> {
            wallpaperView.setBackgroundColor(0xFF1a1a2e);
        });
    }

    private void saveLastSite(long siteId) {
        requireContext().getSharedPreferences("app_prefs", 0)
                .edit()
                .putLong(PREFS_KEY_LAST_SITE_ID, siteId)
                .apply();
    }

    private void loadCategories() {
        loadingText.setText("加载分类中...");
        loadingText.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                List<CategoryInfo> categories = MovieRepository.loadCategories(currentSiteBase);

                handler.post(() -> {
                    loadingText.setVisibility(View.GONE);
                    if (categories == null || categories.isEmpty()) {
                        loadingText.setText("暂无分类");
                        return;
                    }

                    // Restore last category or use first
                    String lastCat = requireContext().getSharedPreferences("app_prefs", 0)
                            .getString(PREFS_KEY_LAST_CATEGORY, null);

                    if (lastCat != null) {
                        for (CategoryInfo c : categories) {
                            if (c.typeName.equals(lastCat)) {
                                selectCategory(c);
                                return;
                            }
                        }
                    }

                    selectCategory(categories.get(0));
                });
            } catch (Exception e) {
                handler.post(() -> {
                    loadingText.setText("加载失败: " + e.getMessage());
                });
            }
        });
    }

    private void selectCategory(CategoryInfo cat) {
        currentCategory = cat.typeName;
        currentCategoryId = cat.typeId;
        currentPage = 1;
        hasMore = true;
        adapter.setItems(new ArrayList<>());

        // Save last category
        requireContext().getSharedPreferences("app_prefs", 0)
                .edit()
                .putString(PREFS_KEY_LAST_CATEGORY, currentCategory)
                .apply();

        buildCategoryTabs();
        loadMovies();
    }

    private void buildCategoryTabs() {
        categoryBar.removeAllViews();

        // Load categories from DB
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<com.personalassistant.app.db.entity.CategoryEntity> cats =
                    db.categoryDao().getBySourceIdBlocking((int) currentSiteId);

            handler.post(() -> {
                if (cats != null) {
                    for (com.personalassistant.app.db.entity.CategoryEntity ce : cats) {
                        addCategoryTab(ce.name, ce.typeId);
                    }
                }
                if (categoryBar.getChildCount() == 0) {
                    addCategoryTab(currentCategory, currentCategoryId);
                }
            });
        });
    }

    private void addCategoryTab(String name, String typeId) {
        Button btn = new Button(requireContext());
        btn.setText(name);
        btn.setTextSize(11);
        btn.setAllCaps(false);
        btn.setPadding(dp(12), dp(6), dp(12), dp(6));
        boolean active = name.equals(currentCategory);
        btn.setTextColor(active ? 0xFFFFFFFF : 0xFF7E879F);
        btn.setBackgroundColor(active ? 0xFF7C3AED : 0x1A7C3AED);
        btn.setOnClickListener(v -> {
            currentPage = 1;
            hasMore = true;
            adapter.setItems(new ArrayList<>());
            currentCategory = name;
            currentCategoryId = typeId;
            requireContext().getSharedPreferences("app_prefs", 0)
                    .edit()
                    .putString(PREFS_KEY_LAST_CATEGORY, currentCategory)
                    .apply();
            buildCategoryTabs();
            loadMovies();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(6);
        btn.setLayoutParams(params);
        categoryBar.addView(btn);
    }

    private void loadMovies() {
        if (isLoading || !hasMore || currentCategoryId.isEmpty()) return;
        isLoading = true;

        if (loadingText.getVisibility() == View.VISIBLE) {
            loadingText.setText("加载中 " + currentPage + "...");
        }

        executor.execute(() -> {
            try {
                List<MovieItem> items = MovieRepository.loadMovies(
                        currentSiteBase, currentCategoryId, currentPage);

                handler.post(() -> {
                    if (currentPage == 1) adapter.setItems(items);
                    else adapter.addItems(items);
                    hasMore = items.size() >= 20;
                    currentPage++;
                    isLoading = false;
                    if (loadingText.getVisibility() == View.VISIBLE) {
                        loadingText.setVisibility(View.GONE);
                    }
                    if (!hasMore && currentPage > 1) {
                        Toast.makeText(requireContext(), "没有更多了", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(requireContext(),
                            "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isLoading = false;
                    if (loadingText.getVisibility() == View.VISIBLE) {
                        loadingText.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void openDetail(MovieItem item) {
        Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
        intent.putExtra("vod_id", item.vodId);
        intent.putExtra("title", item.title);
        intent.putExtra("pic", item.pic);
        intent.putExtra("type", item.type);
        intent.putExtra("year", item.year);
        intent.putExtra("area", item.area);
        intent.putExtra("actor", item.actor);
        intent.putExtra("director", item.director);
        intent.putExtra("desc", item.desc);
        intent.putExtra("score", item.score);
        intent.putExtra("site_id", currentSiteId);
        intent.putExtra("site_base", currentSiteBase);
        intent.putExtra("site_name", getCurrentSiteName());
        intent.putExtra("play_from", item.playFrom);
        intent.putExtra("play_url", item.playUrl);
        startActivity(intent);
    }

    private String getCurrentSiteName() {
        try {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            SourceEntity src = db.sourceDao().getByIdBlocking(currentSiteId);
            return src != null ? src.name : "";
        } catch (Exception e) {
            return "";
        }
    }

    private LinearLayout createSearchRow() {
        LinearLayout searchRow = new LinearLayout(requireContext());
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setPadding(dp(12), dp(8), dp(12), dp(4));

        searchInput = new EditText(requireContext());
        searchInput.setHint("搜索影视...");
        searchInput.setHintTextColor(0xFF6F7890);
        searchInput.setTextColor(0xFFE8EDFF);
        searchInput.setBackgroundColor(0x1AFFFFFF);
        searchInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        searchInput.setLayoutParams(searchParams);

        Button searchBtn = new Button(requireContext());
        searchBtn.setText("搜索");
        searchBtn.setTextColor(0xFFFFFFFF);
        searchBtn.setBackgroundColor(0xFF7C3AED);
        searchBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
        searchBtn.setOnClickListener(v -> doSearch());

        searchRow.addView(searchInput);
        searchRow.addView(searchBtn);
        return searchRow;
    }

    private void doSearch() {
        String kw = searchInput.getText().toString().trim();
        if (kw.isEmpty()) return;
        if (currentCategoryId.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择站点和分类", Toast.LENGTH_SHORT).show();
            return;
        }

        currentPage = 1;
        hasMore = true;
        adapter.setItems(new ArrayList<>());
        loadingText.setText("搜索中...");
        loadingText.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                List<MovieItem> items = MovieRepository.searchMovies(
                        currentSiteBase, kw, currentPage);
                handler.post(() -> {
                    adapter.setItems(items);
                    hasMore = items.size() >= 20;
                    currentPage++;
                    loadingText.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(requireContext(),
                            "搜索失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingText.setVisibility(View.GONE);
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    static int dp(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}
