package com.personalassistant.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.personalassistant.app.R;
import com.personalassistant.app.data.model.CategoryInfo;
import com.personalassistant.app.data.model.MovieItem;
import com.personalassistant.app.data.repository.MovieRepository;
import com.personalassistant.app.data.repository.SiteRepository;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.SourceEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieHomeFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_SITE_ID = "last_site_id";
    private static final String KEY_SEARCH_HISTORY = "movie_search_history";
    private static final long SEARCH_DEBOUNCE_MS = 500;

    private LinearLayout tvCatBar;
    private RecyclerView tvGrid;
    private FrameLayout tvCatDropdown;
    private GridLayout tvCatGrid;
    private TextView tvSectionName;
    private ImageButton tvCatToggle;
    private TextView tvSourceBtn;
    private EditText tvSearchView;
    private FrameLayout tvSearchHistoryDropdown;
    private LinearLayout skeletonGrid;
    private LinearLayout emptyGuide;
    private LinearLayout errorState;
    private TextView errorStateText;
    private Button errorRetryBtn;
    private TextView tvLoadStatus;
    private LinearLayout tvLoadMoreWrap;
    private Button tvLoadMoreBtn;
    private TextView tvPageInfo;
    private TextView pullRefreshIndicator;
    private MovieGridAdapter adapter;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newFixedThreadPool(2);

    private long currentSiteId = -1;
    private String currentSiteBase = "";
    private String currentTypeId = "";
    private String currentTypeName = "推荐";
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean isSearching = false;
    private String searchKeyword = "";

    private List<CategoryInfo> allCategories = new ArrayList<>();
    private List<String> searchHistory = new ArrayList<>();
    private String debounceQuery = "";
    private Runnable debounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movie_tv, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        bindViews(rootView);
        initRecyclerView();
        loadSearchHistory();
        initSearchView();
        initSourceBtn();
        initCatToggle();
        initLoadMore();
        initPullRefresh(rootView);
        loadLastSiteAndInit();
    }

    private void bindViews(View root) {
        tvCatBar = root.findViewById(R.id.tv_cat_bar);
        tvGrid = root.findViewById(R.id.tv_grid);
        tvCatDropdown = root.findViewById(R.id.tv_cat_dropdown);
        tvCatGrid = root.findViewById(R.id.tv_cat_grid);
        tvSectionName = root.findViewById(R.id.tv_section_name);
        tvCatToggle = root.findViewById(R.id.tv_cat_toggle);
        tvSourceBtn = root.findViewById(R.id.tv_source_btn);
        tvSearchView = root.findViewById(R.id.tv_search_view);
        tvSearchHistoryDropdown = root.findViewById(R.id.tv_search_history_dropdown);
        skeletonGrid = root.findViewById(R.id.skeleton_grid);
        emptyGuide = root.findViewById(R.id.empty_guide);
        errorState = root.findViewById(R.id.error_state);
        errorStateText = root.findViewById(R.id.error_state_text);
        errorRetryBtn = root.findViewById(R.id.error_retry_btn);
        tvLoadStatus = root.findViewById(R.id.tv_load_status);
        tvLoadMoreWrap = root.findViewById(R.id.tv_load_more_wrap);
        tvLoadMoreBtn = root.findViewById(R.id.tv_load_more_btn);
        tvPageInfo = root.findViewById(R.id.tv_page_info);
        pullRefreshIndicator = root.findViewById(R.id.pull_refresh_indicator);
    }

    private void initRecyclerView() {
        adapter = new MovieGridAdapter();
        adapter.setOnItemClickListener(item -> openDetail(item));
        tvGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        tvGrid.setAdapter(adapter);
        tvGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                GridLayoutManager lm = (GridLayoutManager) rv.getLayoutManager();
                if (lm != null && !isLoading && hasMore && dy > 0) {
                    int lastPos = lm.findLastCompletelyVisibleItemPosition();
                    int total = lm.getItemCount();
                    if (lastPos >= total - 4 && lastPos >= 0) {
                        loadMovies();
                    }
                }
            }
        });
    }

    private void initSearchView() {
        tvSearchView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showSearchHistory();
            } else {
                hideSearchHistory();
            }
        });

        tvSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (debounceRunnable != null) {
                    mainHandler.removeCallbacks(debounceRunnable);
                }
                debounceQuery = query;
                if (query.isEmpty()) {
                    hideSearchHistory();
                    if (isSearching) {
                        isSearching = false;
                        resetToListing();
                    }
                    return;
                }
                debounceRunnable = () -> {
                    if (debounceQuery.equals(query) && !query.isEmpty()) {
                        performSearch(query);
                    }
                };
                mainHandler.postDelayed(debounceRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        tvSearchView.setOnEditorActionListener((v, actionId, event) -> {
            String q = v.getText().toString().trim();
            if (!q.isEmpty()) {
                performSearch(q);
            }
            return true;
        });
    }

    private void initSourceBtn() {
        tvSourceBtn.setOnClickListener(v -> showRepoPanel());
    }

    private void initCatToggle() {
        tvCatToggle.setOnClickListener(v -> {
            if (tvCatDropdown.getVisibility() == View.VISIBLE) {
                tvCatDropdown.setVisibility(View.GONE);
            } else {
                tvCatDropdown.setVisibility(View.VISIBLE);
                buildCategoryDropdown();
            }
        });
    }

    private void initLoadMore() {
        tvLoadMoreBtn.setOnClickListener(v -> loadMovies());
    }

    private void initPullRefresh(View root) {
        root.setOnTouchListener((v, event) -> {
            return false;
        });
    }

    private void loadLastSiteAndInit() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        long lastId = prefs.getLong(KEY_LAST_SITE_ID, -1);

        if (lastId > 0) {
            restoreSite(lastId);
        } else {
            showSitePicker();
        }
    }

    private void restoreSite(long siteId) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                SourceEntity src = db.sourceDao().getByIdBlocking(siteId);
                mainHandler.post(() -> {
                    if (src != null) {
                        currentSiteId = src.id;
                        currentSiteBase = src.base;
                        loadCategories();
                    } else {
                        showSitePicker();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(this::showSitePicker);
            }
        });
    }

    private void showSitePicker() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
            mainHandler.post(() -> {
                if (sources == null || sources.isEmpty()) {
                    showRepoInputDialog();
                    return;
                }
                String[] names = new String[sources.size() + 1];
                names[0] = "添加新仓库";
                for (int i = 0; i < sources.size(); i++) {
                    names[i + 1] = sources.get(i).name;
                }
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("选择站点")
                        .setItems(names, (dialog, which) -> {
                            if (which == 0) {
                                showRepoInputDialog();
                            } else {
                                SourceEntity sel = sources.get(which - 1);
                                currentSiteId = sel.id;
                                currentSiteBase = sel.base;
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
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
            mainHandler.post(() -> {
                if (sources == null || sources.isEmpty()) {
                    showRepoInputDialog();
                    return;
                }
                String[] items = new String[sources.size()];
                for (int i = 0; i < sources.size(); i++) {
                    items[i] = sources.get(i).name + " (" + sources.get(i).base + ")";
                }
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
                            requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE)
                                    .edit().remove(KEY_LAST_SITE_ID).apply();
                            ToastCompat.show(requireContext(), "已删除: " + src.name);
                            showSitePicker();
                        })
                        .show();
            });
        });
    }

    private void loadAndSaveRepo(String repoUrl) {
        executor.execute(() -> {
            try {
                com.personalassistant.app.data.model.RepoConfig config =
                        SiteRepository.loadRepoConfig(repoUrl);
                mainHandler.post(() -> {
                    if (config == null || config.sites == null || config.sites.isEmpty()) {
                        ToastCompat.show(requireContext(), "仓库加载失败或无可用站点");
                        return;
                    }
                });
                if (config == null || config.sites == null || config.sites.isEmpty()) return;

                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<SourceEntity> saved = new ArrayList<>();

                for (com.personalassistant.app.data.model.SiteInfo site : config.sites) {
                    if (!"MV".equals(site.typeLabel)) continue;
                    if (site.api.isEmpty()) continue;

                    SourceEntity entity = new SourceEntity();
                    entity.name = site.name;
                    entity.url = site.api;
                    entity.base = extractSiteBase(site.api);
                    entity.isBuiltin = 0;
                    entity.addedTime = System.currentTimeMillis();
                    entity.lastSyncTime = System.currentTimeMillis();
                    entity.status = 1;

                    SourceEntity existing = db.sourceDao().getByBaseBlocking(entity.base);
                    if (existing != null) {
                        entity.id = existing.id;
                        db.sourceDao().updateLastSync(entity.id, System.currentTimeMillis());
                    } else {
                        long newId = db.sourceDao().insert(entity);
                        entity.id = (int) newId;
                    }
                    saved.add(entity);
                }

                if (!saved.isEmpty()) {
                    SourceEntity first = saved.get(0);
                    currentSiteId = first.id;
                    currentSiteBase = first.base;
                    saveLastSite(currentSiteId);
                    mainHandler.post(this::loadCategories);
                } else {
                    mainHandler.post(() ->
                            ToastCompat.show(requireContext(), "无可用站点"));
                }
            } catch (Exception e) {
                final String msg = "加载失败: " + e.getMessage();
                mainHandler.post(() -> ToastCompat.show(requireContext(), msg));
            }
        });
    }

    private void saveLastSite(long siteId) {
        requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE)
                .edit().putLong(KEY_LAST_SITE_ID, siteId).apply();
    }

    private void loadCategories() {
        showSkeleton(true);
        executor.execute(() -> {
            try {
                List<CategoryInfo> cats = MovieRepository.loadCategories(currentSiteBase);
                mainHandler.post(() -> {
                    showSkeleton(false);
                    if (cats == null || cats.isEmpty()) {
                        showError("暂无分类数据");
                        return;
                    }
                    allCategories = cats;
                    buildCategoryTabs(cats);
                    selectDefaultCategory(cats);
                });
            } catch (Exception e) {
                mainHandler.post(() -> showError("加载分类失败: " + e.getMessage()));
            }
        });
    }

    private void buildCategoryTabs(List<CategoryInfo> cats) {
        tvCatBar.removeAllViews();
        tvCatDropdown.setVisibility(View.GONE);

        for (CategoryInfo cat : cats) {
            TextView tab = new TextView(requireContext());
            tab.setText(cat.typeName);
            tab.setTextSize(12);
            tab.setTextColor(0xFF7E879F);
            tab.setPadding(dp(10), dp(6), dp(10), dp(6));
            tab.setTag(cat.typeId);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(4);
            tab.setLayoutParams(params);
            tab.setOnClickListener(v -> {
                String tid = (String) ((Button)v).getText().toString();
                String tname = (String) v.getTag();
                selectCategory(tid, tname);
                tvCatDropdown.setVisibility(View.GONE);
            });
            tvCatBar.addView(tab);
        }
    }

    private void selectDefaultCategory(List<CategoryInfo> cats) {
        if (cats.isEmpty()) return;
        CategoryInfo first = cats.get(0);
        selectCategory(first.typeId, first.typeName);
    }

    private void selectCategory(String typeId, String typeName) {
        currentTypeId = typeId;
        currentTypeName = typeName;
        currentPage = 1;
        hasMore = true;
        isSearching = false;
        searchKeyword = "";
        tvSearchView.setText("");
        adapter.setItems(new ArrayList<>());

        for (int i = 0; i < tvCatBar.getChildCount(); i++) {
            View child = tvCatBar.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                boolean active = tv.getTag().equals(typeId);
                tv.setTextColor(active ? 0xFFFFFFFF : 0xFF7E879F);
                tv.setBackgroundColor(active ? 0xFF2196F3 : 0x0FFFFFFF);
            }
        }

        tvSectionName.setText(typeName);
        loadMovies();
    }

    private void buildCategoryDropdown() {
        tvCatGrid.removeAllViews();
        if (allCategories == null || allCategories.isEmpty()) return;

        for (CategoryInfo cat : allCategories) {
            TextView catBtn = new TextView(requireContext());
            catBtn.setText(cat.typeName);
            catBtn.setTextSize(12);
            catBtn.setTextColor(0xFFB0B8CC);
            catBtn.setGravity(android.view.Gravity.CENTER);
            catBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
            catBtn.setBackgroundColor(0x0FFFFFFF);
            catBtn.setClickable(true);
            catBtn.setTag(cat.typeId);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            catBtn.setLayoutParams(lp);

            catBtn.setOnClickListener(v -> {
                String tid = (String) ((TextView)v).getText().toString();
                String tname = (String) v.getTag();
                selectCategory(tid, tname);
                tvCatDropdown.setVisibility(View.GONE);
            });
            tvCatGrid.addView(catBtn);
        }
    }

    private void loadMovies() {
        if (isLoading || !hasMore) return;
        if (currentTypeId.isEmpty() && !isSearching) return;
        isLoading = true;

        tvLoadStatus.setText(isSearching ? "搜索中 " + currentPage + "..." : "加载中 " + currentPage + "...");
        tvLoadStatus.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            List<MovieItem> items;
            try {
                if (isSearching) {
                    items = MovieRepository.searchMovies(currentSiteBase, searchKeyword, currentPage);
                } else {
                    items = MovieRepository.loadMovies(currentSiteBase, currentTypeId, currentPage);
                }
            } catch (Exception e) {
                final String err = e.getMessage();
                mainHandler.post(() -> {
                    isLoading = false;
                    tvLoadStatus.setVisibility(View.GONE);
                    showError(err != null ? err : "加载失败");
                });
                return;
            }

            final List<MovieItem> result = items != null ? items : new ArrayList<>();
            mainHandler.post(() -> {
                if (currentPage == 1) {
                    adapter.setItems(result);
                } else {
                    adapter.addItems(result);
                }
                hasMore = result.size() >= 20;
                currentPage++;
                isLoading = false;
                tvLoadStatus.setVisibility(View.GONE);
                updateLoadMoreUI();
                updateStates();
            });
        });
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) return;
        isSearching = true;
        searchKeyword = keyword;
        currentPage = 1;
        hasMore = true;
        adapter.setItems(new ArrayList<>());
        addSearchHistory(keyword);
        tvSectionName.setText("搜索: " + keyword);
        loadMovies();
    }

    private void resetToListing() {
        isSearching = false;
        searchKeyword = "";
        currentPage = 1;
        hasMore = true;
        if (!currentTypeId.isEmpty()) {
            tvSectionName.setText(currentTypeName);
            loadMovies();
        }
    }

    private void loadSearchHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet(KEY_SEARCH_HISTORY, null);
        if (historySet != null) {
            searchHistory = new ArrayList<>(historySet);
        }
    }

    private void addSearchHistory(String keyword) {
        if (keyword.isEmpty()) return;
        searchHistory.remove(keyword);
        searchHistory.add(0, keyword);
        if (searchHistory.size() > 10) {
            searchHistory = searchHistory.subList(0, 10);
        }
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_SEARCH_HISTORY, new HashSet<>(searchHistory)).apply();
    }

    private void showSearchHistory() {
        if (searchHistory.isEmpty()) return;
        tvSearchHistoryDropdown.removeAllViews();

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(14), dp(8), dp(14), dp(8));
        container.setBackgroundColor(0xFF0F1923);

        for (String hist : searchHistory) {
            TextView item = new TextView(requireContext());
            item.setText(hist);
            item.setTextSize(12);
            item.setTextColor(0xFFB0B8CC);
            item.setPadding(dp(8), dp(8), dp(8), dp(8));
            item.setOnClickListener(v -> {
                tvSearchView.setText(hist);
                tvSearchView.setSelection(hist.length());
                hideSearchHistory();
                performSearch(hist);
            });
            item.setOnLongClickListener(v -> {
                removeSearchHistoryItem(hist);
                return true;
            });
            container.addView(item);
        }

        tvSearchHistoryDropdown.addView(container);
        tvSearchHistoryDropdown.setVisibility(View.VISIBLE);
    }

    private void hideSearchHistory() {
        tvSearchHistoryDropdown.setVisibility(View.GONE);
    }

    private void removeSearchHistoryItem(String keyword) {
        searchHistory.remove(keyword);
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_SEARCH_HISTORY, new HashSet<>(searchHistory)).apply();
        if (searchHistory.isEmpty()) {
            hideSearchHistory();
        } else {
            showSearchHistory();
        }
    }

    private void showRepoPanel() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
            mainHandler.post(() -> {
                if (sources == null || sources.isEmpty()) {
                    showSitePicker();
                    return;
                }
                String[] names = new String[sources.size()];
                for (int i = 0; i < sources.size(); i++) {
                    names[i] = sources.get(i).name;
                }
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("选择站点")
                        .setItems(names, (dialog, which) -> {
                            SourceEntity sel = sources.get(which);
                            currentSiteId = sel.id;
                            currentSiteBase = sel.base;
                            saveLastSite(currentSiteId);
                            allCategories.clear();
                            currentTypeId = "";
                            adapter.setItems(new ArrayList<>());
                            tvCatBar.removeAllViews();
                            tvSectionName.setText("请选择分类");
                            showEmptyGuide();
                            loadCategories();
                        })
                        .setPositiveButton("管理仓库", (d, w) -> showRepoManageDialog())
                        .setNegativeButton("取消", null)
                        .show();
            });
        });
    }

    private void updateLoadMoreUI() {
        if (hasMore && currentPage > 1) {
            tvLoadMoreWrap.setVisibility(View.VISIBLE);
            tvPageInfo.setText("第" + (currentPage - 1) + "页");
        } else {
            tvLoadMoreWrap.setVisibility(View.GONE);
        }
    }

    private void updateStates() {
        int count = adapter.getItemCount();
        if (count == 0 && !isLoading && !isSearching) {
            showEmptyGuide();
        } else if (count > 0) {
            hideAllStates();
        }
    }

    private void showSkeleton(boolean show) {
        mainHandler.post(() -> {
            skeletonGrid.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                emptyGuide.setVisibility(View.GONE);
                errorState.setVisibility(View.GONE);
                tvGrid.setVisibility(View.GONE);
            } else {
                tvGrid.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showError(String msg) {
        mainHandler.post(() -> {
            hideAllStates();
            skeletonGrid.setVisibility(View.GONE);
            emptyGuide.setVisibility(View.GONE);
            errorState.setVisibility(View.VISIBLE);
            errorStateText.setText(msg);
            errorRetryBtn.setOnClickListener(v -> {
                errorState.setVisibility(View.GONE);
                if (isSearching) {
                    loadMovies();
                } else {
                    loadCategories();
                }
            });
        });
    }

    private void showEmptyGuide() {
        mainHandler.post(() -> {
            hideAllStates();
            skeletonGrid.setVisibility(View.GONE);
            tvGrid.setVisibility(View.GONE);
            emptyGuide.setVisibility(View.VISIBLE);
        });
    }

    private void hideAllStates() {
        skeletonGrid.setVisibility(View.GONE);
        emptyGuide.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
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

    private String extractSiteBase(String apiUrl) {
        if (apiUrl == null || apiUrl.isEmpty()) return "";
        String base = apiUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String[] suffixes = {
            "/api.php/provide/vod/at/xml",
            "/api.php/provide/vod",
            "/api.php/provide",
            "/api.php",
            "/huoshan.php",
            "/xiong.php",
            "/sb.php",
            "/jinja.php",
            "/appline.php",
            "/chaojijingxuan.php",
            "/cj.php",
            "/v2.php",
            "/index.php",
        };
        for (String suf : suffixes) {
            if (base.endsWith(suf)) {
                base = base.substring(0, base.length() - suf.length());
                break;
            }
        }
        return base;
    }

    private int dp(int val) {
        return (int) (val * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (debounceRunnable != null) {
            mainHandler.removeCallbacks(debounceRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceRunnable != null) {
            mainHandler.removeCallbacks(debounceRunnable);
        }
        executor.shutdownNow();
    }

    private static class ToastCompat {
        static void show(android.content.Context ctx, String msg) {
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
