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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.personalassistant.app.R;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.FavoriteEntity;
import com.personalassistant.app.db.entity.HistoryEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LibraryFragment extends Fragment {
    private TabHost tabHost;
    private LinearLayout historyList;
    private LinearLayout favList;
    private TextView historyEmpty;
    private TextView favEmpty;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Disposable historyDisposable;
    private Disposable favDisposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF05070D);

        // Header
        TextView header = new TextView(requireContext());
        header.setText("历史 / 收藏");
        header.setTextColor(0xFFFFFFFF);
        header.setTextSize(22);
        header.setPadding(0, dp(16), 0, dp(16));
        header.setGravity(android.view.Gravity.CENTER);
        root.addView(header);

        // TabHost
        tabHost = new TabHost(requireContext(), null);
        tabHost.setId(android.R.id.tabhost);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        tabHost.setLayoutParams(tabParams);

        LinearLayout tabContainer = new LinearLayout(requireContext());
        tabContainer.setOrientation(LinearLayout.VERTICAL);
        tabHost.addView(tabContainer);

        TabWidget tabs = new android.widget.TabWidget(requireContext());
        tabs.setId(android.R.id.tabs);
        tabContainer.addView(tabs);

        FrameLayout frame = new FrameLayout(requireContext());
        frame.setId(android.R.id.tabcontent);
        frame.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        tabContainer.addView(frame);

        // History tab content
        LinearLayout historyContent = new LinearLayout(requireContext());
        historyContent.setId(R.id.history_tab);
        historyContent.setOrientation(LinearLayout.VERTICAL);
        historyContent.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        historyContent.setPadding(dp(16), dp(8), dp(16), dp(8));
        tabContainer.addView(historyContent);

        // Clear button
        LinearLayout historyHeaderRow = new LinearLayout(requireContext());
        historyHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        historyHeaderRow.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        historyHeaderRow.setPadding(0, dp(4), 0, dp(8));

        TextView historyTitle = new TextView(requireContext());
        historyTitle.setText("观看历史");
        historyTitle.setTextColor(0xFFFFFFFF);
        historyTitle.setTextSize(16);
        historyTitle.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        historyTitle.setLayoutParams(titleParams);
        historyHeaderRow.addView(historyTitle);

        Button historyClearBtn = new Button(requireContext());
        historyClearBtn.setText("清空");
        historyClearBtn.setTextSize(11);
        historyClearBtn.setAllCaps(false);
        historyClearBtn.setTextColor(0xFFFF6B6B);
        historyClearBtn.setBackgroundColor(0x0FFFFFFF);
        historyClearBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        historyClearBtn.setOnClickListener(v -> showHistoryClearConfirm());
        historyHeaderRow.addView(historyClearBtn);
        historyContent.addView(historyHeaderRow);

        historyList = new LinearLayout(requireContext());
        historyList.setOrientation(LinearLayout.VERTICAL);
        historyContent.addView(historyList);

        historyEmpty = new TextView(requireContext());
        historyEmpty.setText("暂无历史记录");
        historyEmpty.setTextColor(0xFF667788);
        historyEmpty.setGravity(android.view.Gravity.CENTER);
        historyEmpty.setPadding(0, dp(60), 0, 0);
        historyList.addView(historyEmpty);

        // Favorites tab content
        LinearLayout favContent = new LinearLayout(requireContext());
        favContent.setId(R.id.fav_tab);
        favContent.setOrientation(LinearLayout.VERTICAL);
        favContent.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        favContent.setPadding(dp(16), dp(8), dp(16), dp(8));
        tabContainer.addView(favContent);

        LinearLayout favHeaderRow = new LinearLayout(requireContext());
        favHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        favHeaderRow.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        favHeaderRow.setPadding(0, dp(4), 0, dp(8));

        TextView favTitle = new TextView(requireContext());
        favTitle.setText("我的收藏");
        favTitle.setTextColor(0xFFFFFFFF);
        favTitle.setTextSize(16);
        favTitle.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams favTitleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        favTitle.setLayoutParams(favTitleParams);
        favHeaderRow.addView(favTitle);

        Button favClearBtn = new Button(requireContext());
        favClearBtn.setText("清空");
        favClearBtn.setTextSize(11);
        favClearBtn.setAllCaps(false);
        favClearBtn.setTextColor(0xFFFF6B6B);
        favClearBtn.setBackgroundColor(0x0FFFFFFF);
        favClearBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        favClearBtn.setOnClickListener(v -> showFavClearConfirm());
        favHeaderRow.addView(favClearBtn);
        favContent.addView(favHeaderRow);

        favList = new LinearLayout(requireContext());
        favList.setOrientation(LinearLayout.VERTICAL);
        favContent.addView(favList);

        favEmpty = new TextView(requireContext());
        favEmpty.setText("暂无收藏影片");
        favEmpty.setTextColor(0xFF667788);
        favEmpty.setGravity(android.view.Gravity.CENTER);
        favEmpty.setPadding(0, dp(60), 0, 0);
        favList.addView(favEmpty);

        // Setup tabs
        tabHost.setup();
        TabHost.TabSpec historyTab = tabHost.newTabSpec("history");
        historyTab.setIndicator("历史");
        historyTab.setContent(R.id.history_tab);
        tabHost.addTab(historyTab);

        TabHost.TabSpec favTab = tabHost.newTabSpec("favorites");
        favTab.setIndicator("收藏");
        favTab.setContent(R.id.fav_tab);
        tabHost.addTab(favTab);

        root.addView(tabHost);

        loadHistory();
        loadFavorites();

        return root;
    }

    private void loadHistory() {
        historyDisposable = AppDatabase.getInstance(requireContext())
                .historyDao()
                .getRecentHistory()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(histories -> {
                    historyList.removeAllViews();
                    if (histories == null || histories.isEmpty()) {
                        historyList.addView(historyEmpty);
                        return;
                    }
                    for (HistoryEntity h : histories) {
                        historyList.addView(createHistoryRow(h));
                    }
                }, e -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
    }

    private void loadFavorites() {
        favDisposable = AppDatabase.getInstance(requireContext())
                .favoriteDao()
                .getAllFavorites()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(favorites -> {
                    favList.removeAllViews();
                    if (favorites == null || favorites.isEmpty()) {
                        favList.addView(favEmpty);
                        return;
                    }
                    for (FavoriteEntity f : favorites) {
                        favList.addView(createFavRow(f));
                    }
                }, e -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
    }

    private LinearLayout createHistoryRow(HistoryEntity h) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackgroundColor(0xFF101D2B);
        row.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MoviePlayerActivity.class);
            intent.putExtra("vod_id", h.movieId);
            intent.putExtra("site_base", h.siteBase);
            intent.putExtra("site_name", h.siteName);
            startActivity(intent);
        });

        ImageView poster = new ImageView(requireContext());
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(50), dp(70));
        posterParams.rightMargin = dp(10);
        poster.setLayoutParams(posterParams);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(0xFF1D3557);
        row.addView(poster);

        if (h.pic != null && !h.pic.isEmpty()) {
            Glide.with(requireContext())
                    .load(h.pic)
                    .centerCrop()
                    .placeholder(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .error(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .into(poster);
        }

        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textCol.setLayoutParams(textParams);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(h.movieTitle);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(14);
        textCol.addView(tvTitle);

        StringBuilder info = new StringBuilder();
        if (h.siteName != null && !h.siteName.isEmpty()) info.append(h.siteName);
        if (h.playFrom != null && !h.playFrom.isEmpty()) {
            if (info.length() > 0) info.append(" / ");
            info.append(h.playFrom);
        }
        if (h.episodeName != null && !h.episodeName.isEmpty()) {
            info.append(" - ").append(h.episodeName);
        }
        TextView tvInfo = new TextView(requireContext());
        tvInfo.setText(info.toString());
        tvInfo.setTextColor(0xFF8899AA);
        tvInfo.setTextSize(11);
        tvInfo.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvInfo);

        row.addView(textCol);
        return row;
    }

    private LinearLayout createFavRow(FavoriteEntity f) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackgroundColor(0xFF101D2B);
        row.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MoviePlayerActivity.class);
            intent.putExtra("vod_id", f.movieId);
            intent.putExtra("site_base", f.siteBase);
            intent.putExtra("site_name", f.siteName);
            startActivity(intent);
        });

        ImageView poster = new ImageView(requireContext());
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(50), dp(70));
        posterParams.rightMargin = dp(10);
        poster.setLayoutParams(posterParams);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(0xFF1D3557);
        row.addView(poster);

        if (f.pic != null && !f.pic.isEmpty()) {
            Glide.with(requireContext())
                    .load(f.pic)
                    .centerCrop()
                    .placeholder(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .error(new android.graphics.drawable.ColorDrawable(0xFF1D3557))
                    .into(poster);
        }

        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textCol.setLayoutParams(textParams);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(f.movieTitle);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(14);
        textCol.addView(tvTitle);

        TextView tvInfo = new TextView(requireContext());
        tvInfo.setText(f.siteName != null ? f.siteName : "");
        tvInfo.setTextColor(0xFF8899AA);
        tvInfo.setTextSize(11);
        tvInfo.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvInfo);

        Button unfavBtn = new Button(requireContext());
        unfavBtn.setText("取消");
        unfavBtn.setTextSize(10);
        unfavBtn.setAllCaps(false);
        unfavBtn.setTextColor(0xFFFF6B35);
        unfavBtn.setBackgroundColor(0x0FFFFFFF);
        unfavBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        unfavBtn.setOnClickListener(v -> {
            executor.execute(() -> {
                AppDatabase.getInstance(requireContext()).favoriteDao().remove(f.movieId, f.siteName);
                handler.post(this::loadFavorites);
            });
        });
        textCol.addView(unfavBtn);

        row.addView(textCol);
        return row;
    }

    private void showHistoryClearConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清空历史")
                .setMessage("确定要清空所有历史记录吗？")
                .setPositiveButton("确定", (d, w) -> {
                    executor.execute(() -> {
                        AppDatabase.getInstance(requireContext()).historyDao().clearAll();
                        handler.post(this::loadHistory);
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showFavClearConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("清空收藏")
                .setMessage("确定要清空所有收藏吗？")
                .setPositiveButton("确定", (d, w) -> {
                    executor.execute(() -> {
                        AppDatabase.getInstance(requireContext()).favoriteDao().clearAll();
                        handler.post(this::loadFavorites);
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
        if (historyDisposable != null && !historyDisposable.isDisposed()) {
            historyDisposable.dispose();
        }
        if (favDisposable != null && !favDisposable.isDisposed()) {
            favDisposable.dispose();
        }
    }

    static int dp(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}
