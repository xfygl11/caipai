package webapp.newcloud.lottery.movie.util;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 日志管理器 - 用于收集和存储应用运行日志和错误信息
 * 支持日志导出功能
 */
public class LogManager {
    private static final String TAG = "LogManager";
    private static final int MAX_LOG_COUNT = 500;
    
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    );
    
    private static final CopyOnWriteArrayList<String> logList = new CopyOnWriteArrayList<>();
    private static volatile LogManager instance;
    
    /**
     * 日志级别枚举
     */
    public enum Level {
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E"),
        CRASH("X");
        
        private final String shortName;
        
        Level(String shortName) {
            this.shortName = shortName;
        }
        
        public String getShortName() {
            return shortName;
        }
    }
    
    private LogManager() {
    }
    
    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (LogManager.class) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 添加日志
     * @param level 日志级别
     * @param message 日志消息
     */
    public static void addLog(Level level, String message) {
        String timestamp = DATE_FORMAT_THREAD_LOCAL.get().format(new Date());
        String logEntry = String.format("[%s] %s | %s", timestamp, level.getShortName(), message);
        
        // CopyOnWriteArrayList 本身是线程安全的，不需要额外的 synchronized
        if (logList.size() >= MAX_LOG_COUNT) {
            logList.remove(0);
        }
        logList.add(logEntry);
        
        // 同时输出到系统日志
        switch (level) {
            case DEBUG:
                Log.d(TAG, message);
                break;
            case INFO:
                Log.i(TAG, message);
                break;
            case WARN:
                Log.w(TAG, message);
                break;
            case ERROR:
            case CRASH:
                Log.e(TAG, message);
                break;
        }
    }
    
    /**
     * 添加普通日志
     * @param message 日志消息
     */
    public static void addLog(String message) {
        addLog(Level.INFO, message);
    }
    
    /**
     * 添加错误日志
     * @param message 错误消息
     */
    public static void addError(String message) {
        addLog(Level.ERROR, message);
    }
    
    /**
     * 添加崩溃日志
     * @param message 崩溃消息
     */
    public static void addCrash(String message) {
        addLog(Level.CRASH, message);
    }
    
    /**
     * 添加异常日志
     * @param exception 异常对象
     */
    public static void addException(Exception exception) {
        if (exception == null) return;
        
        String message = exception.getMessage() != null ? exception.getMessage() : exception.toString();
        addLog(Level.ERROR, message);
        
        // 添加堆栈信息
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        addLog(Level.ERROR, "Stack trace:\n" + sb.toString());
    }
    
    /**
     * 获取所有日志
     * @return 日志列表（只读）
     */
    public static List<String> getLogs() {
        return Collections.unmodifiableList(new ArrayList<>(logList));
    }
    
    /**
     * 获取指定级别的日志
     * @param level 日志级别
     * @return 过滤后的日志列表
     */
    public static List<String> getLogsByLevel(Level level) {
        List<String> filtered = new ArrayList<>();
        String levelPrefix = " " + level.getShortName() + " |";
        for (String log : logList) {
            if (log.contains(levelPrefix)) {
                filtered.add(log);
            }
        }
        return filtered;
    }
    
    /**
     * 清空所有日志
     */
    public static void clearLogs() {
        logList.clear();
        addLog(Level.INFO, "日志已清空");
    }
    
    /**
     * 导出日志到文件
     * @param filePath 文件路径
     * @return 是否导出成功
     */
    public static boolean exportLogs(String filePath) {
        if (logList.isEmpty()) {
            addLog(Level.WARN, "没有可导出的日志");
            return false;
        }
        
        try {
            java.io.File file = new java.io.File(filePath);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            
                writer.write("=== 日志导出 ===\n");
                writer.write("导出时间: " + DATE_FORMAT_THREAD_LOCAL.get().format(new Date()) + "\n");
            writer.write("日志总数: " + logList.size() + "\n");
            writer.write("================\n\n");
            
            for (String log : logList) {
                writer.write(log + "\n");
            }
            
            writer.close();
            addLog(Level.INFO, "日志已导出到: " + filePath);
            return true;
            
        } catch (Exception e) {
            addException(e);
            return false;
        }
    }
    
    /**
     * 检查是否有日志
     * @return 是否有日志
     */
    public static boolean hasLogs() {
        return !logList.isEmpty();
    }
    
    /**
     * 获取日志数量
     * @return 日志数量
     */
    public static int getLogCount() {
        return logList.size();
    }
}
