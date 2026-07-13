package webapp.newcloud.lottery.movie;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import webapp.newcloud.lottery.movie.model.Movie;

public class MovieDetailActivity extends AppCompatActivity {

    private ImageView ivDetailPoster;
    private TextView tvDetailTitle;
    private TextView tvDetailTag;
    private TextView tvDetailInfo;
    private TextView tvDetailDesc;
    private Button btnPlay;
    private LinearLayout llEpisodesWrap;
    private LinearLayout llEpisodes;
    private LinearLayout llLinesWrap;
    private LinearLayout llLines;
    private TextView tvLinesTitle;

    private Movie movie;
    private JSONObject spiderDetail;
    private List<String> lineNames = new ArrayList<>();
    private List<List<String>> episodeUrls = new ArrayList<>();
    private List<List<String>> episodeNames = new ArrayList<>();
    private int currentLineIndex = 0;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        initViews();
        movie = new Movie();
        movie.vodId = getStringExtra("vod_id");
        movie.title = getStringExtra("title");
        movie.pic = getStringExtra("pic");
        movie.tag = getStringExtra("tag");
        movie.type = getStringExtra("type");
        movie.year = getStringExtra("year");
        movie.area = getStringExtra("area");
        movie.play = getStringExtra("play");
        movie.desc = getStringExtra("desc");
        movie.score = getStringExtra("score");
        movie.actor = getStringExtra("actor");
        movie.director = getStringExtra("director");
        
        if (movie.title == null || movie.title.isEmpty() || movie.vodId == null || movie.vodId.isEmpty()) {
            android.util.Log.e("MovieDetail", "Missing required intent data: title=" + movie.title + " vodId=" + movie.vodId);
            finish();
            return;
        }

        String pluginKey = null;
        if (getIntent().getStringExtra("plugin_key") != null) {
            pluginKey = getIntent().getStringExtra("plugin_key");
        }

