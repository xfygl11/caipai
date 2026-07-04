package com.personalassistant.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.personalassistant.app.R;
import com.personalassistant.app.db.AppDatabase;
import com.personalassistant.app.db.entity.HistoryEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HistoryFragment extends Fragment {
    private LinearLayout historyList;
    private TextView emptyText;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF05070D);
        root.setPadding(dp(16), dp(16), dp(16), dp(0));

        // Header
        TextView header = new TextView(requireContext());
        header.setText("观看历史");
        header.setTextColor(0xFFFFFFFF);
        header.setTextSize(18);
        header.setPadding(0, dp(8), 0, dp(16));
        root.addView(header);

        // Clear button
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.END);

        Button clearBtn = new Button(requireContext());
        clearBtn.setText("清空");
        clearBtn.setTextSize(11);
        clearBtn.setAllCaps(false);
        clearBtn.setTextColor(0xFFFF6B6B);
        clearBtn.setBackgroundColor(0x0FFFFFFF);
        clearBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        clearBtn.setOnClickListener(v -> showClearConfirm());
        headerRow.addView(clearBtn);
        root.addView(headerRow);

        // List
        historyList = new LinearLayout(requireContext());
        historyList.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyList);

        emptyText = new TextView(requireContext());
        emptyText.setText("暂无历史记录");
        emptyText.setTextColor(0xFF667788);
        emptyText.setGravity(android.view.Gravity.CENTER);
        emptyText.setPadding(0, dp(60), 0, 0);
        historyList.addView(emptyText);

        loadHistory();
        return root;
    }

    private void loadHistory() {
        disposable = AppDatabase.getInstance(requireContext())
                .historyDao()
                .getRecentHistory()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(histories -> {
                    historyList.removeAllViews();
                    if (histories == null || histories.isEmpty()) {
                        historyList.addView(emptyText);
                        return;
                    }
                    for (HistoryEntity h : histories) {
                        historyList.addView(createHistoryRow(h));
                    }
                }, e -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
    }

    private LinearLayout createHistoryRow(HistoryEntity h) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackgroundColor(0xFF101D2B);
        row.setOnClickListener(v -> openHistory(h));

        ImageView poster = new ImageView(requireContext());
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(50), dp(70));
        posterParams.rightMargin = dp(10);
        poster.setLayoutParams(posterParams);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(0xFF1D3557);
        row.addView(poster);

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

        // Progress bar
        if (h.duration > 0 && h.position > 0) {
            float progress = (float) h.position / h.duration;
            LinearLayout progressBar = new LinearLayout(requireContext());
            progressBar.setOrientation(LinearLayout.HORIZONTAL);
            progressBar.setPadding(0, dp(4), 0, 0);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    dp((int)(100 * progress)), ViewGroup.LayoutParams.WRAP_CONTENT);
            barParams.leftMargin = dp(2);
            TextView tvProgress = new TextView(requireContext());
            tvProgress.setText(String.format("%.0f%%", progress * 100));
            tvProgress.setTextColor(0xFF7C3AED);
            tvProgress.setTextSize(10);
            tvProgress.setLayoutParams(barParams);
            progressBar.addView(tvProgress);

            TextView tvFiller = new TextView(requireContext());
            LinearLayout.LayoutParams fillerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            fillerParams.weight = 1;
            tvFiller.setLayoutParams(fillerParams);
            progressBar.addView(tvFiller);
            textCol.addView(progressBar);
        }

        row.addView(textCol);
        return row;
    }

    private void openHistory(HistoryEntity h) {
        // Open detail or play directly
        Toast.makeText(requireContext(), "打开: " + h.movieTitle, Toast.LENGTH_SHORT).show();
    }

    private void showClearConfirm() {
        new android.app.AlertDialog.Builder(requireContext())
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    static int dp(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}
