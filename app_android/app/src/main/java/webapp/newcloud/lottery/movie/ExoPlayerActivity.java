package webapp.newcloud.lottery.movie;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * EXO Player 原生 Activity
 * 支持集数列表、播放进度保存、倍速切换、手势控制
 */
public class ExoPlayerActivity extends Activity {
    
    private static final String TAG = "ExoPlayerActivity";
    private static final String PREFS_NAME = "ExoPlayerPrefs";
    private static final int[] SPEED_OPTIONS = {50, 75, 100, 125, 150, 200}; // 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x
    
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private TextView titleTextView;
    private Button closeButton;
    private TextView errorMessageView;
    private LinearLayout episodesContainer;
    private TextView speedTextView;
    private TextView timeTextView;
    private TextView progressTextView;
    private ProgressBar loadingIndicator;
    
    private JSONArray currentEpisodes = null;
    private int currentEpisodeIndex = 0;
    private String currentVideoTitle = "";
    private String currentVideoUrl = "";
    private int currentSpeedIndex = 2; // 默认1x
    
    private SharedPreferences sharedPreferences;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // 手势检测
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 隐藏状态栏，全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // 创建原生布局
        createLayout();
        
        // 初始化 ExoPlayer
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        exoPlayer = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();
        playerView.setPlayer(exoPlayer);
        
        // 设置播放器状态监听
        setupPlayerListeners();
        
        // 初始化手势检测
        setupGestureDetector();
        
        // 从 Intent 获取播放参数
        handleIntent(getIntent());
        
