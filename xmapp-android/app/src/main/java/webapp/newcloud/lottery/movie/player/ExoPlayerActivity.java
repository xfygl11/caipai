package webapp.newcloud.lottery.movie.player;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class ExoPlayerActivity extends Activity implements Player.Listener {

    private static final String TAG = "ExoPlayerActivity";

    private PlayerView playerView;
    private SimpleExoPlayer exoPlayer;
    private TextView titleView;
    private boolean isEpisodesMode = false;
    private int currentEpisodeIndex = 0;
    private List<String> episodeTitles = new ArrayList<>();
    private List<String> episodeUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(webapp.newcloud.lottery.movie.R.layout.activity_exoplayer);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        playerView = findViewById(webapp.newcloud.lottery.movie.R.id.player_view);
        titleView = findViewById(webapp.newcloud.lottery.movie.R.id.episodeTitle);

        String title = getIntent().getStringExtra("title");
        String url = getIntent().getStringExtra("url");
        String poster = getIntent().getStringExtra("poster");
        isEpisodesMode = getIntent().getBooleanExtra("EPISODES_MODE", false);

        String[] epTitles = getIntent().getStringArrayExtra("epTitles");
        String[] epUrls = getIntent().getStringArrayExtra("epUrls");

        if (epTitles != null && epUrls != null && epTitles.length == epUrls.length) {
            for (int i = 0; i < epTitles.length; i++) {
                episodeTitles.add(epTitles[i]);
                episodeUrls.add(epUrls[i]);
            }
        }

        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(30000, 120000, 1000, 5000)
                .build();

        exoPlayer = new SimpleExoPlayer.Builder(this).setLoadControl(loadControl).build();
        playerView.setPlayer(exoPlayer);
        playerView.setControllerHideOnTouch(true);

        if (isEpisodesMode && !episodeUrls.isEmpty()) {
            currentEpisodeIndex = 0;
            playEpisode(0);
            updateTitleView();
            setupEpisodeNavigation();
        } else if (url != null && !url.isEmpty()) {
            MediaItem mediaItem = new MediaItem.Builder().setUri(url).build();
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            if (title != null) {
                titleView.setText(title);
            }
        }

        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(this);
    }

    private void playEpisode(int index) {
        if (index < 0 || index >= episodeUrls.size()) return;
        currentEpisodeIndex = index;
        MediaItem mediaItem = new MediaItem.Builder().setUri(episodeUrls.get(index)).build();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);
        updateTitleView();
    }

    private void updateTitleView() {
        if (isEpisodesMode && !episodeTitles.isEmpty() && currentEpisodeIndex < episodeTitles.size()) {
            titleView.setText(episodeTitles.get(currentEpisodeIndex));
        }
    }

    private void setupEpisodeNavigation() {
        FrameLayout container = findViewById(webapp.newcloud.lottery.movie.R.id.episodeContainer);
        if (container == null) return;
        container.removeAllViews();

        ImageView prevBtn = new ImageView(this);
        prevBtn.setImageResource(android.R.drawable.ic_media_previous);
        prevBtn.setOnClickListener(v -> {
            if (currentEpisodeIndex > 0) playEpisode(currentEpisodeIndex - 1);
        });
        container.addView(prevBtn);

        ImageView nextBtn = new ImageView(this);
        nextBtn.setImageResource(android.R.drawable.ic_media_next);
        nextBtn.setOnClickListener(v -> {
            if (currentEpisodeIndex < episodeUrls.size() - 1) playEpisode(currentEpisodeIndex + 1);
        });
        container.addView(nextBtn);
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY: Log.d(TAG, "Player ready"); break;
            case Player.STATE_BUFFERING: Log.d(TAG, "Player buffering"); break;
            case Player.STATE_ENDED:
                Log.d(TAG, "Player ended");
                runOnUiThread(() -> {
                    if (isEpisodesMode && currentEpisodeIndex < episodeUrls.size() - 1) {
                        playEpisode(currentEpisodeIndex + 1);
                    } else {
                        finish();
                    }
                });
                break;
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "Player error: " + error.getMessage(), error);
        if (isEpisodesMode && currentEpisodeIndex < episodeUrls.size() - 1) {
            playEpisode(currentEpisodeIndex + 1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) exoPlayer.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.removeListener(this);
            exoPlayer.release();
        }
    }

    @Override
    public void onBackPressed() {
        if (isEpisodesMode && currentEpisodeIndex > 0) {
            playEpisode(currentEpisodeIndex - 1);
        } else {
            super.onBackPressed();
        }
    }
}
