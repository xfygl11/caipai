package com.personalassistant.app.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.personalassistant.app.R;
import com.personalassistant.app.data.model.Episode;
import com.personalassistant.app.data.model.ParseConfig;
import com.personalassistant.app.data.model.PlayLine;
import com.personalassistant.app.data.repository.MovieRepository;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.FavoriteEntity;
import com.personalassistant.app.db.entity.HistoryEntity;
import com.personalassistant.app.db.entity.ParseConfigEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoviePlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private String vodId;
    private String siteBase;
    private String siteName;
    private String lineName;
    private String episodeName;
    private int episodeIndex;
    private ArrayList<String> allEpisodesData;
    private List<PlayLine> playLines = new ArrayList<>();
    private List<ParseConfig> parseConfigs = new ArrayList<>();
    private ParseConfig currentParse;
    private String currentUrl;
    private String currentEpName;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvTitle;
    private TextView tvMovieTitle;
    private TextView tvError;
    private Button btnRetry;
    private Button btnNextParse;
    private HorizontalScrollView parserScroll;
    private LinearLayout parserButtons;
    private RecyclerView rvEpisodes;
    private EpisodeAdapter episodeAdapter;
    private int currentSpeedIndex = 1;
    private final float[] SPEEDS = {0.5f, 1.0f, 1.5f, 2.0f};
    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setSystemUIHidden(true);

        setContentView(R.layout.activity_movie_player);

        vodId = getIntent().getStringExtra("vod_id");
        siteBase = getIntent().getStringExtra("site_base");
        siteName = getIntent().getStringExtra("site_name");
        lineName = getIntent().getStringExtra("line_name");
        episodeName = getIntent().getStringExtra("episode_name");
        episodeIndex = getIntent().getIntExtra("episode_index", 0);
        allEpisodesData = getIntent().getStringArrayListExtra("all_episodes");
        currentUrl = getIntent().getStringExtra("url");

        tvTitle = findViewById(R.id.tv_title);
        tvMovieTitle = findViewById(R.id.tv_movie_title);
        tvError = findViewById(R.id.tv_error_msg);
        btnRetry = findViewById(R.id.btn_retry);
        btnNextParse = findViewById(R.id.btn_next_parse);
        parserScroll = findViewById(R.id.parser_scroll);
        parserButtons = findViewById(R.id.parser_buttons);
        rvEpisodes = findViewById(R.id.rv_episodes);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_parser).setOnClickListener(v -> showParseDialog());
        findViewById(R.id.btn_fav).setOnClickListener(v -> saveToFavorites());
        findViewById(R.id.btn_speed).setOnClickListener(v -> showSpeedDialog());

        btnRetry.setOnClickListener(v -> {
            findViewById(R.id.error_overlay).setVisibility(View.GONE);
            playUrl(currentUrl);
        });

        btnNextParse.setOnClickListener(v -> tryNextParse());

        if (siteName != null && !siteName.isEmpty()) {
            TextView tvSite = findViewById(R.id.tv_site);
            tvSite.setText(" [" + siteName + "]");
        }

        if (vodId != null && !vodId.isEmpty()) {
            loadMovieDetail();
        } else if (currentUrl != null && !currentUrl.isEmpty()) {
            if (episodeName != null) {
                tvMovieTitle.setText(episodeName);
            }
            initPlayer(currentUrl);
        } else {
            Toast.makeText(this, "无效的视频链接", Toast.LENGTH_SHORT).show();
            finish();
        }

        loadParseConfigs();
    }

    private void loadMovieDetail() {
        TextView loading = new TextView(this);
        loading.setText("加载详情中...");
        loading.setTextColor(0xFF667788);
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setTextSize(14);
        loading.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout episodeSection = findViewById(R.id.episode_section);
        episodeSection.addView(loading, 0);

        executor.execute(() -> {
            try {
                com.personalassistant.app.data.model.MovieItem detail =
                        MovieRepository.getMovieById(siteBase != null ? siteBase : "", vodId);
                handler.post(() -> {
                    if (episodeSection != null && episodeSection.indexOfChild(loading) >= 0) {
                        episodeSection.removeView(loading);
                    }
                    if (detail != null) {
                        if (detail.title != null && !detail.title.isEmpty()) {
                            tvMovieTitle.setText(detail.title);
                            tvTitle.setText(detail.title);
                        }
                        if (detail.playLines != null && !detail.playLines.isEmpty()) {
                            playLines = detail.playLines;
                            buildEpisodeList();
                            buildLineButtons();
                        }
                        if (detail.playUrl != null && !detail.playUrl.isEmpty()) {
                            tvTitle.setText(detail.title != null ? detail.title : "");
                        }
                        if (currentUrl != null && !currentUrl.isEmpty()) {
                            initPlayer(currentUrl);
                        } else if (detail.playLines != null && !detail.playLines.isEmpty()) {
                            PlayLine firstLine = detail.playLines.get(0);
                            if (firstLine.episodes != null && !firstLine.episodes.isEmpty()) {
                                Episode firstEp = firstLine.episodes.get(0);
                                String resolved = MovieRepository.resolveVideoUrl(siteBase, firstEp.url);
                                currentUrl = resolved;
                                currentEpName = firstEp.name;
                                initPlayer(resolved);
                            }
                        }
                    } else {
                        loading.setText("加载失败");
                        loading.setTextColor(0xFFEF4444);
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    if (episodeSection != null && episodeSection.indexOfChild(loading) >= 0) {
                        episodeSection.removeView(loading);
                    }
                    Toast.makeText(MoviePlayerActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void buildEpisodeList() {
        if (rvEpisodes == null) return;

        episodeAdapter = new EpisodeAdapter(this, this::playEpisode);
        rvEpisodes.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvEpisodes.setAdapter(episodeAdapter);
        refreshEpisodeList();
    }

    private void refreshEpisodeList() {
        if (episodeAdapter == null || playLines.isEmpty()) return;

        ArrayList<Episode> allEps = new ArrayList<>();
        for (PlayLine line : playLines) {
            if (line.episodes != null) {
                for (Episode ep : line.episodes) {
                    allEps.add(ep);
                }
            }
        }
        episodeAdapter.setItems(allEps);
    }

    private void buildLineButtons() {
        LinearLayout lineButtons = findViewById(R.id.episode_line_buttons);
        lineButtons.removeAllViews();

        for (int i = 0; i < playLines.size(); i++) {
            PlayLine line = playLines.get(i);
            Button btn = new Button(this);
            btn.setText(line.lineName);
            btn.setTextSize(10);
            btn.setAllCaps(false);
            btn.setMinWidth(dp(60));
            btn.setPadding(dp(8), dp(6), dp(8), dp(6));

            boolean isActive = line.lineName.equals(this.lineName);
            btn.setTextColor(isActive ? 0xFFFFFFFF : 0xFF8899AA);
            btn.setBackgroundColor(isActive ? 0xFF7C3AED : 0x22FFFFFF);

            final int idx = i;
            btn.setOnClickListener(v -> {
                PlayLine selectedLine = playLines.get(idx);
                if (selectedLine.episodes != null && !selectedLine.episodes.isEmpty()) {
                    Episode firstEp = selectedLine.episodes.get(0);
                    String resolved = MovieRepository.resolveVideoUrl(siteBase, firstEp.url);
                    playEpisode(firstEp, resolved);
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(4);
            btn.setLayoutParams(params);
            lineButtons.addView(btn);
        }
    }

    private void playEpisode(Episode ep, String url) {
        String resolvedUrl = MovieRepository.resolveVideoUrl(siteBase, ep.url);
        playEpisode(ep.name, resolvedUrl);
    }

    private void playEpisode(String epName, String url) {
        this.currentEpName = epName;
        this.currentUrl = url;

        if (tvMovieTitle != null && vodId != null) {
            String baseTitle = tvMovieTitle.getText().toString();
            tvMovieTitle.setText(baseTitle + " - " + epName);
        }

        findViewById(R.id.error_overlay).setVisibility(View.GONE);
        initPlayer(url);
        saveProgressToPrefs(epName, url, 0);
    }

    private void initPlayer(String url) {
        if (url == null || url.isEmpty()) {
            showError("无效的视频链接");
            return;
        }

        if (player != null) {
            player.removeListener(playerListener);
            player.stop();
            player.release();
        }

        player = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        playerView.setKeepScreenOn(true);

        // Restore playback position
        long resumePosition = 0;
        if (currentEpName != null && currentUrl != null) {
            resumePosition = getSavedProgress(currentEpName, currentUrl);
        }

        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.setPlaybackSpeed(SPEEDS[currentSpeedIndex]);
        if (resumePosition > 0 && resumePosition < player.getDuration()) {
            player.seekTo(resumePosition);
        }
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(playerListener);
    }

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY) {
                saveProgressToPrefs(currentEpName, currentUrl,
                        player != null ? player.getCurrentPosition() : 0);
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            runOnUiThread(() -> {
                String errorMsg = formatErrorMessage(error);
                showError(errorMsg);
            });
        }
    };

    private String formatErrorMessage(PlaybackException error) {
        if (error == null) return "播放失败";
        int code = error.errorCode;
        switch (code) {
            case PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW:
                return "直播回放过期，尝试重新加载";
            case PlaybackException.ERROR_CODE_DECODING_FAILED:
                return "解码失败，尝试切换解析";
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
                return "网络连接失败，请检查网络后重试";
            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED:
                return "加载失败，可能是链接失效";
            default:
                return "播放失败: " + error.getLocalizedMessage();
        }
    }

    private void showError(String msg) {
        findViewById(R.id.error_overlay).setVisibility(View.VISIBLE);
        if (tvError != null) tvError.setText(msg);
    }

    private void playUrl(String url) {
        if (url == null || url.isEmpty()) {
            showError("无效的视频链接");
            return;
        }
        initPlayer(url);
    }

    private void loadParseConfigs() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MoviePlayerActivity.this);
                List<ParseConfigEntity> entities = db.parseConfigDao().getAllConfigsBlocking();

                if (entities != null) {
                    for (ParseConfigEntity e : entities) {
                        ParseConfig cfg = new ParseConfig();
                        cfg.name = e.name;
                        cfg.url = e.url;
                        cfg.type = e.type;
                        cfg.flags = new ArrayList<>();
                        if (e.flags != null && !e.flags.isEmpty()) {
                            for (String flag : e.flags.split(",")) {
                                cfg.flags.add(flag.trim());
                            }
                        }
                        parseConfigs.add(cfg);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }

            handler.post(() -> {
                if (!parseConfigs.isEmpty()) {
                    buildParserButtons();
                }
            });
        });
    }

    private void buildParserButtons() {
        if (parseConfigs.isEmpty() || parserButtons == null) return;

        parserButtons.removeAllViews();
        for (int i = 0; i < parseConfigs.size(); i++) {
            final ParseConfig cfg = parseConfigs.get(i);
            Button btn = new Button(this);
            btn.setText(cfg.name);
            btn.setTextSize(10);
            btn.setAllCaps(false);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundColor(0xFF7C3AED);
            btn.setPadding(dp(12), dp(6), dp(12), dp(6));

            btn.setOnClickListener(v -> {
                currentParse = cfg;
                applyParse(cfg, currentUrl);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(4);
            btn.setLayoutParams(params);
            parserButtons.addView(btn);
        }
        parserScroll.setVisibility(View.VISIBLE);
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

        new AlertDialog.Builder(this)
                .setTitle("选择解析")
                .setItems(names, (dialog, which) -> {
                    ParseConfig cfg = parseConfigs.get(which);
                    currentParse = cfg;
                    applyParse(cfg, currentUrl);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void applyParse(ParseConfig parse, String videoUrl) {
        if (videoUrl == null) {
            Toast.makeText(this, "当前无视频链接", Toast.LENGTH_SHORT).show();
            return;
        }

        String parseUrl = parse.url + videoUrl;
        if (parse.flags != null && !parse.flags.isEmpty()) {
            boolean hasTag = false;
            for (String flag : parse.flags) {
                if (flag != null && flag.contains("tag=")) {
                    hasTag = true;
                    String tagValue = flag.substring(flag.indexOf("tag=") + 4);
                    if (!parseUrl.contains("?")) {
                        parseUrl += "?tag=" + tagValue;
                    } else {
                        parseUrl += "&tag=" + tagValue;
                    }
                    break;
                }
            }
            if (!hasTag) {
                for (String flag : parse.flags) {
                    if (flag != null && flag.contains("url=")) {
                        parseUrl = videoUrl;
                        break;
                    }
                }
            }
        }

        resolveAndPlay(parseUrl);
    }

    private void resolveAndPlay(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            showError("解析结果为空");
            return;
        }

        TextView loading = new TextView(this);
        loading.setText("解析中...");
        loading.setTextColor(0xFF667788);
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setTextSize(14);

        LinearLayout root = findViewById(R.id.root_layout);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(100);
        root.addView(loading, 1, lp);

        executor.execute(() -> {
            final String finalUrl = urlString;
            try {
                String html = com.personalassistant.app.data.network.OkHttpUtil.get(urlString);
                handler.post(() -> {
                    if (root.indexOfChild(loading) >= 0) {
                        root.removeView(loading);
                    }
                    if (html == null || html.isEmpty()) {
                        showError("解析返回空结果");
                        return;
                    }
                    String videoUrl = MovieRepository.parseHtmlVideoUrl(html);
                    if (videoUrl != null && !videoUrl.isEmpty()) {
                        currentUrl = videoUrl;
                        initPlayer(videoUrl);
                    } else {
                        videoUrl = MovieRepository.parseHtmlVideoUrlFromJsoup(html);
                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            currentUrl = videoUrl;
                            initPlayer(videoUrl);
                        } else {
                            showError("解析未找到视频地址，已尝试直接播放");
                            playUrl(finalUrl);
                        }
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    if (root.indexOfChild(loading) >= 0) {
                        root.removeView(loading);
                    }
                    showError("解析失败: " + e.getMessage());
                });
            }
        });
    }

    private void tryNextParse() {
        if (parseConfigs.isEmpty()) return;

        int currentIndex = currentParse != null ? parseConfigs.indexOf(currentParse) : -1;
        int nextIndex = (currentIndex + 1) % parseConfigs.size();
        currentParse = parseConfigs.get(nextIndex);

        if (currentUrl != null) {
            applyParse(currentParse, currentUrl);
        }
    }

    private void showSpeedDialog() {
        String[] speeds = {"0.5x", "1x", "1.5x", "2x"};
        new AlertDialog.Builder(this)
                .setTitle("播放速度")
                .setSingleChoiceItems(speeds, currentSpeedIndex, (dialog, which) -> {
                    currentSpeedIndex = which;
                    if (player != null) {
                        player.setPlaybackSpeed(SPEEDS[which]);
                    }
                    Button btnSpeed = findViewById(R.id.btn_speed);
                    if (btnSpeed != null) btnSpeed.setText(speeds[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            setSystemUIHidden(true);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setSystemUIHidden(false);
        }
    }

    private void setSystemUIHidden(boolean hidden) {
        View decorView = getWindow().getDecorView();
        if (hidden) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        } else {
            decorView.setSystemUiVisibility(0);
        }
    }

    private void saveToFavorites() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MoviePlayerActivity.this);
                FavoriteEntity entity = new FavoriteEntity();
                entity.movieId = vodId;
                String movieTitle = tvMovieTitle != null ? tvMovieTitle.getText().toString() : "";
                entity.movieTitle = movieTitle;
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

    private void saveProgressToPrefs(String epName, String url, long position) {
        if (epName == null || url == null) return;
        String key = "play_progress_" + siteBase + "|" + epName + "_" + url;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            key = sb.toString();
        } catch (Exception ignored) {
        }

        android.content.SharedPreferences prefs = getSharedPreferences("player_prefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key + "_pos", position);
        editor.putLong(key + "_time", System.currentTimeMillis());
        editor.apply();
    }

    private long getSavedProgress(String epName, String url) {
        if (epName == null || url == null) return 0;
        String key = "play_progress_" + siteBase + "|" + epName + "_" + url;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            key = sb.toString();
        } catch (Exception ignored) {
        }

        android.content.SharedPreferences prefs = getSharedPreferences("player_prefs", MODE_PRIVATE);
        long timestamp = prefs.getLong(key + "_time", 0);
        if (timestamp == 0) return 0;

        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        long maxAge = 7L * 24 * 60 * 60 * 1000;
        if (diff > maxAge) return 0;

        return prefs.getLong(key + "_pos", 0);
    }

    private void saveHistory() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MoviePlayerActivity.this);
                long position = 0;
                long duration = 0;
                try {
                    if (player != null) {
                        position = player.getCurrentPosition();
                        duration = player.getDuration();
                    }
                } catch (Exception ignored) {
                }

                HistoryEntity entity = new HistoryEntity();
                entity.movieId = vodId;
                entity.movieTitle = tvMovieTitle != null ? tvMovieTitle.getText().toString() : "";
                entity.siteName = siteName != null ? siteName : "";
                entity.siteBase = siteBase != null ? siteBase : "";
                entity.playFrom = lineName != null ? lineName : "";
                entity.episodeName = currentEpName != null ? currentEpName : "";
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
        if (playerView != null) playerView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerView != null) playerView.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isFullscreen) {
            setSystemUIHidden(true);
        }
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (isFullscreen && playerView != null) {
            boolean visible = playerView.getVisibility() == View.VISIBLE;
            setSystemUIHidden(!visible);
            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
            playerView.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.isPlaying()) {
            player.setPlayWhenReady(false);
        }
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
            player.removeListener(playerListener);
            player.stop();
            player.release();
            player = null;
        }
    }

    int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    static class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
        private final List<Episode> items = new ArrayList<>();
        private final java.util.function.BiConsumer<Episode, String> onPlay;
        private final android.content.Context ctx;

        EpisodeAdapter(android.content.Context context, java.util.function.BiConsumer<Episode, String> onPlay) {
            this.ctx = context;
            this.onPlay = onPlay;
        }

        void setItems(List<Episode> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.content.Context context = parent.getContext();
            Button btn = new Button(context);
            btn.setAllCaps(false);
            btn.setTextColor(0xFFDFE8FF);
            btn.setBackgroundColor(0x12FFFFFF);
            btn.setPadding(dp(0, context), dp(10, context), dp(0, context), dp(10, context));
            btn.setTextSize(12);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.width = dp(56, context);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.rightMargin = dp(3, context);
            params.bottomMargin = dp(6, context);
            btn.setLayoutParams(params);

            return new ViewHolder(btn);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Episode ep = items.get(position);
            String displayName = ep.name;
            if (displayName != null && displayName.contains(".")) {
                displayName = displayName.substring(displayName.lastIndexOf(".") + 1);
            } else if (displayName != null && displayName.contains(" ")) {
                displayName = displayName.substring(displayName.lastIndexOf(" ") + 1);
            }
            if (displayName == null || displayName.isEmpty()) {
                displayName = String.valueOf(position + 1);
            }
            holder.button.setText(displayName);
            holder.button.setOnClickListener(v -> {
                if (ep.url != null) {
                    String resolved = com.personalassistant.app.data.repository.MovieRepository.resolveVideoUrl("", ep.url);
                    onPlay.accept(ep, resolved);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            Button button;
            ViewHolder(View v) {
                super(v);
                button = (Button) v;
            }
        }

        static int dp(int val, android.content.Context context) {
            return (int) (val * context.getResources().getDisplayMetrics().density);
        }
    }
}