        // 更新时间显示
        startProgressUpdate();
    }
    
    private void createLayout() {
        // 根布局
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setBackgroundColor(0xFF000000);
        
        // 顶部控制栏
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0x80000000);
        topBar.setPadding(8, 8, 8, 8);
        
        titleTextView = new TextView(this);
        titleTextView.setTextSize(16);
        titleTextView.setTextColor(0xFFFFFFFF);
        titleTextView.setSingleLine(true);
        titleTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleTextView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        
        speedTextView = new TextView(this);
        speedTextView.setTextSize(14);
        speedTextView.setTextColor(0xFF90CAF9);
        speedTextView.setText("1.0x");
        speedTextView.setPadding(16, 0, 16, 0);
        speedTextView.setOnClickListener(v -> showSpeedDialog());
        
        closeButton = new Button(this);
        closeButton.setText("✕");
        closeButton.setTextSize(14);
        closeButton.setTextColor(0xFFFFFFFF);
        closeButton.setBackgroundColor(0x00000000);
        closeButton.setOnClickListener(v -> finish());
        
        topBar.addView(titleTextView);
        topBar.addView(speedTextView);
        topBar.addView(closeButton);
        
        // 播放器视图
        playerView = new PlayerView(this);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        
        // 加载指示器
        loadingIndicator = new ProgressBar(this);
        loadingIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        loadingIndicator.setVisibility(View.GONE);
        
        // 底部信息栏
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(0x80000000);
        bottomBar.setPadding(8, 4, 8, 4);
        
        timeTextView = new TextView(this);
        timeTextView.setTextSize(12);
        timeTextView.setTextColor(0xFFFFFFFF);
        timeTextView.setText("00:00 / 00:00");
        
        progressTextView = new TextView(this);
        progressTextView.setTextSize(12);
        progressTextView.setTextColor(0xFFAAAAAA);
        progressTextView.setText("");
        progressTextView.setPadding(16, 0, 0, 0);
        
        bottomBar.addView(timeTextView);
        bottomBar.addView(progressTextView);
        
        // 集数列表容器（横向滚动）
        TextView episodesTitle = new TextView(this);
        episodesTitle.setText("选集");
        episodesTitle.setTextSize(14);
        episodesTitle.setTextColor(0xFFFFFFFF);
        episodesTitle.setPadding(8, 8, 8, 4);
        
        HorizontalScrollView episodesScroll = new HorizontalScrollView(this);
        episodesScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        episodesScroll.setHorizontalScrollBarEnabled(false);
        
        episodesContainer = new LinearLayout(this);
        episodesContainer.setOrientation(LinearLayout.HORIZONTAL);
        episodesContainer.setPadding(8, 4, 8, 8);
        
        episodesScroll.addView(episodesContainer);
        
        // 错误信息
        errorMessageView = new TextView(this);
        errorMessageView.setTextSize(14);
        errorMessageView.setTextColor(0xFFEF5350);
        errorMessageView.setGravity(android.view.Gravity.CENTER);
        errorMessageView.setPadding(16, 16, 16, 16);
        errorMessageView.setVisibility(View.GONE);
        
        // 添加到根布局
        rootLayout.addView(topBar);
        rootLayout.addView(playerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        rootLayout.addView(bottomBar);
        rootLayout.addView(episodesTitle);
        rootLayout.addView(episodesScroll);
        rootLayout.addView(errorMessageView);
        
        setContentView(rootLayout);
    }
    
    private void setupPlayerListeners() {
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(() -> {
                    errorMessageView.setText("播放失败: " + error.getMessage());
                    errorMessageView.setVisibility(View.VISIBLE);
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(ExoPlayerActivity.this, "播放失败: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                runOnUiThread(() -> {
                    if (isPlaying) {
                        errorMessageView.setVisibility(View.GONE);
                    }
                    loadingIndicator.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
                });
            }
            
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                runOnUiThread(() -> {
                    if (playbackState == Player.STATE_READY) {
                        loadingIndicator.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        loadingIndicator.setVisibility(View.VISIBLE);
                    } else if (playbackState == Player.STATE_ENDED) {
                        // 播放结束，自动播放下一集
                        playNextEpisode();
                    }
                });
            }
        });
    }
    
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 双击切换播放/暂停
                if (exoPlayer != null) {
                    exoPlayer.setPlayWhenReady(!exoPlayer.getPlayWhenReady());
                }
                return true;
            }
            
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // 单击显示/隐藏控制栏
                return super.onSingleTapConfirmed(e);
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // 水平滑动：快进/快退
                float deltaX = e2.getX() - e1.getX();
                if (Math.abs(deltaX) > SWIPE_THRESHOLD && exoPlayer != null) {
                    long currentPosition = exoPlayer.getCurrentPosition();
                    long seekAmount = (long)(deltaX * 500); // 每1px = 500ms
                    long newPosition = Math.max(0, Math.min(exoPlayer.getDuration(), currentPosition + seekAmount));
                    exoPlayer.seekTo(newPosition);
                    return true;
                }
                return false;
            }
        });
        
        playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }
    
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        
        String jsonData = intent.getStringExtra("play_data");
        if (jsonData != null) {
            try {
                JSONObject data = new JSONObject(jsonData);
                currentVideoTitle = data.optString("title", "视频");
                String url = data.optString("url", "");
                boolean isLive = data.optBoolean("isLive", false);

                // 如果 url 为空，尝试从 episodes 数组中获取
                if (url.isEmpty()) {
                    JSONArray episodes = data.optJSONArray("episodes");
                    if (episodes != null && episodes.length() > 0) {
                        currentEpisodes = episodes;
                        currentEpisodeIndex = 0;
                        buildEpisodesList();
                        playEpisode(0);
                        return;
                    }
                }

                currentVideoUrl = url;
                titleTextView.setText(currentVideoTitle);

                if (!url.isEmpty()) {
                    playUrl(url, isLive);
                } else {
                    errorMessageView.setText("无效的视频地址");
                    errorMessageView.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                errorMessageView.setText("解析播放数据失败: " + e.getMessage());
                errorMessageView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void playUrl(String url, boolean isLive) {
        if (exoPlayer == null) return;
        
        currentVideoUrl = url;
        errorMessageView.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.VISIBLE);
        
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(Uri.parse(url));
        
        if (isLive) {
            mediaItemBuilder.setLiveConfiguration(
                    new MediaItem.LiveConfiguration.Builder()
                            .setMaxOffsetMs(30_000)
                            .build()
            );
        }
        
        MediaItem mediaItem = mediaItemBuilder.build();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        
        // 尝试恢复播放进度
        String progressKey = "progress_" + md5(url);
        long savedProgress = sharedPreferences.getLong(progressKey, 0);
        if (savedProgress > 0 && !isLive) {
            exoPlayer.seekTo(savedProgress);
        }
        
        exoPlayer.setPlayWhenReady(true);
    }
    
    private void buildEpisodesList() {
        if (currentEpisodes == null || episodesContainer == null) return;
        
        episodesContainer.removeAllViews();
        
        for (int i = 0; i < currentEpisodes.length(); i++) {
            try {
                JSONObject ep = currentEpisodes.getJSONObject(i);
                String name = ep.optString("name", "第" + (i + 1) + "集");
                
                Button btn = new Button(this);
                btn.setText(name);
                btn.setTextSize(12);
                btn.setPadding(12, 8, 12, 8);
                btn.setTextColor(i == currentEpisodeIndex ? 0xFFFFFFFF : 0xFF90CAF9);
                btn.setBackgroundColor(i == currentEpisodeIndex ? 0xFF7C3AED : 0x307C3AED);
                
                final int index = i;
                btn.setOnClickListener(v -> playEpisode(index));
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(4, 0, 4, 0);
                btn.setLayoutParams(lp);
                
                episodesContainer.addView(btn);
            } catch (Exception e) {
                Log.e(TAG, "buildEpisodesList error: " + e.getMessage());
            }
        }
    }
    
    private void playEpisode(int index) {
        if (currentEpisodes == null || index < 0 || index >= currentEpisodes.length()) return;
        
        try {
            currentEpisodeIndex = index;
            JSONObject ep = currentEpisodes.getJSONObject(index);
            String url = ep.optString("url", "");
            String name = ep.optString("name", "第" + (index + 1) + "集");
            
            if (url.isEmpty()) {
                Toast.makeText(this, "无播放地址", Toast.LENGTH_SHORT).show();
                return;
            }
            
            currentVideoUrl = url;
            titleTextView.setText(currentVideoTitle + " - " + name);
            
            playUrl(url, false);
            buildEpisodesList(); // 更新选中状态
            
        } catch (Exception e) {
            Toast.makeText(this, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void playNextEpisode() {
        if (currentEpisodes != null && currentEpisodeIndex < currentEpisodes.length() - 1) {
            playEpisode(currentEpisodeIndex + 1);
        }
    }
    
    private void showSpeedDialog() {
        // 创建倍速选择对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("播放速度");
        
        String[] speedLabels = new String[SPEED_OPTIONS.length];
        for (int i = 0; i < SPEED_OPTIONS.length; i++) {
            speedLabels[i] = (SPEED_OPTIONS[i] / 100.0f) + "x";
        }
        
        builder.setSingleChoiceItems(speedLabels, currentSpeedIndex, (dialog, which) -> {
            setSpeed(which);
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    private void setSpeed(int speedIndex) {
        if (speedIndex < 0 || speedIndex >= SPEED_OPTIONS.length) return;
        
        currentSpeedIndex = speedIndex;
        float speed = SPEED_OPTIONS[speedIndex] / 100.0f;
        
        if (exoPlayer != null) {
            exoPlayer.setPlaybackSpeed(speed);
        }
        
        speedTextView.setText(speed + "x");
    }
    
    private void startProgressUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateProgress();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    private void updateProgress() {
        if (exoPlayer == null) return;
        
        long currentPosition = exoPlayer.getCurrentPosition();
        long duration = exoPlayer.getDuration();
        
        if (duration > 0) {
            String current = formatTime(currentPosition);
            String total = formatTime(duration);
            timeTextView.setText(current + " / " + total);
            
            // 保存播放进度
            if (currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
                String progressKey = "progress_" + md5(currentVideoUrl);
                sharedPreferences.edit().putLong(progressKey, currentPosition).apply();
            }
        }
    }
    
    private String formatTime(long ms) {
        int seconds = (int) (ms / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
            // 保存播放进度
            saveProgress();
        }
    }
    
    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (exoPlayer != null) {
            saveProgress();
            exoPlayer.release();
            exoPlayer = null;
        }
        super.onDestroy();
    }
    
    private void saveProgress() {
        if (exoPlayer == null || currentVideoUrl == null || currentVideoUrl.isEmpty()) return;
        
        long currentPosition = exoPlayer.getCurrentPosition();
        String progressKey = "progress_" + md5(currentVideoUrl);
        sharedPreferences.edit().putLong(progressKey, currentPosition).apply();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 屏幕旋转时不重建Activity，由系统自动处理布局
    }
    
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        // 处理音量键
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            // 让系统处理音量
            return super.onKeyDown(keyCode, event);
        }
        
        // 处理返回键
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        
        // 处理方向键（TV遥控器）
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
            if (exoPlayer != null) {
                exoPlayer.seekTo(Math.max(0, exoPlayer.getCurrentPosition() - 10000));
            }
            return true;
        }
        
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (exoPlayer != null) {
                exoPlayer.seekTo(Math.min(exoPlayer.getDuration(), exoPlayer.getCurrentPosition() + 10000));
            }
            return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    // ===== 公共方法（供JS调用）=====
    
    public void play(String jsonData) {
        try {
            JSONObject data = new JSONObject(jsonData);
            String title = data.optString("title", "视频");
            String url = data.optString("url", "");
            boolean isLive = data.optBoolean("isLive", false);
            
            Intent intent = new Intent(this, ExoPlayerActivity.class);
            intent.putExtra("play_data", jsonData);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "播放数据解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private static String md5(String input) {
        if (input == null) return "null";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
    
    public void playEpisodes(String jsonData) {
        try {
            JSONObject data = new JSONObject(jsonData);
            JSONArray episodes = data.optJSONArray("episodes");
            
            if (episodes != null && episodes.length() > 0) {
                Intent intent = new Intent(this, ExoPlayerActivity.class);
                intent.putExtra("play_data", jsonData);
                startActivity(intent);
            } else {
                Toast.makeText(this, "没有可播放的集数", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "播放数据解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}