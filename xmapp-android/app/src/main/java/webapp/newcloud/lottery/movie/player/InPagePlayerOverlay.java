package webapp.newcloud.lottery.movie.player;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.video.VideoSize;

public class InPagePlayerOverlay implements Player.Listener {
    private static final String TAG = "InPagePlayerOverlay";

    private WebView webView;
    private FrameLayout overlay;
    private SimpleExoPlayer exoPlayer;
    private SurfaceView surfaceView;
    private TextView titleView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;
    private String currentUrl;

    public InPagePlayerOverlay(WebView webView, String title, String url, boolean isLive) {
        this.webView = webView;
        this.currentUrl = url;

        overlay = new FrameLayout(webView.getContext());
        // 播放器高度: 40% 屏幕高度，最小 280dp
        int screenHeight = webView.getContext().getResources().getDisplayMetrics().heightPixels;
        int minHeight = (int) (280 * webView.getContext().getResources().getDisplayMetrics().density);
        int overlayHeight = Math.max(screenHeight * 2 / 5, minHeight);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                overlayHeight));
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setVisibility(View.GONE);
        overlay.setTag("InPagePlayerOverlay");

        // SurfaceView 必须在其他控件之前添加，确保它在背景层
        surfaceView = new SurfaceView(webView.getContext());
        surfaceView.setZOrderMediaOverlay(true);
        FrameLayout.LayoutParams surfaceParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        surfaceView.setLayoutParams(surfaceParams);
        overlay.addView(surfaceView);

        // 关闭按钮（在 SurfaceView 上方）
        ImageView closeBtn = new ImageView(webView.getContext());
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.END | Gravity.TOP;
        closeParams.setMargins(8, 8, 8, 8);
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setAlpha(0.8f);
        closeBtn.setOnClickListener(v -> dismiss());
        overlay.addView(closeBtn);

        // 标题（在 SurfaceView 上方）
        titleView = new TextView(webView.getContext());
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(14);
        titleView.setMaxLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleView.setText(title);
        titleView.setShadowLayer(2, 0, 1, Color.BLACK);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.BOTTOM | Gravity.START;
        titleParams.setMargins(8, 0, 80, 8);
        titleView.setLayoutParams(titleParams);
        overlay.addView(titleView);

        // 将 overlay 添加到 WebView 父容器最上层
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent != null) {
            parent.addView(overlay);
            Log.d(TAG, "Overlay added to parent: childCount=" + parent.getChildCount()
                    + " overlayIndex=" + parent.indexOfChild(overlay)
                    + " parentClass=" + parent.getClass().getSimpleName());
        } else {
            Log.e(TAG, "WebView parent is null, cannot add overlay");
        }

        // 初始化 ExoPlayer（必须在主线程）
        mainHandler.post(() -> initExoPlayer(url));
    }

    private void initExoPlayer(String url) {
        try {
            Log.d(TAG, "initExoPlayer starting: url=" + url + " thread=" + Thread.currentThread().getName());
            LoadControl loadControl = new DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15000, 60000, 500, 2000)
                    .build();
            exoPlayer = new SimpleExoPlayer.Builder(webView.getContext())
                    .setLoadControl(loadControl)
                    .build();

            exoPlayer.setVideoSurfaceView(surfaceView);
            exoPlayer.addListener(this);

            if (url != null && !url.isEmpty()) {
                MediaItem mediaItem = MediaItem.fromUri(url);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.setPlayWhenReady(true);
                Log.d(TAG, "ExoPlayer prepare() called, playWhenReady=true");
            } else {
                Log.e(TAG, "URL is empty, cannot play");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ExoPlayer", e);
        }
    }

    public void show() {
        mainHandler.post(() -> {
            if (overlay != null) {
                overlay.setVisibility(View.VISIBLE);
                overlay.bringToFront();
                isShowing = true;
                Log.d(TAG, "Overlay shown: visibility=" + overlay.getVisibility()
                        + " size=" + overlay.getWidth() + "x" + overlay.getHeight()
                        + " surfaceSize=" + surfaceView.getWidth() + "x" + surfaceView.getHeight());
            }
        });
    }

    public void dismiss() {
        mainHandler.post(() -> {
            if (overlay != null) {
                overlay.setVisibility(View.GONE);
                isShowing = false;
                int parentChildCount = -1;
                ViewGroup parent = (ViewGroup) overlay.getParent();
                if (parent != null) {
                    parentChildCount = parent.getChildCount();
                    parent.removeView(overlay);
                }
                releasePlayer();
                Log.d(TAG, "Overlay dismissed and removed from parent: wasChildCount=" + parentChildCount);
            }
        });
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.removeListener(this);
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
            Log.d(TAG, "ExoPlayer released");
        }
        if (surfaceView != null && surfaceView.getHolder() != null) {
            android.view.Surface s = surfaceView.getHolder().getSurface();
            if (s != null && s.isValid()) {
                s.release();
            }
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    // ===== Player.Listener =====

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        String stateStr;
        switch (playbackState) {
            case Player.STATE_IDLE: stateStr = "IDLE"; break;
            case Player.STATE_BUFFERING: stateStr = "BUFFERING"; break;
            case Player.STATE_READY: stateStr = "READY"; break;
            case Player.STATE_ENDED: stateStr = "ENDED"; break;
            default: stateStr = "UNKNOWN(" + playbackState + ")"; break;
        }
        Log.d(TAG, "PlaybackState: " + stateStr + " url=" + currentUrl);

        if (playbackState == Player.STATE_READY && exoPlayer != null) {
            VideoSize vs = exoPlayer.getVideoSize();
            Log.d(TAG, "VideoSize: " + vs.width + "x" + vs.height
                    + " unappliedRotation=" + vs.unappliedRotationDegrees
                    + " pixelRatio=" + vs.pixelWidthHeightRatio);
        }
    }

    @Override
    public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
        String errorMsg = error != null ? error.getMessage() : "null";
        String errorCode = error != null ? error.getErrorCodeName() : "null";
        Log.e(TAG, "PlayerError: code=" + errorCode + " msg=" + errorMsg
                + " url=" + currentUrl, error);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG, "IsPlaying: " + isPlaying + " url=" + currentUrl);
    }
}
