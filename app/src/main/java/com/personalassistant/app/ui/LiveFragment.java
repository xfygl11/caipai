package com.personalassistant.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.personalassistant.app.R;
import com.personalassistant.app.data.model.LiveChannel;
import com.personalassistant.app.data.model.LiveSource;
import com.personalassistant.app.data.repository.LiveRepository;
import com.personalassistant.app.data.repository.SiteRepository;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.LiveSourceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveFragment extends Fragment {
    private RecyclerView channelList;
    private LiveChannelAdapter adapter;
    private TextView loadingText;
    private LinearLayout sourceBar;
    private String currentSourceUrl = "";
    private String currentSourceName = "";
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<LiveChannel> allChannels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0xFF05070D);

        // Source bar
        sourceBar = new LinearLayout(requireContext());
        sourceBar.setOrientation(LinearLayout.HORIZONTAL);
        sourceBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        sourceBar.setWeightSum(1);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sourceBar.setLayoutParams(sourceParams);
        root.addView(sourceBar);

        // Add source button
        Button addBtn = new Button(requireContext());
        addBtn.setText("+ 添加");
        addBtn.setTextSize(11);
        addBtn.setAllCaps(false);
        addBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        addBtn.setTextColor(0xFFFFFFFF);
        addBtn.setBackgroundColor(0xFF7C3AED);
        addBtn.setOnClickListener(v -> showAddSourceDialog());
        sourceBar.addView(addBtn);

        // Loading
        loadingText = new TextView(requireContext());
        loadingText.setText("加载中...");
        loadingText.setTextColor(0xFF667788);
        loadingText.setGravity(android.view.Gravity.CENTER);
        loadingText.setPadding(0, dp(40), 0, 0);
        root.addView(loadingText);

        // Channel list
        channelList = new RecyclerView(requireContext());
        channelList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LiveChannelAdapter();
        adapter.setOnChannelClickListener(channel -> {
            Intent intent = new Intent(requireContext(), LivePlayerActivity.class);
            intent.putExtra("channel_name", channel.name);
            intent.putExtra("channel_url", channel.url);
            intent.putExtra("channel_logo", channel.logo);
            intent.putExtra("source_name", currentSourceName);
            startActivity(intent);
        });
        channelList.setAdapter(adapter);
        channelList.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(channelList);

        loadSources();
        return root;
    }

    private void loadSources() {
        executor.execute(() -> {
            try {
                final List<LiveSource> sources = LiveRepository.loadLiveSources(requireContext());
                handler.post(() -> {
                    buildSourceBar(sources);
                    if (sources != null && !sources.isEmpty()) {
                        loadChannels(sources.get(0));
                    }
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void buildSourceBar(List<LiveSource> sources) {
        sourceBar.removeAllViews();

        Button addBtn = new Button(requireContext());
        addBtn.setText("+");
        addBtn.setTextSize(14);
        addBtn.setAllCaps(false);
        addBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        addBtn.setTextColor(0xFFFFFFFF);
        addBtn.setBackgroundColor(0xFF7C3AED);
        addBtn.setOnClickListener(v -> showAddSourceDialog());
        sourceBar.addView(addBtn);

        for (LiveSource source : sources) {
            Button btn = new Button(requireContext());
            btn.setText(source.name.length() > 8 ? source.name.substring(0, 8) + ".." : source.name);
            btn.setTextSize(11);
            btn.setAllCaps(false);
            btn.setPadding(dp(10), dp(6), dp(10), dp(6));
            boolean active = source.url.equals(currentSourceUrl);
            btn.setTextColor(active ? 0xFFFFFFFF : 0xFF7E879F);
            btn.setBackgroundColor(active ? 0xFF7C3AED : 0x1A7C3AED);
            final LiveSource finalSource = source;
            btn.setOnClickListener(v -> {
                currentSourceUrl = source.url;
                currentSourceName = source.name;
                buildSourceBar(sources);
                loadChannels(finalSource);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(6);
            btn.setLayoutParams(params);
            sourceBar.addView(btn);
        }
    }

    private void loadChannels(LiveSource source) {
        loadingText.setText("加载频道中...");
        loadingText.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                String liveText = com.personalassistant.app.data.network.OkHttpUtil.get(source.url);
                if (liveText == null || liveText.isEmpty()) {
                    handler.post(() -> loadingText.setText("加载失败"));
                    return;
                }

                List<LiveChannel> channels = SiteRepository.parseLiveSource(source.url, liveText);

                handler.post(() -> {
                    allChannels = channels;
                    adapter.setChannels(channels);
                    loadingText.setVisibility(View.GONE);
                    if (channels.isEmpty()) {
                        loadingText.setText("暂无频道");
                        loadingText.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    loadingText.setText("加载失败: " + e.getMessage());
                    loadingText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void showAddSourceDialog() {
        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("直播源名称");
        nameInput.setHintTextColor(0xFF6F7890);
        nameInput.setTextColor(0xFFE8EDFF);

        EditText urlInput = new EditText(requireContext());
        urlInput.setHint("直播源地址");
        urlInput.setHintTextColor(0xFF6F7890);
        urlInput.setTextColor(0xFFE8EDFF);
        urlInput.setText("https://live.fanmingming.com/tv/m3u/ipv6.m3u");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));
        layout.addView(nameInput);
        layout.addView(urlInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("添加直播源")
                .setView(layout)
                .setPositiveButton("确定", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();
                    if (name.isEmpty() || url.isEmpty()) {
                        Toast.makeText(requireContext(), "名称和地址不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addSource(name, url);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addSource(String name, String url) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                LiveSourceEntity entity = new LiveSourceEntity();
                entity.name = name;
                entity.url = url;
                entity.isBuiltin = 0;
                entity.addedTime = System.currentTimeMillis();
                db.liveSourceDao().insert(entity);

                handler.post(() -> {
                    Toast.makeText(requireContext(), "已添加: " + name, Toast.LENGTH_SHORT).show();
                    loadSources();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(),
                        "添加失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    static int dp(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}
