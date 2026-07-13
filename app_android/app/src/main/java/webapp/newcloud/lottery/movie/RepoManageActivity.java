package webapp.newcloud.lottery.movie;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import webapp.newcloud.lottery.movie.util.DbHelper;
import webapp.newcloud.lottery.movie.util.FanTaiYingParser;
import webapp.newcloud.lottery.movie.util.SiteInfo;
import webapp.newcloud.lottery.movie.model.SiteConfig;
import webapp.newcloud.lottery.movie.model.Warehouse;
import webapp.newcloud.lottery.movie.util.HttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class RepoManageActivity extends AppCompatActivity {

    public interface OnSiteClickListener {
        void onSiteClick(SiteConfig site);
    }

    private RecyclerView rvWarehouses;
    private RecyclerView rvSites;
    private TextView tvEmptyHint;

    private WarehouseAdapter warehouseAdapter;
    private SiteAdapter siteAdapter;
    private List<Warehouse> warehouseList = new ArrayList<>();
    private List<SiteConfig> siteList = new ArrayList<>();
    private String currentWarehouseId;
    private final HttpClient httpClient = new HttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_manage);
        
        setTitle("");
        findViewById(R.id.ivClose).setOnClickListener(v -> finish());

        LinearLayout btnAddRepo = findViewById(R.id.btnAddRepo);
        if (btnAddRepo != null) {
            btnAddRepo.setOnClickListener(v -> showAddRepoDialog());
        }

        rvWarehouses = findViewById(R.id.rvWarehouses);
        rvSites = findViewById(R.id.rvSites);
        tvEmptyHint = findViewById(R.id.tvEmptyHint);

        if (rvWarehouses != null) {
            rvWarehouses.setLayoutManager(new LinearLayoutManager(this));
        }
        if (rvSites != null) {
            rvSites.setLayoutManager(new LinearLayoutManager(this));
        }

        try {
            warehouseAdapter = new WarehouseAdapter();
            siteAdapter = new SiteAdapter();
            if (rvWarehouses != null) {
                rvWarehouses.setAdapter(warehouseAdapter);
            }
            if (rvSites != null) {
                rvSites.setAdapter(siteAdapter);
            }
            if (siteAdapter != null) {
                siteAdapter.setListener(site -> {
                    try {
                        App.setCurrentSiteName(site.name);
                    } catch (Exception e) {
                        // App instance may not be ready yet
                    }
                    Toast.makeText(RepoManageActivity.this, "已选中站点: " + site.name, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
            return;
        }

        try {
            loadWarehouses();
        } catch (Exception e) {
            Toast.makeText(this, "加载仓库失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void loadWarehouses() {
        try {
            new Thread(() -> {
                try {
                    List<Warehouse> warehouses = DbHelper.getInstance(this).warehouseDao().getAll();
                    runOnUiThread(() -> {
                        warehouseList.clear();
                        warehouseList.addAll(warehouses);
                        if (warehouseAdapter != null) {
                            warehouseAdapter.notifyDataSetChanged();
                        }
                        if (!warehouses.isEmpty()) {
                            currentWarehouseId = warehouses.get(0).id;
                            loadSites(currentWarehouseId);
                        } else {
                            currentWarehouseId = null;
                            siteList.clear();
                            if (siteAdapter != null) {
                                siteAdapter.notifyDataSetChanged();
                            }
                            if (tvEmptyHint != null) {
                                tvEmptyHint.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(RepoManageActivity.this, "加载仓库失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "DbHelper 初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadSites(String warehouseId) {
        try {
            new Thread(() -> {
                try {
                    List<SiteConfig> allSites = DbHelper.getInstance(this).siteConfigDao().getAll();
                    List<SiteConfig> filtered = new ArrayList<>();
                    for (SiteConfig s : allSites) {
                        if (warehouseId != null && warehouseId.equals(s.warehouseId)) {
                            filtered.add(s);
                        }
                    }
                    runOnUiThread(() -> {
                        siteList.clear();
                        siteList.addAll(filtered);
                        if (siteAdapter != null) {
                            siteAdapter.notifyDataSetChanged();
                        }
                        if (tvEmptyHint != null) {
                            tvEmptyHint.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(RepoManageActivity.this, "加载站点失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAddRepoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加仓库");
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_repo, null);
        EditText etName = dialogView.findViewById(R.id.etDialogName);
        EditText etUrl = dialogView.findViewById(R.id.etDialogUrl);
        TextView tvStatus = dialogView.findViewById(R.id.tvDialogStatus);
        TextView btnConfirm = dialogView.findViewById(R.id.btnDialogConfirm);
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String url = etUrl.getText().toString().trim();
            
            if (TextUtils.isEmpty(name)) {
                tvStatus.setText("请输入仓库名称");
                tvStatus.setTextColor(0xffff6b6b);
                return;
            }
            if (TextUtils.isEmpty(url)) {
                tvStatus.setText("请输入仓库地址");
                tvStatus.setTextColor(0xffff6b6b);
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                tvStatus.setText("地址必须以 http:// 或 https:// 开头");
                tvStatus.setTextColor(0xffff6b6b);
                return;
            }

            tvStatus.setText("正在获取仓库配置...");
            tvStatus.setTextColor(0xff8899aa);
            btnConfirm.setEnabled(false);

            new Thread(() -> {
                final boolean[] successHolder = {false};
                try {
                    String warehouseId = UUID.randomUUID().toString();
                    Warehouse warehouse = new Warehouse();
                    warehouse.id = warehouseId;
                    warehouse.name = name;
                    warehouse.url = url;
                    warehouse.createdAt = System.currentTimeMillis();
                    DbHelper.getInstance(RepoManageActivity.this).warehouseDao().insert(warehouse);
                    
                    // Detect FanTaiYing special format (image/x-ms-bmp content-type or /tv endpoint)
                    if (url.contains("饭太硬") || url.contains("fanying") || url.endsWith("/tv")) {
                        try {
                            // Fetch raw bytes for FanTaiYing format
                            byte[] rawData = httpClient.httpGetRaw(url);
                            if (rawData != null && rawData.length > 0) {
                                runOnUiThread(() -> tvStatus.setText("正在解析饭太硬配置..."));
                                JSONObject config = FanTaiYingParser.parse(rawData);
                                successHolder[0] = parseAndSaveSitesFromConfig(config, warehouseId);
                            } else {
                                successHolder[0] = false;
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> tvStatus.setText("饭太硬解析失败，尝试普通JSON解析: " + e.getMessage()));
                            // Fallback to normal JSON parsing
                            String response = httpClient.httpGet(url);
                            successHolder[0] = parseAndSaveSites(response, warehouseId);
                        }
                    } else {
                        String response = httpClient.httpGet(url);
                        runOnUiThread(() -> tvStatus.setText("正在解析配置..."));
                        successHolder[0] = parseAndSaveSites(response, warehouseId);
                    }
                    
                    final boolean finalSuccess = successHolder[0];
                    runOnUiThread(() -> {
                        if (finalSuccess) {
                            tvStatus.setText("成功！已加载站点");
                            tvStatus.setTextColor(0xff4ade80);
                            dialog.dismiss();
                            loadWarehouses();
                        } else {
                            tvStatus.setText("配置格式错误，无法解析JSON");
                            tvStatus.setTextColor(0xffff6b6b);
                        }
                        btnConfirm.setEnabled(true);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        tvStatus.setText("获取失败: " + e.getMessage());
                        tvStatus.setTextColor(0xffff6b6b);
                        btnConfirm.setEnabled(true);
                    });
                }
            }).start();
        });
        
        dialog.show();
    }

    private boolean parseAndSaveSitesFromConfig(JSONObject config, String warehouseId) {
        try {
            List<SiteInfo> siteInfos = FanTaiYingParser.parseSites(config);
            for (SiteInfo si : siteInfos) {
                SiteConfig site = new SiteConfig();
                site.id = UUID.randomUUID().toString();
                site.warehouseId = warehouseId;
                site.key = si.key;
                site.name = si.name;
                site.type = si.type;
                site.api = si.api;
                site.searchable = si.searchable;
                site.quickSearch = si.quickSearch;
                site.filterable = si.filterable;
                site.ext = si.ext != null ? si.ext : "{}";
                site.sourceType = "warehouse";
                site.createdAt = System.currentTimeMillis();
                DbHelper.getInstance(this).siteConfigDao().insert(site);
            }
            return !siteInfos.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean parseAndSaveSites(String text, String warehouseId) {
        try {
            text = text.trim();
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            
            JSONObject json = new JSONObject(text);
            if (!json.has("sites")) return false;
            
            JSONArray sitesArray = json.getJSONArray("sites");
            if (sitesArray.length() == 0) return false;
            
            List<SiteConfig> sites = new ArrayList<>();
            
            for (int i = 0; i < sitesArray.length(); i++) {
                JSONObject siteObj = sitesArray.getJSONObject(i);
                SiteConfig site = new SiteConfig();
                site.id = UUID.randomUUID().toString();
                site.warehouseId = warehouseId;
                site.key = siteObj.optString("key", siteObj.optString("name", ""));
                site.name = siteObj.optString("name", "未命名");
                site.type = siteObj.optInt("type", 1);
                site.api = siteObj.optString("api", "");
                site.searchable = siteObj.optInt("searchable", 0);
                site.quickSearch = siteObj.optInt("quickSearch", 0);
                site.filterable = siteObj.optInt("filterable", 0);
                site.type_flag = siteObj.optString("type_flag", "");
                site.playerType = siteObj.optInt("playerType", 0);
                site.ext = siteObj.has("ext") ? (siteObj.get("ext") instanceof String 
                    ? (String) siteObj.get("ext") : siteObj.optString("ext", "{}")) : "{}";
                site.pass = siteObj.optBoolean("pass", false);
                site.sourceType = "warehouse";
                site.createdAt = System.currentTimeMillis();
                sites.add(site);
            }
            
            for (SiteConfig site : sites) {
                DbHelper.getInstance(this).siteConfigDao().insert(site);
            }
            
            return !sites.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteWarehouse(String id) {
        try {
            new Thread(() -> {
                try {
                    DbHelper.getInstance(RepoManageActivity.this).siteConfigDao().deleteByWarehouseId(id);
                    DbHelper.getInstance(RepoManageActivity.this).warehouseDao().deleteById(id);
                    runOnUiThread(this::loadWarehouses);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(RepoManageActivity.this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class WarehouseAdapter extends RecyclerView.Adapter<WarehouseAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageView ivDelete;
            LinearLayout root;
            ViewHolder(View v) {
                super(v);
                root = v.findViewById(R.id.llWarehouseItem);
                tvName = v.findViewById(R.id.tvWarehouseName);
                ivDelete = v.findViewById(R.id.ivDeleteWarehouse);
                
                root.setOnClickListener(v2 -> {
                    int pos = getAdapterPosition();
                    if (pos >= 0 && pos < warehouseList.size()) {
                        currentWarehouseId = warehouseList.get(pos).id;
                        loadSites(currentWarehouseId);
                        RepoManageActivity.this.runOnUiThread(() -> warehouseAdapter.notifyDataSetChanged());
                    }
                });
                
                ivDelete.setOnClickListener(v3 -> {
                    int pos = getAdapterPosition();
                    if (pos >= 0 && pos < warehouseList.size()) {
                        new AlertDialog.Builder(RepoManageActivity.this)
                            .setMessage("确定删除该仓库及其中所有站点？")
                            .setPositiveButton("确定", (d, w) -> deleteWarehouse(warehouseList.get(pos).id))
                            .setNegativeButton("取消", null)
                            .show();
                    }
                });
            }
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_warehouse, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Warehouse w = warehouseList.get(position);
            holder.tvName.setText(w.name);
            holder.root.setSelected(currentWarehouseId != null && currentWarehouseId.equals(w.id));
        }
        @Override
        public int getItemCount() { return warehouseList.size(); }
    }

    private class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {
        private RepoManageActivity.OnSiteClickListener listener;

        void setListener(RepoManageActivity.OnSiteClickListener l) { listener = l; }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSiteName;
            TextView tvSiteInfo;
            TextView tvSiteApi;
            ViewHolder(View v) {
                super(v);
                tvSiteName = v.findViewById(R.id.tvSiteName);
                tvSiteInfo = v.findViewById(R.id.tvSiteInfo);
                tvSiteApi = v.findViewById(R.id.tvSiteApi);
            }
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repo_site, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SiteConfig site = siteList.get(position);
            holder.tvSiteName.setText(site.name);
            String typeLabel = site.type == 0 ? "XML" : site.type == 1 ? "JSON" : site.type == 3 ? "JS插件" : "未知";
            String searchTag = (site.searchable > 0 || site.quickSearch > 0) ? " · 可搜索" : "";
            holder.tvSiteInfo.setText(typeLabel + searchTag);
            holder.tvSiteApi.setText(TextUtils.isEmpty(site.api) ? "(无API地址)" : site.api);
            holder.itemView.setOnClickListener(v -> {
                App.setCurrentSiteName(site.name);
                Toast.makeText(RepoManageActivity.this, "已选中站点: " + site.name, Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onSiteClick(site);
            });
        }
        @Override
        public int getItemCount() { return siteList.size(); }
    }
}
