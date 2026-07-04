package com.personalassistant.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.personalassistant.app.R;
import com.personalassistant.app.data.model.Episode;
import com.personalassistant.app.data.model.ParseConfig;
import com.personalassistant.app.data.model.PlayLine;
import com.personalassistant.app.data.repository.MovieRepository;
import com.personalassistant.app.data.repository.SiteRepository;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.HistoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoviePlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private LinearLayout episodeBar;
    private String vodId;
    private String siteBase;
    private String siteName;
    private String lineName;
    private String episodeName;
    private int episodeIndex;
    private ArrayList<String> allEpisodesData;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ParseConfig currentParse;
    private List<ParseConfig> parseConfigs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = getIntent().getStringExtra("title");
        String url = getIntent().getStringExtra("url");
        vodId = getIntent().getStringExtra("vod_id");
        siteBase = getIntent().getStringExtra("site_base");
        siteName = getIntent().getStringExtra("site_name");
        lineName = getIntent().getStringExtra("line_name");
        episodeName = getIntent().getStringExtra("episode_name");
        episodeIndex = getIntent().getIntExtra("episode_index", 0);
        allEpisodesData = getIntent().getStringArrayListExtra("all_episodes");

        if (title != null) setTitle(title);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);

        // Top toolbar
        LinearLayout topBar = createTopBar();
        root.addView(topBar);

        // Player
        playerView = new PlayerView(this);
        playerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(playerView);

        // Episode bar
        episodeBar = new LinearLayout(this);
        episodeBar.setOrientation(LinearLayout.HORIZONTAL);
        episodeBar.setBackgroundColor(0xFF111827);
        episodeBar.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams epParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        epParams.weight = 0;
        episodeBar.setLayoutParams(epParams);
        root.addView(episodeBar);

        setContentView(root);
        initPlayer(url);
        buildEpisodeBar();
        loadParseConfigs();
    }

    private LinearLayout createTopBar() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF111827);
        topBar.setPadding(dp(8), dp(4), dp(8), dp(4));

        // Back button
        Button backBtn = new Button(this);
        backBtn.setText("\u2190");
        backBtn.setTextSize(18);
        backBtn.setTextColor(0xFFFFFFFF);
        backBtn.setBackgroundColor(0x33FFFFFF);
        backBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        // Site name
        if (siteName != null && !siteName.isEmpty()) {
            TextView tvSite = new TextView(this);
            tvSite.setText(" [" + siteName + "]");
            tvSite.setTextColor(0xFF7C3AED);
            tvSite.setTextSize(12);
            tvSite.setGravity(Gravity.CENTER_VERTICAL);
            topBar.addView(tvSite);
        }

        // Resolution button
        Button resBtn = new Button(this);
        resBtn.setText("解析");
        resBtn.setTextSize(11);
        resBtn.setTextColor(0xFFFFFFFF);
        resBtn.setBackgroundColor(0xFF7C3AED);
        resBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        resBtn.setOnClickListener(v -> showParseDialog());
        topBar.addView(resBtn);

        // Collect button
        Button favBtn = new Button(this);
        favBtn.setText("收藏");
        favBtn.setTextSize(11);
        favBtn.setTextColor(0xFFFFFFFF);
        favBtn.setBackgroundColor(0xFF16A34A);
        favBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        favBtn.setOnClickListener(v -> saveToFavorites());
        topBar.addView(favBtn);

        return topBar;
    }

    private void initPlayer(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "无效的视频链接", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);

        String currentUrl = getIntent().getStringExtra("url");
        MediaItem mediaItem = MediaItem.fromUri(currentUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    saveHistory();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(() -> {
                    Toast.makeText(MoviePlayerActivity.this,
                            "播放失败，尝试其他解析...", Toast.LENGTH_SHORT).show();
                    tryNextParse();
                });
            }
        });
    }

    private void buildEpisodeBar() {
        episodeBar.removeAllViews();
        if (allEpisodesData == null || allEpisodesData.isEmpty()) return;

        for (int i = 0; i < allEpisodesData.size() && i < 10; i++) {
            String data = allEpisodesData.get(i);
            String[] parts = data.split("\\|", 2);
            if (parts.length < 2) continue;

            String lineName = parts[0];
            String epsStr = parts[1];
            String[] eps = epsStr.split("\\|");

            Button btn = new Button(this);
            btn.setText(lineName + "(" + eps.length + ")");
            btn.setTextSize(10);
            btn.setAllCaps(false);
            btn.setMinWidth(dp(60));
            btn.setPadding(dp(8), dp(6), dp(8), dp(6));

            final String finalLineName = lineName;
            final String finalEpsStr = epsStr;

            boolean isActive = lineName.equals(this.lineName);
            btn.setTextColor(isActive ? 0xFFFFFFFF : 0xFF8899AA);
            btn.setBackgroundColor(isActive ? 0xFF7C3AED : 0x22FFFFFF);
            btn.setOnClickListener(v -> {
                // Switch to first episode of this line
                String[] firstEp = finalEpsStr.split("=", 2);
                if (firstEp.length >= 2) {
                    String resolvedUrl = MovieRepository.resolveVideoUrl(siteBase, firstEp[1]);
                    playEpisode(firstEp[0], resolvedUrl);
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(4);
            btn.setLayoutParams(params);
            episodeBar.addView(btn);
        }
    }

    private void playEpisode(String epName, String url) {
        Intent intent = getIntent();
        intent.putExtra("url", url);
        intent.putExtra("title", getTitle().toString().replaceFirst(" - .+$", "") + " - " + epName);
        intent.putExtra("episode_name", epName);
        // Recreate activity with new URL
        finish();
        startActivity(intent);
    }

    private void loadParseConfigs() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                List<com.personalassistant.app.db.entity.ParseConfigEntity> entities =
                        db.parseConfigDao().getAllConfigsBlocking();

                if (entities != null) {
                    for (com.personalassistant.app.db.entity.ParseConfigEntity e : entities) {
                        ParseConfig cfg = new ParseConfig();
                        cfg.name = e.name;
                        cfg.url = e.url;
                        cfg.type = e.type;
                        cfg.flags = new java.util.ArrayList<>();
                        if (e.flags != null && !e.flags.isEmpty()) {
                            for (String flag : e.flags.split(",")) {
                                cfg.flags.add(flag.trim());
                            }
                        }
                        parseConfigs.add(cfg);
                    }
                }

                // Also load from site repo if available
                if (siteBase != null && !siteBase.isEmpty()) {
                    // Try to load parses from saved repo config
                }
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void showParseDialog() {
        if (parseConfigs.isEmpty()) {
            Toast.makeText(this, "暂无可用解析", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[parseConfigs.size()];
        for (int i = 0; i < parseConfigs.size(); i++) {
            names[i] = parseConfigs.get(i).name;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("选择解析")
                .setItems(names, (dialog, which) -> {
                    ParseConfig cfg = parseConfigs.get(which);
                    currentParse = cfg;
                    useParse(cfg);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void useParse(ParseConfig parse) {
        String currentUrl = getIntent().getStringExtra("url");
        String parseUrl = parse.url + currentUrl;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(parseUrl));
        intent.setType("text/plain");
        startActivity(intent);
    }

    private void tryNextParse() {
        if (currentParse == null || parseConfigs.isEmpty()) return;

        int currentIndex = parseConfigs.indexOf(currentParse);
        int nextIndex = (currentIndex + 1) % parseConfigs.size();
        currentParse = parseConfigs.get(nextIndex);
        useParse(currentParse);
    }

    private void saveToFavorites() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MoviePlayerActivity.this);
                com.personalassistant.app.db.entity.FavoriteEntity entity =
                        new com.personalassistant.app.db.entity.FavoriteEntity();
                entity.movieId = vodId;
                entity.movieTitle = getTitle().toString();
                entity.siteName = siteName != null ? siteName : "";
                entity.siteBase = siteBase != null ? siteBase : "";
                entity.pic = getIntent().getStringExtra("pic");
                entity.timestamp = System.currentTimeMillis();

                long result = db.favoriteDao().insert(entity);
                handler.post(() -> {
                    if (result > 0) {
                        Toast.makeText(MoviePlayerActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MoviePlayerActivity.this, "已收藏过", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(MoviePlayerActivity.this,
                        "收藏失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveHistory() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MoviePlayerActivity.this);
                long position = player.getCurrentPosition();
                long duration = player.getDuration();

                HistoryEntity entity = new HistoryEntity();
                entity.movieId = vodId;
                entity.movieTitle = getTitle().toString();
                entity.siteName = siteName != null ? siteName : "";
                entity.siteBase = siteBase != null ? siteBase : "";
                entity.playFrom = lineName != null ? lineName : "";
                entity.episodeName = episodeName != null ? episodeName : "";
                entity.episodeIndex = episodeIndex;
                entity.position = position;
                entity.duration = duration;
                entity.timestamp = System.currentTimeMillis();

                db.historyDao().insert(entity);
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            saveHistory();
            player.setPlayWhenReady(false);
        }
        playerView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerView.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
        executor.shutdownNow();
    }

    private void releasePlayer() {
        if (player != null) {
            saveHistory();
            player.stop();
            player.release();
            player = null;
        }
    }

    int dp(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }
}
