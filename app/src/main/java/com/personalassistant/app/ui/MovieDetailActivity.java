package com.personalassistant.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.personalassistant.app.R;
import com.personalassistant.app.data.model.Episode;
import com.personalassistant.app.data.model.PlayLine;
import com.personalassistant.app.data.repository.MovieRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieDetailActivity extends AppCompatActivity {
    private TabHost tabHost;
    private ScrollView mainScroll;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String vodId;
    private String title;
    private String siteBase;
    private String siteName;
    private List<PlayLine> allPlayLines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vodId = getIntent().getStringExtra("vod_id");
        title = getIntent().getStringExtra("title");
        String pic = getIntent().getStringExtra("pic");
        String type = getIntent().getStringExtra("type");
        String year = getIntent().getStringExtra("year");
        String area = getIntent().getStringExtra("area");
        String actor = getIntent().getStringExtra("actor");
        String director = getIntent().getStringExtra("director");
        String desc = getIntent().getStringExtra("desc");
        String score = getIntent().getStringExtra("score");
        siteBase = getIntent().getStringExtra("site_base");
        siteName = getIntent().getStringExtra("site_name");

        setTitle(title != null ? title : "影片详情");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF05070D);

        ScrollView scroll = new ScrollView(this);
        scroll.setId(View.generateViewId());
        mainScroll = scroll;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        content.setPadding(pad, pad, pad, pad);

        // Poster
        if (pic != null && !pic.isEmpty()) {
            ImageView poster = new ImageView(this);
            poster.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));
            poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(pic).into(poster);
            content.addView(poster);
        }

        // Title + score
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setPadding(0, dp(12), 0, 0);
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(20);
        titleRow.addView(tvTitle);
        if (score != null && !score.isEmpty()) {
            TextView tvScore = new TextView(this);
            tvScore.setText(" " + score);
            tvScore.setTextColor(0xFFFFA500);
            tvScore.setTextSize(16);
            titleRow.addView(tvScore);
        }
        content.addView(titleRow);

        // Meta info
        if (type != null || year != null || area != null) {
            TextView tvMeta = new TextView(this);
            StringBuilder sb = new StringBuilder();
            if (type != null && !type.isEmpty()) sb.append(type);
            if (year != null && !year.isEmpty()) sb.append(" / ").append(year);
            if (area != null && !area.isEmpty()) sb.append(" / ").append(area);
            tvMeta.setText(sb.toString().replaceFirst("^ / ", ""));
            tvMeta.setTextColor(0xFF8899AA);
            tvMeta.setTextSize(13);
            tvMeta.setPadding(0, dp(4), 0, 0);
            content.addView(tvMeta);
        }

        if (director != null && !director.isEmpty()) {
            TextView tvDir = new TextView(this);
            tvDir.setText("导演: " + director.replaceAll("<[^>]*>", ""));
            tvDir.setTextColor(0xFF8899AA);
            tvDir.setTextSize(13);
            tvDir.setPadding(0, dp(4), 0, 0);
            content.addView(tvDir);
        }

        if (actor != null && !actor.isEmpty()) {
            TextView tvActor = new TextView(this);
            tvActor.setText("主演: " + actor.replaceAll("<[^>]*>", ""));
            tvActor.setTextColor(0xFF8899AA);
            tvActor.setTextSize(13);
            tvActor.setPadding(0, dp(4), 0, 0);
            content.addView(tvActor);
        }

        if (siteName != null && !siteName.isEmpty()) {
            TextView tvSite = new TextView(this);
            tvSite.setText("来源: " + siteName);
            tvSite.setTextColor(0xFF7C3AED);
            tvSite.setTextSize(12);
            tvSite.setPadding(0, dp(4), 0, 0);
            content.addView(tvSite);
        }

        if (desc != null && !desc.isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText("简介: " + desc.replaceAll("<[^>]*>", ""));
            tvDesc.setTextColor(0xFFAABBCC);
            tvDesc.setTextSize(13);
            tvDesc.setPadding(0, dp(12), 0, 0);
            content.addView(tvDesc);
        }

        // TabHost for play lines
        tabHost = new TabHost(this, null);
        tabHost.setId(android.R.id.tabhost);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        tabParams.topMargin = dp(16);
        tabHost.setLayoutParams(tabParams);

        tabHost.setup();
        content.addView(tabHost);

        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);

        // Load episodes from passed data or fetch detail
        String playFrom = getIntent().getStringExtra("play_from");
        String playUrl = getIntent().getStringExtra("play_url");
        if (playFrom != null && playUrl != null && !playFrom.isEmpty() && !playUrl.isEmpty()) {
            allPlayLines = MovieRepository.parsePlayLines(playFrom, playUrl);
            buildTabs();
        } else if (vodId != null) {
            loadDetail();
        }
    }

    private void loadDetail() {
        TextView loading = new TextView(this);
        loading.setText("加载详情中...");
        loading.setTextColor(0xFF667788);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(20), 0, 0);

        executor.execute(() -> {
            try {
                com.personalassistant.app.data.model.MovieItem detail =
                        MovieRepository.getMovieById(siteBase != null ? siteBase : "", vodId);
                handler.post(() -> {
                    if (detail != null && detail.playLines != null && !detail.playLines.isEmpty()) {
                        allPlayLines = detail.playLines;
                        buildTabs();
                    } else {
                        loading.setText("暂无播放信息");
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    loading.setText("加载失败: " + e.getMessage());
                });
            }
        });
    }

    private void buildTabs() {
        if (allPlayLines.isEmpty()) {
            TextView noEp = new TextView(this);
            noEp.setText("暂无剧集");
            noEp.setTextColor(0xFF667788);
            noEp.setGravity(Gravity.CENTER);
            noEp.setPadding(0, dp(40), 0, 0);
            ScrollView sv = mainScroll;
            if (sv != null) {
                LinearLayout content = (LinearLayout) sv.getChildAt(0);
                content.addView(noEp);
            }
            return;
        }

        tabHost.setup();
        for (int i = 0; i < allPlayLines.size(); i++) {
            final PlayLine line = allPlayLines.get(i);
            TabHost.TabSpec spec = tabHost.newTabSpec(line.lineName);
            final int tabIndex = i;
            spec.setContent(new TabHost.TabContentFactory() {
                @Override
                public View createTabContent(String tag) {
                    LinearLayout panel = new LinearLayout(MovieDetailActivity.this);
                    panel.setOrientation(LinearLayout.VERTICAL);
                    panel.setPadding(dp(8), dp(8), dp(8), dp(8));
                    panel.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));

                    if (line.episodes != null) {
                        for (final Episode ep : line.episodes) {
                            Button btn = new Button(MovieDetailActivity.this);
                            btn.setText(ep.name);
                            btn.setTextColor(0xFFDFE8FF);
                            btn.setBackgroundColor(0x12FFFFFF);
                            btn.setAllCaps(false);
                            btn.setPadding(dp(0), dp(10), dp(0), dp(10));
                            btn.setOnClickListener(ev -> playEpisode(ep, line.lineName));

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.bottomMargin = dp(6);
                            btn.setLayoutParams(params);
                            panel.addView(btn);
                        }
                    }
                    return panel;
                }
            });
            spec.setIndicator(line.lineName);
            tabHost.addTab(spec);
        }
    }

    private void playEpisode(Episode ep, String lineName) {
        String resolvedUrl = MovieRepository.resolveVideoUrl(siteBase, ep.url);

        Intent intent = new Intent(MovieDetailActivity.this, MoviePlayerActivity.class);
        intent.putExtra("title", title + " - " + ep.name);
        intent.putExtra("url", resolvedUrl);
        intent.putExtra("vod_id", vodId);
        intent.putExtra("site_id", getIntent().getIntExtra("site_id", 0));
        intent.putExtra("site_base", siteBase);
        intent.putExtra("site_name", siteName);
        intent.putExtra("line_name", lineName);
        intent.putExtra("episode_name", ep.name);
        intent.putExtra("episode_index", 0);
        intent.putStringArrayListExtra("all_episodes", serializeEpisodes(allPlayLines));
        startActivity(intent);
    }

    private ArrayList<String> serializeEpisodes(List<PlayLine> lines) {
        ArrayList<String> result = new ArrayList<>();
        for (PlayLine line : lines) {
            StringBuilder sb = new StringBuilder();
            sb.append(line.lineName);
            if (line.episodes != null) {
                for (Episode ep : line.episodes) {
                    if (sb.length() > line.lineName.length()) sb.append("|");
                    sb.append(ep.name).append("=").append(ep.url);
                }
            }
            result.add(sb.toString());
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    int dp(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }
}
