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
import com.personalassistant.app.db.entity.FavoriteEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FavoritesFragment extends Fragment {
    private LinearLayout favList;
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

        TextView header = new TextView(requireContext());
        header.setText("我的收藏");
        header.setTextColor(0xFFFFFFFF);
        header.setTextSize(18);
        header.setPadding(0, dp(8), 0, dp(16));
        root.addView(header);

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

        favList = new LinearLayout(requireContext());
        favList.setOrientation(LinearLayout.VERTICAL);
        root.addView(favList);

        emptyText = new TextView(requireContext());
        emptyText.setText("暂无收藏");
        emptyText.setTextColor(0xFF667788);
        emptyText.setGravity(android.view.Gravity.CENTER);
        emptyText.setPadding(0, dp(60), 0, 0);
        favList.addView(emptyText);

        loadFavorites();
        return root;
    }

    private void loadFavorites() {
        disposable = AppDatabase.getInstance(requireContext())
                .favoriteDao()
                .getAllFavorites()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(favs -> {
                    favList.removeAllViews();
                    if (favs == null || favs.isEmpty()) {
                        favList.addView(emptyText);
                        return;
                    }
                    for (FavoriteEntity f : favs) {
                        favList.addView(createFavRow(f));
                    }
                }, e -> Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show());
    }

    private LinearLayout createFavRow(FavoriteEntity f) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackgroundColor(0xFF101D2B);

        ImageView poster = new ImageView(requireContext());
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(50), dp(70));
        posterParams.rightMargin = dp(10);
        poster.setLayoutParams(posterParams);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(0xFF1D3557);
        if (f.pic != null && !f.pic.isEmpty()) {
            Glide.with(requireContext()).load(f.pic).into(poster);
        }
        row.addView(poster);

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
        StringBuilder info = new StringBuilder();
        if (f.siteName != null && !f.siteName.isEmpty()) info.append(f.siteName);
        if (f.tag != null && !f.tag.isEmpty()) info.append(" / ").append(f.tag);
        if (f.year != null && !f.year.isEmpty()) info.append(" / ").append(f.year);
        tvInfo.setText(info.toString());
        tvInfo.setTextColor(0xFF8899AA);
        tvInfo.setTextSize(11);
        tvInfo.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvInfo);

        // Remove button
        Button removeBtn = new Button(requireContext());
        removeBtn.setText("\u00D7");
        removeBtn.setTextSize(16);
        removeBtn.setAllCaps(false);
        removeBtn.setTextColor(0xFFFF6B6B);
        removeBtn.setBackgroundColor(0x0FFFFFFF);
        removeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        final FavoriteEntity finalF = f;
        removeBtn.setOnClickListener(v -> {
            executor.execute(() -> {
                AppDatabase.getInstance(requireContext()).favoriteDao().remove(finalF.movieId, finalF.siteName);
                handler.post(FavoritesFragment.this::loadFavorites);
            });
        });
        row.addView(removeBtn);

        return row;
    }

    private void showClearConfirm() {
        new android.app.AlertDialog.Builder(requireContext())
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
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    static int dp(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}
