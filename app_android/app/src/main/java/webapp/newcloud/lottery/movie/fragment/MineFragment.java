package webapp.newcloud.lottery.movie.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import webapp.newcloud.lottery.movie.MainActivity;
import webapp.newcloud.lottery.movie.RepoManageActivity;
import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.util.DbHelper;
import webapp.newcloud.lottery.movie.model.Favorite;
import webapp.newcloud.lottery.movie.model.History;
import webapp.newcloud.lottery.movie.model.SiteConfig;
import webapp.newcloud.lottery.movie.model.LiveChannel;
import webapp.newcloud.lottery.movie.model.Warehouse;

public class MineFragment extends Fragment {

    private TextView tvFavCount;
    private TextView tvHistCount;
    private TextView tvLiveCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.util.Log.d("MineFragment", "=== onCreateView ===");
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        android.util.Log.d("MineFragment", "=== inflated: " + (view != null) + " ===");
        
        tvFavCount = view.findViewById(R.id.tvFavCount);
        tvHistCount = view.findViewById(R.id.tvHistCount);
        tvLiveCount = view.findViewById(R.id.tvLiveCount);

        setupMenuItems(view);
        loadCounts();
        
        android.util.Log.d("MineFragment", "=== onCreateView returning ===");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("MineFragment", "=== onResume, getView()=" + (getView() != null ? "not-null" : "null") + " ===");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        android.util.Log.d("MineFragment", "=== onHiddenChanged: hidden=" + hidden + " ===");
    }

    private void setupMenuItems(View view) {
        view.findViewById(R.id.menu_warehouse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (getContext() == null) {
                        Toast.makeText(MineFragment.this.getContext(), "页面未就绪", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.showSiteSelectPanel();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "打开站点管理失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
        view.findViewById(R.id.menu_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.switchToSearch();
                }
            }
        });
        view.findViewById(R.id.menu_live_source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.switchToLive();
                }
            }
        });
        view.findViewById(R.id.menu_lottery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.switchToLottery();
                }
            }
        });
        view.findViewById(R.id.menu_favorites).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.switchToLibrary();
                }
            }
        });
        view.findViewById(R.id.menu_import_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImportExportDialog();
            }
        });
        view.findViewById(R.id.menu_clear_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearData();
            }
        });
        view.findViewById(R.id.menu_clear_cache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearCache();
            }
        });
        view.findViewById(R.id.menu_export_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportConfig();
            }
        });
        view.findViewById(R.id.menu_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "个人助手 TV v9.9", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCounts() {
        if (getContext() == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final int favCount = DbHelper.getInstance(getContext()).favoriteDao().getAll().size();
                    final int histCount = DbHelper.getInstance(getContext()).historyDao().getAll().size();
                    final int liveCount = DbHelper.getInstance(getContext()).liveChannelDao().getAll().size();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvFavCount.setText(String.valueOf(favCount));
                            tvHistCount.setText(String.valueOf(histCount));
                            tvLiveCount.setText(String.valueOf(liveCount));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void clearData() {
        if (getContext() == null) return;
        DbHelper.getInstance(getContext()).favoriteDao().clearAll();
        DbHelper.getInstance(getContext()).historyDao().clearAll();
        loadCounts();
        Toast.makeText(getContext(), "已清空收藏和历史", Toast.LENGTH_SHORT).show();
    }

    private void clearCache() {
        if (getContext() == null) return;
        DbHelper.getInstance(getContext()).movieDao().clearAll();
        Toast.makeText(getContext(), "已清理缓存", Toast.LENGTH_SHORT).show();
    }

    private void exportConfig() {
        if (getContext() == null) return;
        new Thread(() -> {
            try {
                List<SiteConfig> sites = DbHelper.getInstance(getContext()).siteConfigDao().getAll();
                List<Favorite> favorites = DbHelper.getInstance(getContext()).favoriteDao().getAll();
                List<History> history = DbHelper.getInstance(getContext()).historyDao().getAll();
                List<Warehouse> warehouses = DbHelper.getInstance(getContext()).warehouseDao().getAll();
                List<LiveChannel> channels = DbHelper.getInstance(getContext()).liveChannelDao().getAll();
                
                org.json.JSONObject config = new org.json.JSONObject();
                
                org.json.JSONArray sitesArray = new org.json.JSONArray();
                for (SiteConfig s : sites) {
                    org.json.JSONObject siteObj = new org.json.JSONObject();
                    siteObj.put("key", s.key);
                    siteObj.put("name", s.name);
                    siteObj.put("type", s.type);
                    siteObj.put("api", s.api);
                    siteObj.put("searchable", s.searchable);
                    siteObj.put("quickSearch", s.quickSearch);
                    siteObj.put("filterable", s.filterable);
                    siteObj.put("ext", s.ext != null ? s.ext : "{}");
                    sitesArray.put(siteObj);
                }
                config.put("sites", sitesArray);
                
                org.json.JSONArray favsArray = new org.json.JSONArray();
                for (Favorite f : favorites) {
                    org.json.JSONObject favObj = new org.json.JSONObject();
                    favObj.put("movieId", f.movieId);
                    favObj.put("title", f.title);
                    favsArray.put(favObj);
                }
                config.put("favs", favsArray);
                
                org.json.JSONArray histArray = new org.json.JSONArray();
                for (History h : history) {
                    org.json.JSONObject histObj = new org.json.JSONObject();
                    histObj.put("movieId", h.movieId);
                    histObj.put("title", h.title);
                    histArray.put(histObj);
                }
                config.put("history", histArray);
                
                String jsonStr = config.toString(2);
                
                requireActivity().runOnUiThread(() -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                    builder.setTitle("导出配置 (" + sites.size() + " 个站点)");
                    
                    EditText editText = new EditText(getContext());
                    editText.setText(jsonStr);
                    editText.setKeyListener(null);
                    editText.setMinLines(6);
                    builder.setView(editText);
                    
                    builder.setPositiveButton("复制到剪贴板", (d, w) -> {
                        android.content.ClipboardManager clipboard = 
                            (android.content.ClipboardManager) requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("config", jsonStr);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    });
                    
                    builder.setNegativeButton("保存为文件", (d, w) -> {
                        saveConfigToFile(jsonStr);
                    });
                    
                    builder.setNeutralButton("取消", null);
                    builder.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void saveConfigToFile(String jsonStr) {
        if (getContext() == null) return;
        
        String fileName = "tvbox-config-" + System.currentTimeMillis() + ".json";
        java.io.File dir = requireActivity().getExternalFilesDir(null);
        if (dir == null) dir = requireActivity().getCacheDir();
        
        try {
            java.io.File file = new java.io.File(dir, fileName);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(jsonStr);
            writer.close();
            
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "已保存到: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showImportExportDialog() {
        if (getContext() == null) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("配置导入/导出");
        
        String[] items = {"导出配置到剪贴板", "从剪贴板导入配置", "保存配置为文件", "从文件导入配置"};
        builder.setItems(items, (d, w) -> {
            switch (w) {
                case 0: exportConfig(); break;
                case 1: importFromClipboard(); break;
                case 2: saveConfigToFile(""); break;
                case 3: importFromFile(); break;
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void importFromClipboard() {
        if (getContext() == null) return;
        
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(getContext(), "剪贴板为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        CharSequence clipText = clipboard.getPrimaryClip().getItemAt(0).getText();
        if (clipText == null || clipText.length() == 0) {
            Toast.makeText(getContext(), "剪贴板内容为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        importConfigFromJson(clipText.toString());
    }

    private void importFromFile() {
        if (getContext() == null) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("选择配置JSON文件");
        builder.setMessage("请输入JSON配置内容");
        
        final EditText editText = new EditText(getContext());
        editText.setHint("粘贴JSON内容或选择文件");
        editText.setMinLines(8);
        builder.setView(editText);
        
        builder.setPositiveButton("导入", (d, w) -> {
            String jsonStr = editText.getText().toString().trim();
            if (!jsonStr.isEmpty()) {
                importConfigFromJson(jsonStr);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void importConfigFromJson(String jsonStr) {
        if (getContext() == null) return;
        
        new Thread(() -> {
            try {
                org.json.JSONObject json = new org.json.JSONObject(jsonStr);
                org.json.JSONArray sitesArray = json.optJSONArray("sites");
                
                if (sitesArray == null || sitesArray.length() == 0) {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "没有找到站点数据", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                int imported = 0;
                for (int i = 0; i < sitesArray.length(); i++) {
                    org.json.JSONObject siteObj = sitesArray.getJSONObject(i);
                    SiteConfig site = new SiteConfig();
                    site.id = java.util.UUID.randomUUID().toString();
                    site.key = siteObj.optString("key", siteObj.optString("name", ""));
                    site.name = siteObj.optString("name", "未命名");
                    site.type = siteObj.optInt("type", 1);
                    site.api = siteObj.optString("api", "");
                    site.searchable = siteObj.optInt("searchable", 0);
                    site.quickSearch = siteObj.optInt("quickSearch", 0);
                    site.filterable = siteObj.optInt("filterable", 0);
                    site.ext = siteObj.optString("ext", "{}");
                    site.sourceType = "imported";
                    site.createdAt = System.currentTimeMillis();
                    
                    DbHelper.getInstance(getContext()).siteConfigDao().insert(site);
                    imported++;
                }
                
                final int finalImported = imported;
                
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "导入成功，共 " + finalImported + " 个站点", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "JSON格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
