package com.personalassistant.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.personalassistant.app.R;
import com.personalassistant.app.data.model.MovieItem;
import com.personalassistant.app.data.repository.MovieRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieSearchActivity extends AppCompatActivity {
    private EditText searchInput;
    private LinearLayout resultsContainer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String siteBase;
    private String siteName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        siteBase = getIntent().getStringExtra("site_base");
        siteName = getIntent().getStringExtra("site_name");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF05070D);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, 0);
        setTitle("搜索影视");

        // Search bar
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchInput = new EditText(this);
        searchInput.setHint("输入关键词搜索...");
        searchInput.setHintTextColor(0xFF6F7890);
        searchInput.setTextColor(0xFFE8EDFF);
        searchInput.setBackgroundColor(0x0FFFFFFF);
        searchInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        searchInput.setLayoutParams(searchParams);

        Button searchBtn = new Button(this);
        searchBtn.setText("搜索");
        searchBtn.setTextColor(0xFFFFFFFF);
        searchBtn.setBackgroundColor(0xFF7C3AED);
        searchBtn.setOnClickListener(v -> performSearch());

        searchRow.addView(searchInput);
        searchRow.addView(searchBtn);
        root.addView(searchRow);

        // Results
        ScrollView scroll = new ScrollView(this);
        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(resultsContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        root.addView(scroll, scrollParams);

        setContentView(root);
    }

    private void performSearch() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) return;

        if (siteBase == null || siteBase.isEmpty()) {
            Toast.makeText(this, "请先选择站点", Toast.LENGTH_SHORT).show();
            return;
        }

        resultsContainer.removeAllViews();

        TextView loading = new TextView(this);
        loading.setText("搜索中...");
        loading.setTextColor(0xFF667788);
        loading.setPadding(0, dp(20), 0, 0);
        loading.setGravity(Gravity.CENTER);
        resultsContainer.addView(loading);

        executor.execute(() -> {
            try {
                List<MovieItem> items = MovieRepository.searchMovies(siteBase, query, 1);
                handler.post(() -> {
                    resultsContainer.removeAllViews();
                    if (items == null || items.isEmpty()) {
                        TextView empty = new TextView(MovieSearchActivity.this);
                        empty.setText("未找到相关影片");
                        empty.setTextColor(0xFF667788);
                        empty.setPadding(0, dp(40), 0, 0);
                        empty.setGravity(Gravity.CENTER);
                        resultsContainer.addView(empty);
                        return;
                    }
                    for (MovieItem item : items) {
                        resultsContainer.addView(createResultRow(item));
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    resultsContainer.removeAllViews();
                    Toast.makeText(MovieSearchActivity.this,
                            "搜索失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private LinearLayout createResultRow(MovieItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackgroundColor(0xFF101D2B);
        row.setOnClickListener(v -> {
            Intent intent = new Intent(MovieSearchActivity.this, MovieDetailActivity.class);
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
            intent.putExtra("site_id", 0);
            intent.putExtra("site_base", siteBase);
            intent.putExtra("site_name", siteName);
            intent.putExtra("play_from", item.playFrom);
            intent.putExtra("play_url", item.playUrl);
            startActivity(intent);
        });

        // Poster thumbnail
        ImageView poster = new ImageView(this);
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(60), dp(80));
        posterParams.rightMargin = dp(10);
        poster.setLayoutParams(posterParams);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (item.pic != null && !item.pic.isEmpty()) {
            Glide.with(this).load(item.pic).into(poster);
        } else {
            poster.setBackgroundColor(0xFF1D3557);
        }
        row.addView(poster);

        // Text info
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(0), dp(4), dp(0), dp(0));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textCol.setLayoutParams(textParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(item.title);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(15);
        textCol.addView(tvTitle);

        StringBuilder info = new StringBuilder();
        if (item.year != null && !item.year.isEmpty()) info.append(item.year);
        if (item.tag != null && !item.tag.isEmpty()) info.append(" ").append(item.tag);
        if (item.area != null && !item.area.isEmpty()) info.append(" ").append(item.area);
        TextView tvInfo = new TextView(this);
        tvInfo.setText(info.toString());
        tvInfo.setTextColor(0xFF8899AA);
        tvInfo.setTextSize(12);
        tvInfo.setPadding(0, dp(4), 0, 0);
        textCol.addView(tvInfo);

        if (item.actor != null && !item.actor.isEmpty()) {
            TextView tvActor = new TextView(this);
            tvActor.setText("主演: " + item.actor.replaceAll("<[^>]*>", "").substring(0,
                    Math.min(30, item.actor.replaceAll("<[^>]*>", "").length())));
            tvActor.setTextColor(0xFF667788);
            tvActor.setTextSize(11);
            tvActor.setPadding(0, dp(2), 0, 0);
            textCol.addView(tvActor);
        }

        row.addView(textCol);
        return row;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    int dp(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }
}
