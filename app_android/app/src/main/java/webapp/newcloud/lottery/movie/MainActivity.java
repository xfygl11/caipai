package webapp.newcloud.lottery.movie;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import webapp.newcloud.lottery.movie.fragment.HomeFragment;
import webapp.newcloud.lottery.movie.fragment.LibraryFragment;
import webapp.newcloud.lottery.movie.fragment.LiveFragment;
import webapp.newcloud.lottery.movie.fragment.LogWindowFragment;
import webapp.newcloud.lottery.movie.fragment.LotteryFragment;
import webapp.newcloud.lottery.movie.fragment.MineFragment;
import webapp.newcloud.lottery.movie.fragment.SearchFragment;
import webapp.newcloud.lottery.movie.model.LiveChannel;
import webapp.newcloud.lottery.movie.model.Movie;
import webapp.newcloud.lottery.movie.model.SiteConfig;
import webapp.newcloud.lottery.movie.model.Warehouse;
import webapp.newcloud.lottery.movie.util.DbHelper;
import webapp.newcloud.lottery.movie.util.HttpClient;
import webapp.newcloud.lottery.movie.util.LogManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_HOME_FRAGMENT = "home_fragment";
    private static final String KEY_LIVE_FRAGMENT = "live_fragment";
    private static final String KEY_LOTTERY_FRAGMENT = "lottery_fragment";
    private static final String KEY_LIBRARY_FRAGMENT = "library_fragment";
    private static final String KEY_MINE_FRAGMENT = "mine_fragment";
    private static final String KEY_SEARCH_FRAGMENT = "search_fragment";

    private HomeFragment homeFragment;
    private LiveFragment liveFragment;
    private LotteryFragment lotteryFragment;
    private LibraryFragment libraryFragment;
    private MineFragment mineFragment;
    private SearchFragment searchFragment;

    private Fragment currentFragment;
    private SiteConfig currentSite;

    private GestureDetector gestureDetector;
    private TextView logFloatingButton;
    private LogWindowFragment logWindow;


    public interface SitesLoadedCallback {
        void onSitesLoaded(List<SiteConfig> sites);
    }

    public interface CurrentSiteCallback {
        void onSiteLoaded(SiteConfig site);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            liveFragment = new LiveFragment();
            lotteryFragment = new LotteryFragment();
            libraryFragment = new LibraryFragment();
            mineFragment = new MineFragment();
            searchFragment = new SearchFragment();

            fm.beginTransaction()
                .add(R.id.fragmentContainer, homeFragment, KEY_HOME_FRAGMENT)
                .add(R.id.fragmentContainer, liveFragment, KEY_LIVE_FRAGMENT)
                .add(R.id.fragmentContainer, lotteryFragment, KEY_LOTTERY_FRAGMENT)
                .add(R.id.fragmentContainer, libraryFragment, KEY_LIBRARY_FRAGMENT)
                .add(R.id.fragmentContainer, mineFragment, KEY_MINE_FRAGMENT)
                .add(R.id.fragmentContainer, searchFragment, KEY_SEARCH_FRAGMENT)
                .hide(liveFragment)
                .hide(lotteryFragment)
                .hide(libraryFragment)
                .hide(mineFragment)
                .hide(searchFragment)
                .commitNow();

            android.util.Log.d("MainActivity", "Fragments created and added. homeFragment.visible=" + homeFragment.getView());
            currentFragment = homeFragment;
        } else {
            homeFragment = (HomeFragment) fm.findFragmentByTag(KEY_HOME_FRAGMENT);
            liveFragment = (LiveFragment) fm.findFragmentByTag(KEY_LIVE_FRAGMENT);
            lotteryFragment = (LotteryFragment) fm.findFragmentByTag(KEY_LOTTERY_FRAGMENT);
            libraryFragment = (LibraryFragment) fm.findFragmentByTag(KEY_LIBRARY_FRAGMENT);
            mineFragment = (MineFragment) fm.findFragmentByTag(KEY_MINE_FRAGMENT);
            searchFragment = (SearchFragment) fm.findFragmentByTag(KEY_SEARCH_FRAGMENT);

            // Restore the last active fragment from saved state
            String activeFragmentTag = savedInstanceState != null ? savedInstanceState.getString("active_fragment", KEY_HOME_FRAGMENT) : KEY_HOME_FRAGMENT;
            currentFragment = fm.findFragmentByTag(activeFragmentTag);
            if (currentFragment == null) {
                currentFragment = homeFragment;
            }
        }

        setupBottomNavigation();
        setupLongPress();
        // setupLogFloatingButton(); // Temporarily disabled for debugging
    }

    private void setupLongPress() {
        // REMOVED: OnTouchListener was interfering with onClick listeners
        // Long press is now handled in setupBottomNavigation via GestureDetector
    }
    
    /**
     * 设置日志悬浮按钮 - 点击可显示日志窗口
     */
    private void setupLogFloatingButton() {
        // 创建悬浮按钮 - 使用 TextView 作为按钮
        logFloatingButton = new TextView(this);
        
        // 设置按钮样式 - 圆形背景
        logFloatingButton.setText("!");
        logFloatingButton.setTextColor(Color.WHITE);
        logFloatingButton.setTextSize(16f);
        logFloatingButton.setGravity(Gravity.CENTER);
        logFloatingButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_log_floating_button));
        logFloatingButton.setClickable(true);
        logFloatingButton.setFocusable(true);
        logFloatingButton.setVisibility(View.VISIBLE);
        
        // 设置大小和位置
        int size = dp2px(40);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.bottomMargin = dp2px(100);
        params.rightMargin = dp2px(20);
        logFloatingButton.setLayoutParams(params);
        
        // 点击事件 - 显示/隐藏日志窗口
        logFloatingButton.setOnClickListener(v -> {
            if (logWindow == null || !logWindow.isShowing()) {
                showLogWindow();
            } else {
                logWindow.dismiss();
            }
        });
        
        // 添加到窗口
        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        decorView.addView(logFloatingButton);
        
        LogManager.addLog("日志悬浮按钮已初始化");
    }
    
    /**
     * 显示日志窗口
     */
    private void showLogWindow() {
        if (logWindow == null) {
            logWindow = new LogWindowFragment(logFloatingButton);
        }
        
        // 显示窗口
        logWindow.showAsDropDown(logFloatingButton, 0, -dp2px(400));
        
        LogManager.addLog("打开日志窗口");
    }
    
    /**
     * dp 转 px
     */
    private int dp2px(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private void showRepoManageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("仓库管理");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_repo_manage, null);
        builder.setView(dialogView);
        
        RecyclerView rvWarehouses = dialogView.findViewById(R.id.rvWarehouses);
        TextView tvEmptyHint = dialogView.findViewById(R.id.tvEmptyHint);
        Button btnAddRepo = dialogView.findViewById(R.id.btnAddRepo);
        
        rvWarehouses.setLayoutManager(new LinearLayoutManager(this));
        WarehouseListAdapter adapter = new WarehouseListAdapter();
        rvWarehouses.setAdapter(adapter);
        
        final AlertDialog dialog = builder.create();
        
        new Thread(() -> {
            List<Warehouse> warehouses = DbHelper.getInstance(this).warehouseDao().getAll();
            runOnUiThread(() -> {
                adapter.setItems(warehouses);
                if (warehouses.isEmpty()) {
                    tvEmptyHint.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyHint.setVisibility(View.GONE);
                }
            });
        }).start();
        
        btnAddRepo.setOnClickListener(v -> {
            dialog.dismiss();
            showAddWarehouseDialog();
        });
        
        dialog.show();
    }

    private void showAddWarehouseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加仓库");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_warehouse, null);
        builder.setView(dialogView);
        
        android.widget.EditText etName = dialogView.findViewById(R.id.etDialogName);
        android.widget.EditText etUrl = dialogView.findViewById(R.id.etDialogUrl);
        TextView tvStatus = dialogView.findViewById(R.id.tvDialogStatus);
        Button btnConfirm = dialogView.findViewById(R.id.btnDialogConfirm);
        
        final AlertDialog dialog = builder.create();
        
        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String url = etUrl.getText().toString().trim();
            
            if (name.isEmpty()) {
                tvStatus.setText("请输入仓库名称");
                tvStatus.setTextColor(0xffff6b6b);
                return;
            }
            if (url.isEmpty()) {
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
                try {
                    String warehouseId = java.util.UUID.randomUUID().toString();
                    Warehouse warehouse = new Warehouse();
                    warehouse.id = warehouseId;
                    warehouse.name = name;
                    warehouse.url = url;
                    warehouse.createdAt = System.currentTimeMillis();
                    DbHelper.getInstance(MainActivity.this).warehouseDao().insert(warehouse);
                    
                    String response = new HttpClient().httpGet(url);
                    runOnUiThread(() -> tvStatus.setText("正在解析配置..."));
                    
                    boolean success = parseAndSaveSites(response, warehouseId);
                    
                    runOnUiThread(() -> {
                        if (success) {
                            tvStatus.setText("成功！已加载站点");
                            tvStatus.setTextColor(0xff4ade80);
                            dialog.dismiss();
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
                site.id = java.util.UUID.randomUUID().toString();
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

    public void showSiteSelectPanel() {
        new Thread(() -> {
            List<SiteConfig> sites = DbHelper.getInstance(this).siteConfigDao().getAll();
            runOnUiThread(() -> {
                showSitePanel(sites);
            });
        }).start();
    }

    private void showSitePanel(List<SiteConfig> sites) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择站点");
        
        if (sites.isEmpty()) {
            builder.setMessage("暂无站点，请先添加仓库");
        } else {
            String[] siteNames = new String[sites.size()];
            for (int i = 0; i < sites.size(); i++) {
                siteNames[i] = sites.get(i).name;
            }
            
            builder.setItems(siteNames, (dialog, which) -> {
                SiteConfig selected = sites.get(which);
                selectSite(selected);
            });
        }
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void selectSite(SiteConfig site) {
        try {
            App.setCurrentSiteName(site.name);
        } catch (Exception e) {
            // ignore
        }
        currentSite = site;
        Toast.makeText(this, "已选择: " + site.name, Toast.LENGTH_SHORT).show();
        
        if (homeFragment != null) {
            homeFragment.reloadWithSite(site);
        }
    }

    private void setupBottomNavigation() {
        TextView navHome = findViewById(R.id.navHome);
        TextView navLive = findViewById(R.id.navLive);
        TextView navLottery = findViewById(R.id.navLottery);
        TextView navLibrary = findViewById(R.id.navLibrary);
        TextView navMine = findViewById(R.id.navMine);

        // Use GestureDetector to distinguish click vs long-press
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }
            
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                return false;
            }
            
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (currentFragment == homeFragment) {
                    showRepoManageDialog();
                }
            }
        });

        // Home: single tap switches to home, long press opens repo management
        navHome.setOnClickListener(v -> switchToFragment(homeFragment, R.id.navHome));
        navHome.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        navLive.setOnClickListener(v -> switchToFragment(liveFragment, R.id.navLive));
        navLottery.setOnClickListener(v -> switchToFragment(lotteryFragment, R.id.navLottery));
        navLibrary.setOnClickListener(v -> switchToFragment(libraryFragment, R.id.navLibrary));
        navMine.setOnClickListener(v -> switchToFragment(mineFragment, R.id.navMine));

        // Click on site name in home fragment header opens site select
        if (homeFragment != null) {
            homeFragment.setOnSiteNameClickListener(v -> showSiteSelectPanel());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String currentTag = currentFragment != null ? getCurrentFragmentKey() : KEY_HOME_FRAGMENT;
        outState.putString("active_fragment", currentTag);
    }

    private String getCurrentFragmentKey() {
        if (currentFragment == homeFragment) return KEY_HOME_FRAGMENT;
        if (currentFragment == liveFragment) return KEY_LIVE_FRAGMENT;
        if (currentFragment == lotteryFragment) return KEY_LOTTERY_FRAGMENT;
        if (currentFragment == libraryFragment) return KEY_LIBRARY_FRAGMENT;
        if (currentFragment == mineFragment) return KEY_MINE_FRAGMENT;
        if (currentFragment == searchFragment) return KEY_SEARCH_FRAGMENT;
        return KEY_HOME_FRAGMENT;
    }

    private void switchToFragment(Fragment fragment, int navItemId) {
        if (fragment == null) return;
        
        android.util.Log.d("MainActivity", "switchToFragment called: target=" + fragment.getClass().getSimpleName() + ", current=" + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
        android.util.Log.d("MainActivity", "  target.isVisible()=" + fragment.isAdded() + ", target.getView()=" + (fragment.getView() != null ? "not-null" : "null"));
        
        FragmentManager fm = getSupportFragmentManager();
        
        if (currentFragment != fragment) {
            FragmentTransaction ft = fm.beginTransaction();
            if (currentFragment != null) {
                android.util.Log.d("MainActivity", "  hiding " + currentFragment.getClass().getSimpleName());
                ft.hide(currentFragment);
            }
            android.util.Log.d("MainActivity", "  showing " + fragment.getClass().getSimpleName());
            ft.commitNow();
            android.util.Log.d("MainActivity", "  commitNow completed");
            currentFragment = fragment;
        } else {
            android.util.Log.d("MainActivity", "  same fragment, skipping switch");
        }

        android.util.Log.d("MainActivity", "  After switch - homeFragment.getView()=" + (homeFragment != null ? (homeFragment.getView() != null ? "not-null" : "null") : "null"));
        android.util.Log.d("MainActivity", "  After switch - homeFragment.isAdded()=" + (homeFragment != null ? homeFragment.isAdded() : "null"));
        if (homeFragment != null && homeFragment.getView() != null) {
            android.util.Log.d("MainActivity", "  homeFragment.view width=" + homeFragment.getView().getWidth() + ", height=" + homeFragment.getView().getHeight());
            android.util.Log.d("MainActivity", "  homeFragment.view visibility=" + homeFragment.getView().getVisibility());
            android.util.Log.d("MainActivity", "  homeFragment.view alpha=" + homeFragment.getView().getAlpha());
        }
        
        findViewById(R.id.navHome).setSelected(false);
        findViewById(R.id.navLive).setSelected(false);
        findViewById(R.id.navLottery).setSelected(false);
        findViewById(R.id.navLibrary).setSelected(false);
        findViewById(R.id.navMine).setSelected(false);
        findViewById(navItemId).setSelected(true);
    }

    public void loadSites(final SitesLoadedCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<SiteConfig> sites = DbHelper.getInstance(getApplicationContext()).siteConfigDao().getAll();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSitesLoaded(sites);
                    }
                });
            }
        }).start();
    }

    public void getCurrentSite(final CurrentSiteCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<SiteConfig> sites = DbHelper.getInstance(getApplicationContext()).siteConfigDao().getAll();
                final SiteConfig site = sites.isEmpty() ? null : sites.get(0);
                currentSite = site;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSiteLoaded(site);
                    }
                });
            }
        }).start();
    }

    public void openMovieDetail(Movie movie) {
        Intent intent = new Intent(this, MovieDetailActivity.class);
        if (movie != null) {
            intent.putExtra("vod_id", movie.vodId);
            intent.putExtra("title", movie.title);
            intent.putExtra("pic", movie.pic);
            intent.putExtra("tag", movie.tag);
            intent.putExtra("type", movie.type);
            intent.putExtra("year", movie.year);
            intent.putExtra("area", movie.area);
            intent.putExtra("play", movie.play);
            intent.putExtra("desc", movie.desc);
            intent.putExtra("score", movie.score);
            intent.putExtra("actor", movie.actor);
            intent.putExtra("director", movie.director);
        }
        
        if (currentSite != null && currentSite.type == 3) {
            String pluginKey = !currentSite.key.isEmpty() ? currentSite.key : currentSite.name;
            intent.putExtra("plugin_key", pluginKey);
        }
        
        startActivity(intent);
    }

    public void playLiveChannel(LiveChannel channel) {
        Intent intent = new Intent(this, ExoPlayerActivity.class);
        intent.putExtra("url", channel.url);
        intent.putExtra("title", channel.name);
        startActivity(intent);
    }

    public void switchToSearch() {
        if (searchFragment != null) {
            switchToFragment(searchFragment, R.id.navHome);
        }
    }

    public void switchToLive() {
        if (liveFragment != null) {
            switchToFragment(liveFragment, R.id.navLive);
        }
    }

    public void switchToLottery() {
        if (lotteryFragment != null) {
            switchToFragment(lotteryFragment, R.id.navLottery);
        }
    }

    public void switchToLibrary() {
        if (libraryFragment != null) {
            switchToFragment(libraryFragment, R.id.navLibrary);
        }
    }

    // Warehouse list adapter for dialog
    private static class WarehouseListAdapter extends RecyclerView.Adapter<WarehouseListAdapter.ViewHolder> {
        private List<Warehouse> warehouses = new ArrayList<>();
        private OnWarehouseDeleteListener deleteListener;

        interface OnWarehouseDeleteListener {
            void onDelete(Warehouse warehouse);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvUrl;
            ImageView ivDelete;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvWarehouseName);
                tvUrl = v.findViewById(R.id.tvWarehouseUrl);
                ivDelete = v.findViewById(R.id.ivDeleteWarehouse);
            }
        }

        void setItems(List<Warehouse> items) {
            warehouses = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_warehouse_dialog, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Warehouse w = warehouses.get(position);
            holder.tvName.setText(w.name);
            holder.tvUrl.setText(w.url);
            holder.ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(w);
                }
            });
        }

        @Override
        public int getItemCount() { return warehouses.size(); }
    }
}
