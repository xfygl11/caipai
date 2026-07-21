package webapp.newcloud.lottery.movie.player;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class InlinePlayerOverlay implements Player.Listener {
    private static final String TAG = "InlinePlayerOverlay";

    private Dialog dialog;
    private SimpleExoPlayer exoPlayer;
    private PlayerView playerView;
    private FrameLayout overlayContainer;
    private View webView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControllerRunnable;

    public InlinePlayerOverlay(Context context, View webView, String title, String url, boolean isLive) {
        Log.d(TAG, "InlinePlayerOverlay constructor called: title=" + title + " url=" + url + " isLive=" + isLive);
        this.webView = webView;

        dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                releasePlayer();
            }
        });

        overlayContainer = new FrameLayout(context);
        overlayContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 播放器视图
        playerView = new PlayerView(context);
        FrameLayout.LayoutParams playerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        playerView.setLayoutParams(playerParams);
        overlayContainer.addView(playerView);

        // 关闭按钮
        ImageView closeBtn = new ImageView(context);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(16, 16, 16, 16);
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setAlpha(0.7f);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        overlayContainer.addView(closeBtn);

        // 标题
        TextView titleView = new TextView(context);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(16);
        titleView.setMaxLines(2);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleView.setText(title);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        titleParams.setMargins(32, 0, 32, 60);
        titleView.setLayoutParams(titleParams);
        titleView.setBackgroundColor(0x80000000);
        overlayContainer.addView(titleView);

        dialog.setContentView(overlayContainer);
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setGravity(Gravity.CENTER);

        // 在主线程初始化播放器
        final String finalUrl = url;
        final String finalTitle = title;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Initializing ExoPlayer on main thread");
                    LoadControl loadControl = new DefaultLoadControl.Builder()
                            .setBufferDurationsMs(30000, 120000, 1000, 5000)
                            .build();
                    exoPlayer = new SimpleExoPlayer.Builder(context).setLoadControl(loadControl).build();
                    playerView.setPlayer(exoPlayer);
                    playerView.setUseController(false);
                    playerView.setResizeMode(0);

                    if (finalUrl != null && !finalUrl.isEmpty()) {
                        Log.d(TAG, "Setting media item: " + finalUrl);
                        MediaItem mediaItem = new MediaItem.Builder().setUri(finalUrl).build();
                        exoPlayer.setMediaItem(mediaItem);
                        exoPlayer.prepare();
                        exoPlayer.setPlayWhenReady(true);
                        Log.d(TAG, "Media item set, prepare() and setPlayWhenReady(true) called");
                    } else {
                        Log.w(TAG, "URL is null or empty, not setting media item");
                    }

                    exoPlayer.addListener(InlinePlayerOverlay.this);

                    // 音频焦点
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing ExoPlayer", e);
                }
            }
        });
    }

    public void show() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "show() called, dialog.isShowing=" + dialog.isShowing());
                if (!dialog.isShowing()) {
                    dialog.show();
                    Log.d(TAG, "Dialog shown successfully");
                } else {
                    Log.d(TAG, "Dialog already showing");
                }
            }
        });
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.removeListener(this);
            exoPlayer.release();
            exoPlayer = null;
        }
        AudioManager audioManager = (AudioManager) overlayContainer.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                Log.d(TAG, "Player ready");
                break;
            case Player.STATE_BUFFERING:
                Log.d(TAG, "Player buffering");
                break;
            case Player.STATE_ENDED:
                Log.d(TAG, "Player ended");
                break;
        }
    }

    @Override
    public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
        Log.e(TAG, "Player error: " + error.getMessage(), error);
    }
}
