package webapp.newcloud.lottery.movie.fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.util.LogManager;

/**
 * 悬浮日志窗口 - 用于实时查看应用日志和错误信息
 * 可以通过 PopupWindow 显示，支持日志刷新、清空和导出功能
 */
public class LogWindowFragment extends PopupWindow {
    
    private static final int REFRESH_INTERVAL_MS = 2000; // 2秒刷新一次
    
    private RecyclerView logRecyclerView;
    private LogAdapter logAdapter;
    private TextView tvLogCount;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private View rootView;
    
    public LogWindowFragment(@NonNull View anchorView) {
        super(anchorView.getContext());
        
        // 设置 PopupWindow 属性
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(null);
        
        // 初始化刷新处理器
        refreshHandler = new Handler(Looper.getMainLooper());
        
        // 初始化 UI
        initUI(anchorView);
    }
    
    private void initUI(View anchorView) {
        // 膨胀布局
        rootView = LayoutInflater.from(anchorView.getContext())
                .inflate(R.layout.fragment_log_window, null, false);
        
        setContentView(rootView);
        
        // 初始化控件
        logRecyclerView = rootView.findViewById(R.id.log_recycler_view);
        tvLogCount = rootView.findViewById(R.id.tvLogCount);
        Button btnClear = rootView.findViewById(R.id.btnClear);
        Button btnExport = rootView.findViewById(R.id.btnExport);
        Button btnClose = rootView.findViewById(R.id.btnClose);
        
        // 设置 RecyclerView
        logRecyclerView.setLayoutManager(new LinearLayoutManager(anchorView.getContext()));
        logAdapter = new LogAdapter();
        logRecyclerView.setAdapter(logAdapter);
        
        // 初始化日志数量显示
        updateLogCount();
        
        // 开始定时刷新
        startRefresh();
        
        // 清空按钮
        btnClear.setOnClickListener(v -> {
            LogManager.clearLogs();
            logAdapter.updateLogs();
            updateLogCount();
            Toast.makeText(anchorView.getContext(), "日志已清空", Toast.LENGTH_SHORT).show();
        });
        
        // 导出按钮
        btnExport.setOnClickListener(v -> {
            String filePath = anchorView.getContext().getFilesDir().getAbsolutePath() + "/log_export.txt";
            boolean success = LogManager.exportLogs(filePath);
            if (success) {
                Toast.makeText(anchorView.getContext(), "日志已导出到: " + filePath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(anchorView.getContext(), "导出失败", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 关闭按钮
        btnClose.setOnClickListener(v -> {
            dismiss();
        });
    }
    
    private void updateLogCount() {
        int count = LogManager.getLogCount();
        if (tvLogCount != null) {
            tvLogCount.setText("日志数量: " + count);
        }
    }
    
    private void startRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                logAdapter.updateLogs();
                updateLogCount();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
        refreshHandler.post(refreshRunnable);
    }
    
    private void stopRefresh() {
        if (refreshRunnable != null && refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
    
    @Override
    public void dismiss() {
        stopRefresh();
        super.dismiss();
    }
    
    /**
     * 日志适配器
     */
    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        
        private List<String> logs = new java.util.ArrayList<>();
        
        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView textView = view.findViewById(android.R.id.text1);
            textView.setTextSize(10f);
            textView.setPadding(8, 4, 8, 4);
            textView.setLineSpacing(2, 1f);
            return new LogViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            holder.textView.setText(logs.get(position));
            
            // 根据日志级别设置颜色
            String log = logs.get(position);
            if (log.contains(" E |") || log.contains(" X |")) {
                holder.textView.setTextColor(0xFFFF0000); // 红色
            } else if (log.contains(" W |")) {
                holder.textView.setTextColor(0xFFFFFF00); // 黄色
            } else {
                holder.textView.setTextColor(0xFF000000); // 黑色
            }
        }
        
        @Override
        public int getItemCount() {
            return logs.size();
        }
        
        public void updateLogs() {
            List<String> newLogs = LogManager.getLogs();
            logs.clear();
            logs.addAll(newLogs);
            notifyDataSetChanged();
        }
        
        static class LogViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            
            public LogViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
