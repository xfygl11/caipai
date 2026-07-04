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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.personalassistant.app.R;
import com.personalassistant.app.data.model.ParseConfig;
import com.personalassistant.app.data.model.RepoConfig;
import com.personalassistant.app.data.model.SiteInfo;
import com.personalassistant.app.data.repository.SiteRepository;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.ParseConfigEntity;
import com.personalassistant.app.db.entity.SourceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {
    private LinearLayout settingsList;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF05070D);

        ScrollView scroll = new ScrollView(requireContext());
        settingsList = new LinearLayout(requireContext());
        settingsList.setOrientation(LinearLayout.VERTICAL);
        settingsList.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(settingsList);
        root.addView(scroll);

        // Header
        TextView header = new TextView(requireContext());
        header.setText("设置");
        header.setTextColor(0xFFFFFFFF);
        header.setTextSize(22);
        header.setPadding(0, dp(8), 0, dp(24));
        settingsList.addView(header);

        addSection("仓库管理");
        addSettingsItem("添加仓库", v -> showAddRepoDialog());
        addSettingsItem("管理仓库", v -> showRepoManageDialog());
        addSettingsItem("查看仓库信息", v -> showRepoInfo());

        addDivider();
        addSection("解析设置");
        addSettingsItem("管理解析器", v -> showParseManageDialog());
        addSettingsItem("添加解析器", v -> showAddParseDialog());

        addDivider();
        addSection("播放器设置");
        addSettingsItem("默认播放器", v -> {});
        addSettingsItem("清除缓存", v -> showClearCacheDialog());

        addDivider();
        addSection("关于");
        addSettingsItem("版本号", v -> Toast.makeText(requireContext(), "v13.0", Toast.LENGTH_SHORT).show());

        return root;
    }

    private void addSection(String title) {
        TextView sectionTitle = new TextView(requireContext());
        sectionTitle.setText(title);
        sectionTitle.setTextColor(0xFF7C3AED);
        sectionTitle.setTextSize(13);
        sectionTitle.setPadding(0, dp(12), 0, dp(4));
        settingsList.addView(sectionTitle);
    }

    private void addDivider() {
        View divider = new View(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(0, dp(12), 0, dp(12));
        divider.setLayoutParams(params);
        divider.setBackgroundColor(0x1AFFFFFF);
        settingsList.addView(divider);
    }

    private void addSettingsItem(String text, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setBackgroundColor(0xFF101D2B);
        row.setOnClickListener(onClick);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFFE8EDFF);
        tv.setTextSize(15);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tv.setLayoutParams(tvParams);

        row.addView(tv);
        settingsList.addView(row);
    }

    private void showAddRepoDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("仓库地址...");
        input.setHintTextColor(0xFF6F7890);
        input.setTextColor(0xFFE8EDFF);
        input.setText("https://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json");

        new AlertDialog.Builder(requireContext())
                .setTitle("添加仓库")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) loadAndSaveRepo(url);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadAndSaveRepo(String repoUrl) {
        executor.execute(() -> {
            try {
                RepoConfig config = SiteRepository.loadRepoConfig(repoUrl);
                if (config == null) {
                    handler.post(() -> Toast.makeText(requireContext(), "仓库加载失败", Toast.LENGTH_SHORT).show());
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(requireContext());

                // Save movie sites
                for (SiteInfo site : config.sites) {
                    if (!"MV".equals(site.typeLabel)) continue;
                    if (site.api.isEmpty()) continue;

                    String base = site.api.replace("/api.php/provide/vod/at/xml/", "")
                            .replace("/api.php/provide/vod/", "");

                    SourceEntity entity = new SourceEntity();
                    entity.name = site.name;
                    entity.url = site.api;
                    entity.base = base;
                    entity.isBuiltin = 0;
                    entity.addedTime = System.currentTimeMillis();
                    entity.lastSyncTime = System.currentTimeMillis();
                    entity.status = 1;

                    SourceEntity existing = db.sourceDao().getByBaseBlocking(base);
                    if (existing != null) {
                        entity.id = existing.id;
                        db.sourceDao().updateLastSync(entity.id, System.currentTimeMillis());
                    } else {
                        db.sourceDao().insert(entity);
                    }
                }

                // Save parses (type=0 only)
                for (ParseConfig parse : config.parses) {
                    if (parse.type != 0) continue;
                    if (parse.url.isEmpty() || "Demo".equalsIgnoreCase(parse.url)) continue;

                    ParseConfigEntity entity = new ParseConfigEntity();
                    entity.name = parse.name;
                    entity.url = parse.url;
                    entity.type = parse.type;
                    entity.sortOrder = 0;
                    if (parse.flags != null && !parse.flags.isEmpty()) {
                        entity.flags = String.join(",", parse.flags);
                    }
                    db.parseConfigDao().insert(entity);
                }

                handler.post(() -> Toast.makeText(requireContext(), "仓库已保存", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(),
                        "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showRepoManageDialog() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();

                handler.post(() -> {
                    if (sources == null || sources.isEmpty()) {
                        Toast.makeText(requireContext(), "暂无仓库", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] items = new String[sources.size()];
                    for (int i = 0; i < sources.size(); i++) {
                        items[i] = sources.get(i).name + "\n" + sources.get(i).base;
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("仓库列表")
                            .setItems(items, (dialog, which) -> {
                                // TODO: show repo detail
                            })
                            .setPositiveButton("添加", (d, w) -> showAddRepoDialog())
                            .setNeutralButton("删除", (dialog, which) -> {
                                SourceEntity src = sources.get(which);
                                db.sourceDao().deleteById(src.id);
                                Toast.makeText(requireContext(), "已删除: " + src.name, Toast.LENGTH_SHORT).show();
                                showRepoManageDialog();
                            })
                            .show();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showRepoInfo() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<SourceEntity> sources = db.sourceDao().getAllSourcesBlocking();
                List<com.personalassistant.app.db.entity.ParseConfigEntity> parses =
                        db.parseConfigDao().getAllConfigsBlocking();

                StringBuilder info = new StringBuilder();
                info.append("仓库数量: ").append(sources != null ? sources.size() : 0).append("\n");
                info.append("解析器数量: ").append(parses != null ? parses.size() : 0);

                handler.post(() -> new AlertDialog.Builder(requireContext())
                        .setTitle("仓库信息")
                        .setMessage(info.toString())
                        .setPositiveButton("确定", null)
                        .show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showParseManageDialog() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<com.personalassistant.app.db.entity.ParseConfigEntity> configs =
                        db.parseConfigDao().getAllConfigsBlocking();

                handler.post(() -> {
                    if (configs == null || configs.isEmpty()) {
                        Toast.makeText(requireContext(), "暂无解析器", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] items = new String[configs.size()];
                    for (int i = 0; i < configs.size(); i++) {
                        items[i] = configs.get(i).name + " (" + configs.get(i).url.substring(0, Math.min(30, configs.get(i).url.length())) + "...)";
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("解析器列表")
                            .setItems(items, (dialog, which) -> {
                                // TODO: edit parse
                            })
                            .setPositiveButton("添加", (d, w) -> showAddParseDialog())
                            .setNeutralButton("删除", (dialog, which) -> {
                                db.parseConfigDao().deleteByName(configs.get(which).name);
                                showParseManageDialog();
                            })
                            .show();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showAddParseDialog() {
        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("解析器名称");
        nameInput.setHintTextColor(0xFF6F7890);
        nameInput.setTextColor(0xFFE8EDFF);

        EditText urlInput = new EditText(requireContext());
        urlInput.setHint("解析URL (含?url=)");
        urlInput.setHintTextColor(0xFF6F7890);
        urlInput.setTextColor(0xFFE8EDFF);
        urlInput.setText("https://jx.xmflv.com/?url=");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));
        layout.addView(nameInput);
        layout.addView(urlInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("添加解析器")
                .setView(layout)
                .setPositiveButton("确定", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();
                    if (name.isEmpty() || url.isEmpty()) {
                        Toast.makeText(requireContext(), "名称和URL不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    executor.execute(() -> {
                        try {
                            AppDatabase db = AppDatabase.getInstance(requireContext());
                            ParseConfigEntity entity = new ParseConfigEntity();
                            entity.name = name;
                            entity.url = url;
                            entity.type = 0;
                            entity.sortOrder = 0;
                            db.parseConfigDao().insert(entity);
                            handler.post(() -> Toast.makeText(requireContext(), "已添加", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            handler.post(() -> Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showClearCacheDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清除缓存")
                .setMessage("确定要清除所有缓存数据吗？")
                .setPositiveButton("确定", (d, w) -> {
                    executor.execute(() -> {
                        try {
                            AppDatabase db = AppDatabase.getInstance(requireContext());
                            db.movieDao().deleteBySourceId(0);
                            db.categoryDao().deleteBySourceId(0);
                            handler.post(() -> Toast.makeText(requireContext(), "缓存已清除", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            handler.post(() -> Toast.makeText(requireContext(), "清除失败", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
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
