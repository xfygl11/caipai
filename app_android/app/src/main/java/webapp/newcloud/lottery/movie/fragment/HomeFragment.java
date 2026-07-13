package webapp.newcloud.lottery.movie.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import webapp.newcloud.lottery.movie.MainActivity;
import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.SpiderEngine;
import webapp.newcloud.lottery.movie.adapter.MovieGridAdapter;
import webapp.newcloud.lottery.movie.model.Movie;
import webapp.newcloud.lottery.movie.model.SiteConfig;
import webapp.newcloud.lottery.movie.util.HttpClient;
import webapp.newcloud.lottery.movie.util.DbHelper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class HomeFragment extends Fragment implements MovieGridAdapter.OnMovieClickListener {

    private RecyclerView tvGrid;
    private TextView tvSectionName;
    private TextView tvSiteName;
    private TextView movieLoadStatus;
    private LinearLayout skeletonGrid;
    private LinearLayout emptyGuide;
    private LinearLayout errorState;
    private TextView errorStateText;
    private Button errorRetryBtn;
    private LinearLayout tvLoadMoreWrap;
    private TextView tvLoadMoreBtn;
    private TextView tvPageInfo;

    private MovieGridAdapter adapter;
    private List<Movie> movieList = new ArrayList<>();
    private List<String> categories = new ArrayList<>(Collections.singletonList("推荐"));
    private List<String> allCategories = new ArrayList<>(Collections.singletonList("推荐"));
    private String currentCategory = "推荐";
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String currentSiteApi = "";
    private Map<String, String> categoryIdCache = new HashMap<>();
    private SiteConfig currentSite;
    private Map<String, String> typeIdByName = new HashMap<>();

    private final HttpClient httpClient = new HttpClient();
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.util.Log.d("HomeFragment", "=== onCreateView called ===");
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        android.util.Log.d("HomeFragment", "=== inflated view: " + (view != null) + " ===");
        if (view != null) {
            android.util.Log.d("HomeFragment", "  view widthSpec=" + View.MeasureSpec.getMode(View.MeasureSpec.getSize(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))) + ", heightSpec=" + View.MeasureSpec.getMode(View.MeasureSpec.getSize(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))));
            android.util.Log.d("HomeFragment", "  container=" + (container != null ? "not-null" : "null"));
        }
        
        tvGrid = view.findViewById(R.id.tvGrid);
        tvSectionName = view.findViewById(R.id.tvSectionName);
        tvSiteName = view.findViewById(R.id.tvSiteName);
        movieLoadStatus = view.findViewById(R.id.movieLoadStatus);
        skeletonGrid = view.findViewById(R.id.skeletonGrid);
        emptyGuide = view.findViewById(R.id.emptyGuide);
        errorState = view.findViewById(R.id.errorState);
        errorStateText = view.findViewById(R.id.errorStateText);
        errorRetryBtn = view.findViewById(R.id.errorRetryBtn);
        tvLoadMoreWrap = view.findViewById(R.id.tvLoadMoreWrap);
        tvLoadMoreBtn = view.findViewById(R.id.tvLoadMoreBtn);
        tvPageInfo = view.findViewById(R.id.tvPageInfo);

        android.util.Log.d("HomeFragment", "  tvGrid=" + (tvGrid != null ? "found" : "NOT FOUND"));
        android.util.Log.d("HomeFragment", "  tvSectionName=" + (tvSectionName != null ? "found" : "NOT FOUND"));
        android.util.Log.d("HomeFragment", "  tvSiteName=" + (tvSiteName != null ? "found" : "NOT FOUND"));
        android.util.Log.d("HomeFragment", "  emptyGuide=" + (emptyGuide != null ? "found" : "NOT FOUND"));
        android.util.Log.d("HomeFragment", "  errorState=" + (errorState != null ? "found" : "NOT FOUND"));
        
        setupRecyclerView();
        setupListeners();
        setupSiteNameClick();
        
        android.util.Log.d("HomeFragment", "=== onCreateView returning ===");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("HomeFragment", "=== onResume called ===");
        if (getView() != null) {
            android.util.Log.d("HomeFragment", "  getView() not-null, width=" + getView().getWidth() + ", height=" + getView().getHeight());
            android.util.Log.d("HomeFragment", "  getView().getVisibility()=" + getView().getVisibility());
            android.util.Log.d("HomeFragment", "  getView().getAlpha()=" + getView().getAlpha());
            android.util.Log.d("HomeFragment", "  getView().isShown()=" + getView().isShown());
            android.util.Log.d("HomeFragment", "  getView().getMeasuredWidth()=" + getView().getMeasuredWidth());
            android.util.Log.d("HomeFragment", "  getView().getMeasuredHeight()=" + getView().getMeasuredHeight());
        } else {
            android.util.Log.d("HomeFragment", "  getView() is NULL!");
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        android.util.Log.d("HomeFragment", "=== onHiddenChanged: hidden=" + hidden + " ===");
        if (getView() != null) {
            android.util.Log.d("HomeFragment", "  getView() visible, isShown=" + getView().isShown());
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        android.util.Log.d("HomeFragment", "=== setUserVisibleHint: isVisibleToUser=" + isVisibleToUser + " ===");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        android.util.Log.d("HomeFragment", "onViewCreated called, view=" + view);
        loadData();
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        tvGrid.setLayoutManager(layoutManager);
        adapter = new MovieGridAdapter(movieList, this);
        tvGrid.setAdapter(adapter);
        tvGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && hasMore && !isLoading) {
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    int total = layoutManager.getItemCount();
                    if (lastVisible >= total - 3) {
                        loadNextPage();
                    }
                }
            }
        });
    }

    private void setupListeners() {
        tvLoadMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNextPage();
            }
        });
    }

    private void setupSiteNameClick() {
        if (tvSiteName != null) {
            tvSiteName.setOnClickListener(v -> {
                if (siteNameClickListener != null) {
                    siteNameClickListener.onSiteNameClick(v);
                }
            });
        }
    }

    private void loadData() {
        Activity activity = getActivity();
        if (!(activity instanceof MainActivity)) return;
        MainActivity mainActivity = (MainActivity) activity;
        mainActivity.getCurrentSite(new MainActivity.CurrentSiteCallback() {
            @Override
            public void onSiteLoaded(SiteConfig site) {
                    currentSite = site;
                    if (tvSiteName != null) {
                        if (site != null) {
                            tvSiteName.setText("当前站点: " + site.name);
                        } else {
                            tvSiteName.setText("点击选择站点");
                        }
                    }
                    if (site == null) {
                        showEmptyGuide();
                        return;
                    }
                    
                    // Set current spider for SpiderEngine
                    String pluginKey = !site.key.isEmpty() ? site.key : site.name;
                    SpiderEngine.getInstance().setCurrentSpider(pluginKey);
                    
                    currentSiteApi = site.api;
                    
                    // For type=3 (JS Spider), use SpiderEngine instead of direct HTTP
                    if (site.type == 3) {
                        tvSectionName.setVisibility(View.VISIBLE);
                        tvSectionName.setText("推荐");
                        loadHomeVideoViaSpider();
                    } else if (!currentSiteApi.isEmpty()) {
                        tvSectionName.setVisibility(View.VISIBLE);
                        tvSectionName.setText("推荐");
                        detectAndLoadCategories(site, currentSiteApi);
                        loadMovieList();
                    } else {
                        showEmptyGuide();
                    }
                }
            });
    }

    private void loadHomeVideoViaSpider() {
        if (isLoading) return;
        isLoading = true;
        currentPage = 1;
        movieList.clear();
        showSkeleton();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject result = SpiderEngine.getInstance().homeVideo();
                    final List<Movie> movies = new ArrayList<>();
                    
                    if (result != null) {
                        JSONArray listArray = result.optJSONArray("list");
                        if (listArray == null) {
                            listArray = result.optJSONArray("vods");
                        }
                        
                        if (listArray != null) {
                            for (int i = 0; i < listArray.length(); i++) {
                                JSONObject vod = listArray.getJSONObject(i);
                                Movie movie = normalizeVodJson(vod);
                                if (movie != null && !movie.title.isEmpty()) {
                                    movies.add(movie);
                                }
                            }
                        }
                    }

                    final List<Movie> finalMovies = movies;
                    final boolean hasData = !movies.isEmpty();

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            
                            if (hasData) {
                                movieList.clear();
                                movieList.addAll(movies);
                                adapter.update(movies);
                                tvSectionName.setText("推荐");
                                tvLoadMoreWrap.setVisibility(View.GONE);
                                hasMore = false;
                                emptyGuide.setVisibility(View.GONE);
                                errorState.setVisibility(View.GONE);
                                movieLoadStatus.setVisibility(View.GONE);
                            } else {
                                emptyGuide.setVisibility(View.VISIBLE);
                                tvSectionName.setText("推荐 (无数据)");
                            }
                        }
                    });
                } catch (final Exception e) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            showError("加载推荐失败: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void loadMovieList() {
        if (isLoading || currentSiteApi.isEmpty()) return;
        isLoading = true;
        currentPage = 1;
        movieList.clear();
        showSkeleton();

        String url;
        if ("推荐".equals(currentCategory)) {
            url = currentSiteApi + "?ac=detail&pg=" + currentPage;
        } else {
            String typeId = categoryIdCache.get(currentCategory);
            if (typeId != null && !typeId.isEmpty()) {
                url = currentSiteApi + "?ac=detail&t=" + typeId + "&pg=" + currentPage;
            } else {
                url = currentSiteApi + "?ac=detail&pg=" + currentPage;
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String response = httpClient.httpGet(url);
                    if (response == null || response.isEmpty()) {
                        throw new RuntimeException("空响应");
                    }
                    final JsonObject data = gson.fromJson(response, JsonObject.class);
                    
                    List<Movie> movies = new ArrayList<>();
                    if (data != null && !data.isJsonNull()) {
                        JsonArray listArray = data.getAsJsonArray("list");
                        if (listArray == null) {
                            JsonObject dataObj = data.getAsJsonObject("data");
                            if (dataObj != null && !dataObj.isJsonNull()) {
                                listArray = dataObj.getAsJsonArray("list");
                            }
                        }
                        
                        if (listArray != null) {
                            for (JsonElement element : listArray) {
                                if (element.isJsonObject()) {
                                    JsonObject vod = element.getAsJsonObject();
                                    Movie movie = normalizeVod(vod);
                                    if (movie != null && !movie.title.isEmpty()) {
                                        movies.add(movie);
                                    }
                                }
                            }
                        }
                    }

                    final List<Movie> finalMovies = movies;
                    final boolean hasData = !movies.isEmpty();

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            
                            if (hasData) {
                                movieList.clear();
                                movieList.addAll(movies);
                                adapter.update(movies);
                                tvSectionName.setText(currentCategory);
                                tvLoadMoreWrap.setVisibility(View.VISIBLE);
                                hasMore = movies.size() >= 12;
                                if (hasMore) {
                                    tvPageInfo.setText("第 " + currentPage + " 页");
                                }
                                emptyGuide.setVisibility(View.GONE);
                                errorState.setVisibility(View.GONE);
                                movieLoadStatus.setVisibility(View.GONE);
                            } else {
                                emptyGuide.setVisibility(View.VISIBLE);
                                tvSectionName.setText(currentCategory);
                            }
                        }
                    });
                } catch (final Exception e) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            showError("加载失败: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void loadNextPage() {
        if (isLoading || !hasMore) return;
        currentPage++;
        isLoading = true;
        
        // For type=3 sites, use spider category
        if (currentSite != null && currentSite.type == 3) {
            loadCategoryViaSpider();
            return;
        }
        
        String url;
        if ("推荐".equals(currentCategory)) {
            url = currentSiteApi + "?ac=detail&pg=" + currentPage;
        } else {
            String typeId = categoryIdCache.get(currentCategory);
            if (typeId != null && !typeId.isEmpty()) {
                url = currentSiteApi + "?ac=detail&t=" + typeId + "&pg=" + currentPage;
            } else {
                url = currentSiteApi + "?ac=detail&pg=" + currentPage;
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String response = httpClient.httpGet(url);
                    final JsonObject data = gson.fromJson(response, JsonObject.class);
                    
                    List<Movie> movies = new ArrayList<>();
                    if (data != null) {
                        JsonArray listArray = data.getAsJsonArray("list");
                        if (listArray == null) {
                            JsonObject dataObj = data.getAsJsonObject("data");
                            if (dataObj != null) {
                                listArray = dataObj.getAsJsonArray("list");
                            }
                        }
                        if (listArray != null) {
                            for (JsonElement element : listArray) {
                                JsonObject vod = element.getAsJsonObject();
                                Movie movie = normalizeVod(vod);
                                if (movie != null && !movie.title.isEmpty()) {
                                    movies.add(movie);
                                }
                            }
                        }
                    }

                    final List<Movie> finalMovies = movies;
                    final boolean hasData = !movies.isEmpty();

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            if (hasData) {
                                movieList.addAll(movies);
                                adapter.notifyDataSetChanged();
                                hasMore = movies.size() >= 12;
                                tvPageInfo.setText("第 " + currentPage + " 页");
                            } else {
                                hasMore = false;
                            }
                        }
                    });
                } catch (final Exception e) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                        }
                    });
                }
            }
        }).start();
    }

    private Movie normalizeVod(JsonObject vod) {
        if (vod == null) return null;
        Movie movie = new Movie();
        movie.id = UUID.randomUUID().toString();
        movie.vodId = vod.has("id") ? vod.get("id").getAsString() :
                      vod.has("vod_id") ? vod.get("vod_id").getAsString() :
                      vod.has("vodId") ? vod.get("vodId").getAsString() : "";
        movie.title = vod.has("title") ? vod.get("title").getAsString() :
                      vod.has("vod_name") ? vod.get("vod_name").getAsString() : "";
        movie.pic = vod.has("pic") ? vod.get("pic").getAsString() :
                    vod.has("vod_pic") ? vod.get("vod_pic").getAsString() : "";
        movie.tag = vod.has("tag") ? vod.get("tag").getAsString() :
                    vod.has("vod_tag") ? vod.get("vod_tag").getAsString() : "";
        movie.type = vod.has("type") ? vod.get("type").getAsString() : "";
        movie.year = vod.has("year") ? vod.get("year").getAsString() :
                     vod.has("vod_year") ? vod.get("vod_year").getAsString() : "";
        movie.area = vod.has("area") ? vod.get("area").getAsString() : "";
        movie.actor = vod.has("actor") ? vod.get("actor").getAsString() :
                      vod.has("vod_actor") ? vod.get("vod_actor").getAsString() : "";
        movie.director = vod.has("director") ? vod.get("director").getAsString() :
                         vod.has("vod_director") ? vod.get("vod_director").getAsString() : "";
        movie.score = vod.has("score") ? vod.get("score").getAsString() : "";
        movie.quality = vod.has("quality") ? vod.get("quality").getAsString() : "";
        movie.play = vod.has("play") ? vod.get("play").getAsString() :
                     vod.has("vod_play_url") ? vod.get("vod_play_url").getAsString() : "";
        movie.desc = vod.has("desc") ? vod.get("desc").getAsString() :
                     vod.has("vod_content") ? vod.get("vod_content").getAsString() : "";
        movie.createdAt = System.currentTimeMillis();
        return movie;
    }

    private Movie normalizeVodJson(JSONObject vod) {
        if (vod == null) return null;
        Movie movie = new Movie();
        movie.id = UUID.randomUUID().toString();
        movie.vodId = vod.has("id") ? vod.optString("id") :
                      vod.has("vod_id") ? vod.optString("vod_id") :
                      vod.has("vodId") ? vod.optString("vodId") : "";
        movie.title = vod.has("title") ? vod.optString("title") :
                      vod.has("vod_name") ? vod.optString("vod_name") : "";
        movie.pic = vod.has("pic") ? vod.optString("pic") :
                    vod.has("vod_pic") ? vod.optString("vod_pic") : "";
        movie.tag = vod.has("tag") ? vod.optString("tag") :
                    vod.has("vod_tag") ? vod.optString("vod_tag") : "";
        movie.type = vod.has("type") ? vod.optString("type") : "";
        movie.year = vod.has("year") ? vod.optString("year") :
                      vod.has("vod_year") ? vod.optString("vod_year") : "";
        movie.area = vod.has("area") ? vod.optString("area") : "";
        movie.actor = vod.has("actor") ? vod.optString("actor") :
                      vod.has("vod_actor") ? vod.optString("vod_actor") : "";
        movie.director = vod.has("director") ? vod.optString("director") :
                         vod.has("vod_director") ? vod.optString("vod_director") : "";
        movie.score = vod.has("score") ? vod.optString("score") : "";
        movie.quality = vod.has("quality") ? vod.optString("quality") : "";
        movie.play = vod.has("play") ? vod.optString("play") :
                      vod.has("vod_play_url") ? vod.optString("vod_play_url") : "";
        movie.desc = vod.has("desc") ? vod.optString("desc") :
                      vod.has("vod_content") ? vod.optString("vod_content") : "";
        movie.createdAt = System.currentTimeMillis();
        return movie;
    }

    /**
     * Load category content via SpiderEngine for type=3 sites.
     */
    private void loadCategoryViaSpider() {
        if (isLoading) return;
        isLoading = true;
        currentPage = 1;
        movieList.clear();
        showSkeleton();

        String typeId = categoryIdCache.get(currentCategory);
        final String tid = typeId != null && !typeId.isEmpty() ? typeId : "0";

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final java.util.Map<String, String> extend = new HashMap<>();
                    final JSONObject result = SpiderEngine.getInstance().category(tid, currentPage, false, extend);
                    final List<Movie> movies = new ArrayList<>();
                    
                    if (result != null) {
                        JSONArray listArray = result.optJSONArray("list");
                        if (listArray != null) {
                            for (int i = 0; i < listArray.length(); i++) {
                                JSONObject vod = listArray.getJSONObject(i);
                                Movie movie = normalizeVodJson(vod);
                                if (movie != null && !movie.title.isEmpty()) {
                                    movies.add(movie);
                                }
                            }
                        }
                    }

                    final List<Movie> finalMovies = movies;
                    final boolean hasData = !movies.isEmpty();

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            
                            if (hasData) {
                                movieList.clear();
                                movieList.addAll(movies);
                                adapter.update(movies);
                                tvSectionName.setText(currentCategory);
                                tvLoadMoreWrap.setVisibility(View.VISIBLE);
                                hasMore = movies.size() >= 12;
                                if (hasMore) {
                                    tvPageInfo.setText("第 " + currentPage + " 页");
                                }
                                emptyGuide.setVisibility(View.GONE);
                                errorState.setVisibility(View.GONE);
                                movieLoadStatus.setVisibility(View.GONE);
                            } else {
                                emptyGuide.setVisibility(View.VISIBLE);
                                tvSectionName.setText(currentCategory);
                            }
                        }
                    });
                } catch (final Exception e) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            showError("加载失败: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void showSkeleton() {
        skeletonGrid.setVisibility(View.VISIBLE);
        tvGrid.setVisibility(View.GONE);
        emptyGuide.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void hideSkeleton() {
        skeletonGrid.setVisibility(View.GONE);
    }

    private void showEmptyGuide() {
        hideSkeleton();
        tvGrid.setVisibility(View.GONE);
        emptyGuide.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        movieLoadStatus.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        hideSkeleton();
        tvGrid.setVisibility(View.GONE);
        errorStateText.setText(msg);
        errorState.setVisibility(View.VISIBLE);
        emptyGuide.setVisibility(View.GONE);
        movieLoadStatus.setVisibility(View.GONE);
    }

    private void detectAndLoadCategories(SiteConfig site, String apiUrl) {
        if (site.categoryMap != null && !site.categoryMap.isEmpty()) {
            onCategoriesLoaded(site.categoryMap);
            return;
        }
        
        // For type=3 sites, try home() via SpiderEngine to get categories
        if (site.type == 3) {
            loadCategoriesViaSpider(site);
            return;
        }
        
        fetchCategoriesFromApi(apiUrl + "?ac=list", apiUrl + "?ac=detail");
    }

    private void loadCategoriesViaSpider(final SiteConfig site) {
        isLoading = true;
        showSkeleton();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String response = SpiderEngine.getInstance().home(false, site.ext != null ? site.ext : "{}").toString();
                    final java.util.Map<String, String> typeMap = new HashMap<>();
                    
                    JSONObject json = new JSONObject(response);
                    JSONArray classes = json.optJSONArray("class");
                    if (classes != null) {
                        for (int i = 0; i < classes.length(); i++) {
                            JSONObject cls = classes.getJSONObject(i);
                            String id = cls.optString("type_id", "");
                            String name = cls.optString("type_name", "");
                            if (!name.isEmpty()) {
                                name = name.trim().replaceAll("\\s+", "");
                                typeMap.put(name, id);
                            }
                        }
                    }

                    final java.util.Map<String, String> finalTypeMap = typeMap;
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            onCategoriesLoaded(finalTypeMap);
                        }
                    });
                } catch (final Exception e) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                            hideSkeleton();
                            // Fall back to loading recommendations directly
                            loadHomeVideoViaSpider();
                        }
                    });
                }
            }
        }).start();
    }

    private void fetchCategoriesFromApi(String listUrl, String detailUrl) {
        new Thread(() -> {
            try {
                String response = httpClient.httpGet(listUrl);
                if (response == null || response.isEmpty()) throw new RuntimeException("空响应");
                JsonObject data = gson.fromJson(response, JsonObject.class);
                if (data == null || data.isJsonNull()) throw new RuntimeException("解析失败");
                
                java.util.Map<String, String> typeMap = extractClassesToMap(data);
                if (!typeMap.isEmpty()) {
                    onCategoriesLoadedWithData(typeMap, data);
                    return;
                }
            } catch (Exception e) {
                android.util.Log.w("HomeFragment", "fetchCategories listUrl failed: " + e.getMessage());
            }
            
            try {
                String response = httpClient.httpGet(detailUrl);
                if (response == null || response.isEmpty()) throw new RuntimeException("空响应");
                JsonObject data = gson.fromJson(response, JsonObject.class);
                if (data == null || data.isJsonNull()) throw new RuntimeException("解析失败");
                
                java.util.Map<String, String> typeMap = extractClassesToMap(data);
                if (!typeMap.isEmpty()) {
                    onCategoriesLoadedWithData(typeMap, data);
                    return;
                }
            } catch (Exception ignored) {}
            
            onCategoriesLoaded(new HashMap<>());
        }).start();
    }

    private void onCategoriesLoadedWithData(java.util.Map<String, String> typeMap, JsonObject data) {
        requireActivity().runOnUiThread(() -> {
            allCategories.clear();
            allCategories.add("推荐");
            typeIdByName.clear();
            categoryIdCache.clear();
            
            for (java.util.Map.Entry<String, String> entry : typeMap.entrySet()) {
                allCategories.add(entry.getKey());
                typeIdByName.put(entry.getKey(), entry.getValue());
                categoryIdCache.put(entry.getKey(), entry.getValue());
            }
            
            if (currentSite != null) {
                currentSite.categoryMap = typeMap;
                DbHelper.getInstance(getContext()).siteConfigDao().update(currentSite);
            }
            
            loadHomeDataFromCategories(data);
        });
    }

    private void onCategoriesLoaded(java.util.Map<String, String> typeMap) {
        requireActivity().runOnUiThread(() -> {
            allCategories.clear();
            allCategories.add("推荐");
            typeIdByName.clear();
            categoryIdCache.clear();
            
            for (java.util.Map.Entry<String, String> entry : typeMap.entrySet()) {
                allCategories.add(entry.getKey());
                typeIdByName.put(entry.getKey(), entry.getValue());
                categoryIdCache.put(entry.getKey(), entry.getValue());
            }
            loadMovieList();
        });
    }

    private void onCategoriesLoadedWithData(java.util.Map<String, String> typeMap) {
        requireActivity().runOnUiThread(() -> {
            allCategories.clear();
            allCategories.add("推荐");
            typeIdByName.clear();
            categoryIdCache.clear();
            
            for (java.util.Map.Entry<String, String> entry : typeMap.entrySet()) {
                allCategories.add(entry.getKey());
                typeIdByName.put(entry.getKey(), entry.getValue());
                categoryIdCache.put(entry.getKey(), entry.getValue());
            }
            loadMovieList();
        });
    }

    private void loadHomeDataFromCategories(JsonObject data) {
        if (data == null) {
            loadMovieList();
            return;
        }
        
        List<Movie> movies = new ArrayList<>();
        JsonArray listArray = data.getAsJsonArray("list");
        if (listArray == null) {
            JsonObject dataObj = data.getAsJsonObject("data");
            if (dataObj != null) {
                listArray = dataObj.getAsJsonArray("list");
            }
        }
        
        if (listArray != null) {
            for (JsonElement element : listArray) {
                JsonObject vod = element.getAsJsonObject();
                Movie movie = normalizeVod(vod);
                if (movie != null && !movie.title.isEmpty()) {
                    movies.add(movie);
                }
            }
        }
        
        if (!movies.isEmpty()) {
            movieList.clear();
            movieList.addAll(movies);
            adapter.update(movies);
            tvSectionName.setText(currentCategory);
            emptyGuide.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
        }
        
        loadMovieList();
    }

    private java.util.Map<String, String> extractClassesToMap(JsonObject data) {
        java.util.Map<String, String> map = new HashMap<>();
        if (data == null) return map;
        
        JsonArray cls = null;
        if (data.has("class") && data.get("class").isJsonArray()) {
            cls = data.getAsJsonArray("class");
        } else if (data.has("data") && data.get("data").isJsonObject()) {
            JsonObject dataObj = data.getAsJsonObject("data");
            if (dataObj.has("class") && dataObj.get("class").isJsonArray()) {
                cls = dataObj.getAsJsonArray("class");
            }
        } else if (data.has("categories") && data.get("categories").isJsonArray()) {
            cls = data.getAsJsonArray("categories");
        }
        
        if (cls != null) {
            for (JsonElement element : cls) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.has("type_id") ? obj.get("type_id").getAsString() :
                            obj.has("id") ? obj.get("id").getAsString() : "";
                String name = obj.has("type_name") ? obj.get("type_name").getAsString() :
                              obj.has("name") ? obj.get("name").getAsString() : "";
                if (!name.isEmpty()) {
                    name = name.trim().replaceAll("\\s+", "");
                    map.put(name, id);
                }
            }
        }
        return map;
    }

    public void onMovieClick(Movie movie) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.openMovieDetail(movie);
        }
    }

    public void reloadWithSite(SiteConfig site) {
        this.currentSite = site;
        if (tvSiteName != null) {
            if (site != null) {
                tvSiteName.setVisibility(View.VISIBLE);
                tvSiteName.setText("当前站点: " + site.name);
            } else {
                tvSiteName.setText("点击选择站点");
            }
        }
        if (site != null) {
            String pluginKey = !site.key.isEmpty() ? site.key : site.name;
            SpiderEngine.getInstance().setCurrentSpider(pluginKey);
            currentSiteApi = site.api;
            if (site.type == 3) {
                tvSectionName.setVisibility(View.VISIBLE);
                tvSectionName.setText("推荐");
                loadHomeVideoViaSpider();
            } else if (!currentSiteApi.isEmpty()) {
                tvSectionName.setVisibility(View.VISIBLE);
                tvSectionName.setText("推荐");
                loadMovieList();
            }
        }
    }

    public interface OnSiteNameClickListener {
        void onSiteNameClick(View v);
    }

    private OnSiteNameClickListener siteNameClickListener;

    public void setOnSiteNameClickListener(OnSiteNameClickListener listener) {
        this.siteNameClickListener = listener;
    }
}
