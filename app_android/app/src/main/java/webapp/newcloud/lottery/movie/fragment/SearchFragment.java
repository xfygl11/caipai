package webapp.newcloud.lottery.movie.fragment;

import android.view.KeyEvent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import webapp.newcloud.lottery.movie.MainActivity;
import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.SpiderEngine;
import webapp.newcloud.lottery.movie.adapter.MovieGridAdapter;
import webapp.newcloud.lottery.movie.util.DbHelper;
import webapp.newcloud.lottery.movie.model.Movie;
import webapp.newcloud.lottery.movie.model.SiteConfig;
import webapp.newcloud.lottery.movie.util.HttpClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private TextView searchStatus;
    private RecyclerView searchResults;
    private LinearLayout searchHistoryContainer;
    private LinearLayout searchHistoryList;
    private TextView tvClearHistory;
    private MovieGridAdapter adapter;
    private List<Movie> movieList = new ArrayList<>();
    private java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private int totalCount = 0;

    private final HttpClient httpClient = new HttpClient();
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        searchInput = view.findViewById(R.id.searchInput);
        searchStatus = view.findViewById(R.id.searchStatus);
        searchResults = view.findViewById(R.id.searchResults);
        searchHistoryContainer = view.findViewById(R.id.searchHistoryContainer);
        searchHistoryList = view.findViewById(R.id.searchHistoryList);
        tvClearHistory = view.findViewById(R.id.tvClearHistory);

        searchResults.setLayoutManager(new GridLayoutManager(getContext(), 3));

        searchHistoryList.setOnClickListener(v -> searchHistoryContainer.setVisibility(View.GONE));
        tvClearHistory.setOnClickListener(v -> {
            clearSearchHistory();
            searchHistoryContainer.setVisibility(View.GONE);
        });

        searchInput.setOnClickListener(v -> {
            loadSearchHistory();
            if (getSearchHistory().size() > 0) {
                searchHistoryContainer.setVisibility(View.VISIBLE);
            }
        });

        searchInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    searchHistoryContainer.setVisibility(View.GONE);
                } else if (getSearchHistory().size() > 0) {
                    searchHistoryContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        adapter = new MovieGridAdapter(movieList, new MovieGridAdapter.OnMovieClickListener() {
            @Override
            public void onMovieClick(Movie movie) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.openMovieDetail(movie);
                }
            }
        });
        searchResults.setAdapter(adapter);

        view.findViewById(R.id.searchBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                performSearch();
                return true;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("SearchFragment", "=== onResume, getView()=" + (getView() != null ? "not-null" : "null") + " ===");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        android.util.Log.d("SearchFragment", "=== onHiddenChanged: hidden=" + hidden + " ===");
    }

    private void performSearch() {
        performSearch(searchInput.getText().toString().trim());
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) {
            searchStatus.setText("请输入搜索关键词");
            searchStatus.setVisibility(View.VISIBLE);
            return;
        }
        saveSearchHistory(keyword);
        searchHistoryContainer.setVisibility(View.GONE);

        movieList.clear();
        adapter.notifyDataSetChanged();
        searchStatus.setText("搜索中...");
        searchStatus.setVisibility(View.VISIBLE);
        completedCount.set(0);

        if (getContext() == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<SiteConfig> sites = DbHelper.getInstance(getContext()).siteConfigDao().getAll();
                    final List<SiteConfig> searchableSites = new ArrayList<>();
                    for (SiteConfig site : sites) {
                        if (site.searchable > 0 && site.api != null && !site.api.isEmpty()) {
                            searchableSites.add(site);
                        }
                    }

                    if (searchableSites.isEmpty()) {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                searchStatus.setText("没有可搜索的站点");
                            }
                        });
                        return;
                    }

                    final List<Movie> results = new CopyOnWriteArrayList<>();
                    final int total = searchableSites.size();
                    final int maxConcurrent = 8;
                    final List<SiteConfig> queue = new ArrayList<>(searchableSites);
                    final AtomicInteger running = new AtomicInteger(0);

                    final AtomicReference<Runnable> runNextRef = new AtomicReference<>();

                    runNextRef.set(new Runnable() {
                        @Override
                        public void run() {
                            if (queue.isEmpty()) return;
                            if (running.get() >= maxConcurrent) return;
                            running.incrementAndGet();
                            final SiteConfig site = queue.remove(0);

                            final SiteConfig finalSite = site;
                            
                            if (site.type == 3) {
                                // Use SpiderEngine for type=3 sites
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String pluginKey = !finalSite.key.isEmpty() ? finalSite.key : finalSite.name;
                                            SpiderEngine.getInstance().setCurrentSpider(pluginKey);
                                            
                                            final JSONObject searchResult = SpiderEngine.getInstance().search(keyword, true);
                                            if (searchResult != null) {
                                                JSONArray listArray = searchResult.optJSONArray("list");
                                                if (listArray != null) {
                                                    for (int i = 0; i < listArray.length(); i++) {
                                                        JSONObject vod = listArray.getJSONObject(i);
                                                        Movie movie = normalizeVodJson(vod);
                                                        if (movie != null && !movie.title.isEmpty()) {
                                                            results.add(movie);
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        } finally {
                                            running.decrementAndGet();
                                            int completed = completedCount.incrementAndGet();
                                            final int finished = completed;
                                            requireActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    searchStatus.setText("已搜索 " + finished + "/" + total);
                                                    if (finished == total) {
                                                        searchStatus.setText("搜索完成，共 " + results.size() + " 条结果");
                                                    }
                                                }
                                            });
                                            runNextRef.get().run();
                                        }
                                    }
                                }).start();
                            } else {
                                // Use direct HTTP for type=0/1 sites
                                String encodedUrlStr;
                                try {
                                    encodedUrlStr = site.api + "?ac=search&wd=" + java.net.URLEncoder.encode(keyword, "UTF-8");
                                } catch (Exception e) {
                                    encodedUrlStr = site.api + "?ac=search&wd=" + keyword;
                                }
                                final String encodedUrl = encodedUrlStr;
                                
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final String response = httpClient.httpGet(encodedUrl);
                                            final JsonObject data = gson.fromJson(response, JsonObject.class);
                                            if (data != null) {
                                                JsonArray listArray = data.getAsJsonArray("list");
                                                if (listArray == null) {
                                                    JsonObject dataObj = data.getAsJsonObject("data");
                                                    if (dataObj != null) listArray = dataObj.getAsJsonArray("list");
                                                }
                                                if (listArray != null) {
                                                    for (JsonElement element : listArray) {
                                                        JsonObject vod = element.getAsJsonObject();
                                                        Movie movie = normalizeVod(vod);
                                                        if (movie != null && !movie.title.isEmpty()) {
                                                            results.add(movie);
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        } finally {
                                            running.decrementAndGet();
                                            int completed = completedCount.incrementAndGet();
                                            final int finished = completed;
                                            requireActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    searchStatus.setText("已搜索 " + finished + "/" + total);
                                                    if (finished == total) {
                                                        searchStatus.setText("搜索完成，共 " + results.size() + " 条结果");
                                                    }
                                                }
                                            });
                                            runNextRef.get().run();
                                        }
                                    }
                                }).start();
                            }
                        }
                    });

                    for (int i = 0; i < maxConcurrent && !queue.isEmpty(); i++) {
                        runNextRef.get().run();
                    }

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            movieList.clear();
                            movieList.addAll(results);
                            adapter.notifyDataSetChanged();
                            if (results.isEmpty()) {
                                searchStatus.setText("未找到相关结果");
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Movie normalizeVod(JsonObject vod) {
        if (vod == null) return null;
        Movie movie = new Movie();
        movie.id = UUID.randomUUID().toString();
        movie.vodId = vod.has("id") ? vod.get("id").getAsString() :
                      vod.has("vod_id") ? vod.get("vod_id").getAsString() : "";
        movie.title = vod.has("title") ? vod.get("title").getAsString() :
                      vod.has("vod_name") ? vod.get("vod_name").getAsString() : "";
        movie.pic = vod.has("pic") ? vod.get("pic").getAsString() :
                    vod.has("vod_pic") ? vod.get("vod_pic").getAsString() : "";
        movie.tag = vod.has("tag") ? vod.get("tag").getAsString() : "";
        movie.type = vod.has("type") ? vod.get("type").getAsString() : "";
        movie.year = vod.has("year") ? vod.get("year").getAsString() : "";
        movie.area = vod.has("area") ? vod.get("area").getAsString() : "";
        movie.play = vod.has("play") ? vod.get("play").getAsString() : "";
        movie.desc = vod.has("desc") ? vod.get("desc").getAsString() : "";
        movie.createdAt = System.currentTimeMillis();
        return movie;
    }

    private Movie normalizeVodJson(JSONObject vod) {
        if (vod == null) return null;
        Movie movie = new Movie();
        movie.id = UUID.randomUUID().toString();
        movie.vodId = vod.optString("id", vod.optString("vod_id", ""));
        movie.title = vod.optString("title", vod.optString("vod_name", ""));
        movie.pic = vod.optString("pic", vod.optString("vod_pic", ""));
        movie.tag = vod.optString("tag", "");
        movie.type = vod.optString("type", "");
        movie.year = vod.optString("year", "");
        movie.area = vod.optString("area", "");
        movie.play = vod.optString("play", "");
        movie.desc = vod.optString("desc", "");
        movie.createdAt = System.currentTimeMillis();
        return movie;
    }

    private Set<String> getSearchHistory() {
        if (getContext() == null) return new LinkedHashSet<>();
        try {
            String json = getContext().getSharedPreferences("search_history", 0).getString("history", "[]");
            com.google.gson.JsonArray arr = gson.fromJson(json, com.google.gson.JsonArray.class);
            Set<String> history = new LinkedHashSet<>();
            if (arr != null) {
                for (int i = 0; i < arr.size() && i < 20; i++) {
                    history.add(arr.get(i).getAsString());
                }
            }
            return history;
        } catch (Exception e) {
            return new LinkedHashSet<>();
        }
    }

    private void loadSearchHistory() {
        searchHistoryList.removeAllViews();
        Set<String> history = getSearchHistory();
        int count = 0;
        for (String kw : history) {
            if (count >= 10) break;
            final String keyword = kw;
            TextView tv = new TextView(getContext());
            tv.setText(keyword);
            tv.setTextColor(0xff8899aa);
            tv.setTextSize(13);
            tv.setPadding(12, 10, 12, 10);
            tv.setBackground(getContext().getDrawable(R.drawable.bg_input));
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchInput.setText(keyword);
                    searchInput.setSelection(keyword.length());
                    searchHistoryContainer.setVisibility(View.GONE);
                    performSearch(keyword);
                }
            });
            searchHistoryList.addView(tv);
            count++;
        }
    }

    private void saveSearchHistory(String keyword) {
        if (getContext() == null || keyword.trim().isEmpty()) return;
        try {
            SharedPreferences sp = getContext().getSharedPreferences("search_history", 0);
            String json = sp.getString("history", "[]");
            com.google.gson.JsonArray arr = gson.fromJson(json, com.google.gson.JsonArray.class);
            if (arr == null) arr = new com.google.gson.JsonArray();
            
            for (int i = arr.size() - 1; i >= 0; i--) {
                if (keyword.equals(arr.get(i).getAsString())) {
                    arr.remove(i);
                }
            }
            
            com.google.gson.JsonElement ke = gson.toJsonTree(keyword);
            com.google.gson.JsonArray newArr = new com.google.gson.JsonArray();
            newArr.add(ke);
            for (int i = 0; i < arr.size(); i++) {
                newArr.add(arr.get(i));
            }
            arr = newArr;
            while (arr.size() > 20) {
                arr.remove(arr.size() - 1);
            }
            
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("history", gson.toJson(arr));
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearSearchHistory() {
        if (getContext() == null) return;
        getContext().getSharedPreferences("search_history", 0).edit().remove("history").apply();
    }

    public void onMovieClick(Movie movie) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.openMovieDetail(movie);
        }
    }
}
