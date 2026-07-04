package com.personalassistant.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.personalassistant.app.ui.MovieHomeFragment;

public class ShareReceiverActivity extends AppCompatActivity {
    private static final String PREFS_KEY_LAST_SITE_ID = "last_site_id";
    private static final String PREFS_KEY_LAST_CATEGORY = "last_category";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.isEmpty()) {
                handleSharedUrl(sharedText);
            }
        }
        finish();
    }

    private void handleSharedUrl(String url) {
        // Check if it looks like a video URL
        if (url.contains(".m3u8") || url.contains(".mp4") || url.contains("youtu.be")) {
            // Show share panel
            showSharePanel(url);
        } else {
            Toast.makeText(this, "已接收到分享链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSharePanel(String url) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF05070D);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("分享链接");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        root.addView(title);

        TextView urlText = new TextView(this);
        urlText.setText(url);
        urlText.setTextColor(0xFF8899AA);
        urlText.setTextSize(12);
        urlText.setMaxLines(3);
        urlText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(urlText);

        // Open in movie home
        Button openBtn = new Button(this);
        openBtn.setText("在影视中打开");
        openBtn.setTextColor(0xFFFFFFFF);
        openBtn.setBackgroundColor(0xFF7C3AED);
        openBtn.setPadding(dp(20), dp(10), dp(20), dp(10));
        openBtn.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MovieHomeFragment())
                    .commit();
            // Save URL to shared prefs for player to pick up
            getSharedPreferences("share_prefs", 0).edit()
                    .putString("pending_share_url", url)
                    .apply();
            finish();
        });
        root.addView(openBtn);

        // Copy URL
        Button copyBtn = new Button(this);
        copyBtn.setText("复制链接");
        copyBtn.setTextColor(0xFFFFFFFF);
        copyBtn.setBackgroundColor(0xFF444444);
        copyBtn.setPadding(dp(20), dp(10), dp(20), dp(10));
        copyBtn.setOnClickListener(v -> {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("shared_url", url));
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        root.addView(copyBtn);

        setContentView(root);
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