        loadDetail(pluginKey);
    }

    private void initViews() {
        ivDetailPoster = findViewById(R.id.ivDetailPoster);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailTag = findViewById(R.id.tvDetailTag);
        tvDetailInfo = findViewById(R.id.tvDetailInfo);
        tvDetailDesc = findViewById(R.id.tvDetailDesc);
        btnPlay = findViewById(R.id.btnPlay);
        llEpisodesWrap = findViewById(R.id.llEpisodesWrap);
        llEpisodes = findViewById(R.id.llEpisodes);
        llLinesWrap = findViewById(R.id.llLinesWrap);
        llLines = findViewById(R.id.llLines);
        tvLinesTitle = findViewById(R.id.tvLinesTitle);

        btnPlay.setOnClickListener(v -> playSelectedEpisode());
    }

    private String getStringExtra(String key) {
        String val = getIntent().getStringExtra(key);
        return val != null ? val : "";
    }

    private void loadDetail(String pluginKey) {
        btnPlay.setText("加载中...");
        btnPlay.setEnabled(false);

        if (pluginKey != null && !pluginKey.isEmpty() && movie.vodId != null && !movie.vodId.isEmpty()) {
            // Fetch full detail from spider
            new Thread(() -> {
                try {
                    SpiderEngine engine = SpiderEngine.getInstance();
                    engine.setCurrentSpider(pluginKey);
                    spiderDetail = engine.detail(movie.vodId);
                    
                    if (spiderDetail != null) {
                        parseSpiderDetail(spiderDetail);
                    }
                    
                    runOnUiThread(() -> {
                        if (spiderDetail == null) {
                            showBasicDetail();
                        } else {
                            updateUIFromSpider();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(this::showBasicDetail);
                }
            }).start();
        } else {
            runOnUiThread(this::showBasicDetail);
        }
    }

    private void showBasicDetail() {
        tvDetailTitle.setText(movie.title);
        tvDetailInfo.setText(buildInfoString());
        tvDetailDesc.setText(movie.desc != null ? movie.desc : "");

        if (movie.tag != null && !movie.tag.isEmpty()) {
            tvDetailTag.setText(movie.tag);
            tvDetailTag.setVisibility(View.VISIBLE);
        }

        if (movie.pic != null && !movie.pic.isEmpty()) {
            Glide.with(this).load(movie.pic).into(ivDetailPoster);
        } else {
            ivDetailPoster.setBackgroundColor(Color.parseColor("#1a2735"));
        }

        setupPlayButton();
    }

    private void parseSpiderDetail(JSONObject detail) {
        try {
            JSONArray list = detail.optJSONArray("list");
            if (list == null || list.length() == 0) {
                spiderDetail = null;
                return;
            }

            JSONObject vod = list.getJSONObject(0);
            
            // Update movie data from spider
            movie.title = vod.optString("vod_name", vod.optString("title", movie.title));
            movie.pic = vod.optString("vod_pic", vod.optString("pic", movie.pic));
            movie.tag = vod.optString("vod_tag", vod.optString("tag", movie.tag));
            movie.type = vod.optString("type_name", vod.optString("type", movie.type));
            movie.year = vod.optString("vod_year", vod.optString("year", movie.year));
            movie.area = vod.optString("vod_area", vod.optString("area", movie.area));
            movie.actor = vod.optString("vod_actor", vod.optString("actor", movie.actor));
            movie.director = vod.optString("vod_director", vod.optString("director", movie.director));
            movie.desc = vod.optString("vod_content", vod.optString("desc", movie.desc));
            movie.play = vod.optString("vod_play_url", vod.optString("play", movie.play));

            // Parse play URL into lines and episodes
            parsePlayUrl(movie.play);
        } catch (Exception e) {
            e.printStackTrace();
            spiderDetail = null;
        }
    }

    private void parsePlayUrl(String play) {
        if (play == null || play.isEmpty()) return;

        lineNames.clear();
        episodeUrls.clear();
        episodeNames.clear();

        String[] lines = play.split("\\$\\$\\$", -1);
        for (String line : lines) {
            if (line.isEmpty()) continue;
            
            String[] episodes = line.split("#", -1);
            if (episodes.length == 0) continue;

            String lineName = episodes[0].split("\\$", 2)[0];
            if (lineName.isEmpty()) lineName = "线路" + (lineNames.size() + 1);
            lineNames.add(lineName);

            List<String> urls = new ArrayList<>();
            List<String> names = new ArrayList<>();
            
            for (String ep : episodes) {
                String[] parts = ep.split("\\$", 2);
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String url = parts[1].trim();
                    if (!name.isEmpty() && !url.isEmpty()) {
                        names.add(name);
                        urls.add(url);
                    }
                }
            }

            if (!urls.isEmpty()) {
                episodeUrls.add(urls);
                episodeNames.add(names);
            }
        }
    }

    private void updateUIFromSpider() {
        tvDetailTitle.setText(movie.title);
        tvDetailInfo.setText(buildInfoString());
        tvDetailDesc.setText(movie.desc != null && !movie.desc.isEmpty() ? movie.desc : "暂无简介");

        if (movie.tag != null && !movie.tag.isEmpty()) {
            tvDetailTag.setText(movie.tag);
            tvDetailTag.setVisibility(View.VISIBLE);
        }

        if (movie.pic != null && !movie.pic.isEmpty()) {
            Glide.with(this).load(movie.pic).into(ivDetailPoster);
        } else {
            ivDetailPoster.setBackgroundColor(Color.parseColor("#1a2735"));
        }

        // Show lines if available
        if (!lineNames.isEmpty()) {
            tvLinesTitle.setText("共 " + lineNames.size() + " 条播放线路");
            llLinesWrap.setVisibility(View.VISIBLE);
            buildLinesUI();
            
            // Show episodes for first line
            if (!episodeNames.isEmpty() && !episodeUrls.isEmpty()) {
                llEpisodesWrap.setVisibility(View.VISIBLE);
                buildEpisodesUI(0);
            }
        }

        setupPlayButton();
    }

    private void buildLinesUI() {
        llLines.removeAllViews();
        
        for (int i = 0; i < lineNames.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(lineNames.get(i));
            tv.setTextSize(13);
            tv.setTextColor(Color.WHITE);
            tv.setPadding(16, 10, 16, 10);
            tv.setBackground(getDrawable(R.drawable.bg_input));
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setTag(i);
            
            final int index = i;
            tv.setOnClickListener(v -> {
                currentLineIndex = index;
                buildLinesUI();
                buildEpisodesUI(index);
            });

            if (i == 0) {
                tv.setBackgroundColor(Color.parseColor("#9b5cff"));
            } else {
                tv.setBackgroundColor(Color.parseColor("#1a2735"));
            }

            llLines.addView(tv);
        }
    }

    private void buildEpisodesUI(int lineIndex) {
        llEpisodes.removeAllViews();
        
        if (lineIndex >= episodeNames.size()) return;

        List<String> names = episodeNames.get(lineIndex);
        List<String> urls = episodeUrls.get(lineIndex);

        for (int i = 0; i < names.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(names.get(i));
            tv.setTextSize(13);
            tv.setTextColor(Color.parseColor("#aabbcc"));
            tv.setPadding(12, 8, 12, 8);
            tv.setBackground(getDrawable(R.drawable.bg_input));
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setTag(i);

            final int epIndex = i;
            tv.setOnClickListener(v -> {
                // Highlight selected episode
                int childCount = llEpisodes.getChildCount();
                for (int j = 0; j < childCount; j++) {
                    llEpisodes.getChildAt(j).setBackgroundColor(Color.parseColor("#1a2735"));
                }
                tv.setBackgroundColor(Color.parseColor("#9b5cff"));
                
                // Auto play
                currentLineIndex = lineIndex;
                playEpisode(lineIndex, epIndex);
            });

            if (i == 0) {
                tv.setBackgroundColor(Color.parseColor("#9b5cff"));
            }

            llEpisodes.addView(tv);
        }
    }

    private void setupPlayButton() {
        if (!episodeUrls.isEmpty() && !episodeNames.isEmpty()) {
            btnPlay.setEnabled(true);
            btnPlay.setText("播放 " + episodeNames.get(0).get(0));
        } else if (movie.play != null && !movie.play.isEmpty()) {
            btnPlay.setEnabled(true);
            btnPlay.setText("播放");
        } else {
            btnPlay.setText("暂无播放地址");
            btnPlay.setEnabled(false);
        }
    }

    private void playEpisode(int lineIndex, int epIndex) {
        if (lineIndex >= episodeUrls.size()) return;
        List<String> urls = episodeUrls.get(lineIndex);
        if (epIndex >= urls.size()) return;

        String url = urls.get(epIndex);
        String name = epIndex < episodeNames.get(lineIndex).size() 
            ? episodeNames.get(lineIndex).get(epIndex) 
            : "第" + (epIndex + 1) + "集";

        resolveAndPlay(url, name);
    }

    private void playSelectedEpisode() {
        if (episodeUrls.isEmpty() || episodeNames.isEmpty()) {
            Toast.makeText(this, "暂无可播放内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Play first episode of first line by default
        playEpisode(currentLineIndex, 0);
    }

    private void resolveAndPlay(String url, String name) {
        btnPlay.setText("解析中...");
        btnPlay.setEnabled(false);

        final String finalUrl = url;
        final String finalName = name;
        
        new Thread(() -> {
            try {
                String resolvedUrl = finalUrl;
                
                SpiderEngine engine = SpiderEngine.getInstance();
                String currentSite = getIntent().getStringExtra("plugin_key");
                if (currentSite != null && !currentSite.isEmpty()) {
                    engine.setCurrentSpider(currentSite);
                    String spiderPlay = engine.play(null, finalUrl, null);
                    if (spiderPlay != null && !spiderPlay.isEmpty() && !spiderPlay.equals(finalUrl)) {
                        resolvedUrl = spiderPlay;
                    }
                }

                final String finalResolvedUrl = resolvedUrl;
                runOnUiThread(() -> {
                    if (finalResolvedUrl != null && !finalResolvedUrl.isEmpty()) {
                        Intent intent = new Intent(MovieDetailActivity.this, ExoPlayerActivity.class);
                        intent.putExtra("url", finalResolvedUrl);
                        intent.putExtra("title", movie.title + " - " + finalName);
                        startActivity(intent);
                    } else {
                        btnPlay.setText("播放失败");
                        Toast.makeText(MovieDetailActivity.this, "播放地址解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Intent intent = new Intent(MovieDetailActivity.this, ExoPlayerActivity.class);
                    intent.putExtra("url", finalUrl);
                    intent.putExtra("title", movie.title + " - " + finalName);
                    startActivity(intent);
                });
            }
        }).start();
    }

    private String buildInfoString() {
        List<String> parts = new ArrayList<>();
        if (movie.year != null && !movie.year.isEmpty()) parts.add(movie.year);
        if (movie.area != null && !movie.area.isEmpty()) parts.add(movie.area);
        if (movie.type != null && !movie.type.isEmpty()) parts.add(movie.type);
        if (movie.score != null && !movie.score.isEmpty()) parts.add("评分: " + movie.score);
        return String.join(" / ", parts);
    }
}
