package webapp.newcloud.lottery.movie;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * EXO Player 原生 Activity
 * 通过 WebView 的 addJavascriptInterface 调用
 */
public class ExoPlayerActivity extends Activity {
    
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private TextView titleTextView;
    private Button closeButton;
    private TextView errorMessageView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建原生布局
        ViewGroup rootView = new ViewGroup(this) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                // 布局子视图
                int paddingLeft = getPaddingLeft();
                int paddingTop = getPaddingTop();
                
                // 标题栏
                if (titleTextView != null) {
                    titleTextView.layout(paddingLeft, paddingTop, getWidth() - paddingLeft, paddingTop + 60);
                }
                if (closeButton != null) {
                    closeButton.layout(getWidth() - 80 - paddingLeft, paddingTop + 10, getWidth() - paddingLeft, paddingTop + 50);
                }
                
                // 播放器视图
                if (playerView != null) {
                    playerView.layout(0, 60, getWidth(), getHeight() - 60);
                }
                
                // 错误信息
                if (errorMessageView != null) {
                    errorMessageView.layout(paddingLeft, getHeight() / 2 - 50, getWidth() - paddingLeft, getHeight() / 2 + 50);
                }
            }
        };
        
        rootView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        // 标题 TextView
        titleTextView = new TextView(this);
        titleTextView.setTextSize(18);
        titleTextView.setTextColor(0xFFFFFFFF);
        titleTextView.setPadding(16, 10, 80, 10);
        titleTextView.setSingleLine(true);
        titleTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        rootView.addView(titleTextView);
        
        // 关闭按钮
        closeButton = new Button(this);
        closeButton.setText("✕");
        closeButton.setTextSize(16);
        closeButton.setTextColor(0xFFFFFFFF);
        closeButton.setBackgroundColor(0x80000000);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        rootView.addView(closeButton);
        
        // 播放器视图
        playerView = new PlayerView(this);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        rootView.addView(playerView);
        
        // 错误信息 TextView
        errorMessageView = new TextView(this);
        errorMessageView.setTextSize(14);
        errorMessageView.setTextColor(0xFFaaaaaa);
        errorMessageView.setGravity(android.view.Gravity.CENTER);
        errorMessageView.setVisibility(View.GONE);
        rootView.addView(errorMessageView);
        
        setContentView(rootView);
        
        // 初始化 ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);
        
        // 设置播放器状态监听
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorMessageView.setText("播放失败: " + error.getMessage());
                        errorMessageView.setVisibility(View.VISIBLE);
                        Toast.makeText(ExoPlayerActivity.this, "播放失败: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
            
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorMessageView.setVisibility(View.GONE);
                    }
                });
            }
        });
        
        // 从 Intent 获取播放参数
        Intent intent = getIntent();
        if (intent != null) {
            String jsonData = intent.getStringExtra("play_data");
            if (jsonData != null) {
                try {
                    JSONObject data = new JSONObject(jsonData);
                    String title = data.optString("title", "视频");
                    String url = data.optString("url", "");
                    boolean isLive = data.optBoolean("isLive", false);
                    
                    titleTextView.setText(title);
                    
                    if (!url.isEmpty()) {
                        // 设置媒体项
                        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
                        exoPlayer.setMediaItem(mediaItem);
                        exoPlayer.prepare();
                        exoPlayer.setPlayWhenReady(true);
                    } else {
                        errorMessageView.setText("无效的视频地址");
                        errorMessageView.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    errorMessageView.setText("解析播放数据失败");
                    errorMessageView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
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
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
    
    /**
     * 从 WebView JavaScript 调用的方法
     * 通过 addJavascriptInterface 注册
     */
    public void play(String jsonData) {
        try {
            // 验证 JSON 可解析（确保调用方传入有效数据）
            new JSONObject(jsonData);
            Intent intent = new Intent(this, ExoPlayerActivity.class);
            intent.putExtra("play_data", jsonData);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "播放数据解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 播放集数列表
     */
    public void playEpisodes(String jsonData) {
        try {
            JSONObject data = new JSONObject(jsonData);
            String title = data.optString("title", "视频");
            JSONArray episodes = data.optJSONArray("episodes");

            if (episodes != null && episodes.length() > 0) {
                // 默认播放第一集
                JSONObject firstEpisode = episodes.getJSONObject(0);
                String episodeTitle = firstEpisode.optString("name", title);
                String episodeUrl = firstEpisode.optString("url", "");

                JSONObject playData = new JSONObject();
                playData.put("title", episodeTitle);
                playData.put("url", episodeUrl);
                playData.put("isLive", false);

                play(playData.toString());
            } else {
                Toast.makeText(this, "没有可播放的集数", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "播放数据解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
