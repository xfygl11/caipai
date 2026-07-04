package com.personalassistant.app.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class LivePlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String channelName = getIntent().getStringExtra("channel_name");
        String channelUrl = getIntent().getStringExtra("channel_url");
        String channelLogo = getIntent().getStringExtra("channel_logo");
        String sourceName = getIntent().getStringExtra("source_name");

        if (channelName != null) setTitle(channelName);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);

        // Top info bar
        LinearLayout infoBar = new LinearLayout(this);
        infoBar.setOrientation(LinearLayout.HORIZONTAL);
        infoBar.setBackgroundColor(0xFF111827);
        infoBar.setPadding(dp(12), dp(8), dp(12), dp(8));

        TextView tvInfo = new TextView(this);
        tvInfo.setTextColor(0xFFFFFFFF);
        tvInfo.setTextSize(14);
        StringBuilder info = new StringBuilder();
        if (sourceName != null && !sourceName.isEmpty()) info.append(sourceName).append(" - ");
        if (channelName != null) info.append(channelName);
        tvInfo.setText(info.toString());
        infoBar.addView(tvInfo);

        root.addView(infoBar);

        // Player
        playerView = new PlayerView(this);
        playerView.setResizeMode(1); // RESIZE_MODE_FILL = 1
        playerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(playerView);

        setContentView(root);
        initPlayer(channelUrl);
    }

    private void initPlayer(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "无效的视频链接", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(() -> Toast.makeText(LivePlayerActivity.this,
                        "播放失败: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.setPlayWhenReady(false);
        playerView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerView.onResume();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    int dp(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }
}
