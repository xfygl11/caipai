package webapp.newcloud.lottery.movie.example;

import webapp.newcloud.lottery.movie.util.LogManager;

/**
 * 日志管理器使用示例
 * 展示如何在项目中使用 LogManager
 */
public class LogManagerExample {
    
    /**
     * 示例1: 记录普通日志
     */
    public void example1_NormalLogging() {
        // 记录普通信息
        LogManager.addLog("用户登录成功");
        LogManager.addLog("加载数据完成");
        LogManager.addLog("页面切换: Home -> Detail");
    }
    
    /**
     * 示例2: 记录错误日志
     */
    public void example2_ErrorLogging() {
        // 记录错误信息
        LogManager.addError("网络连接失败");
        LogManager.addError("数据解析错误");
        LogManager.addError("文件读写失败");
    }
    
    /**
     * 示例3: 记录异常信息
     */
    public void example3_ExceptionLogging() {
        try {
            // 模拟可能出错的代码
            String data = null;
            int length = data.length();
        } catch (Exception e) {
            // 记录异常
            LogManager.addException(e);
            
            // 或者记录带上下文的异常
            LogManager.addError("处理用户数据时发生异常: " + e.getMessage());
            LogManager.addException(e);
        }
    }
    
    /**
     * 示例4: 记录不同级别的日志
     */
    public void example4_LevelLogging() {
        // 调试日志
        LogManager.addLog(LogManager.Level.DEBUG, "进入方法: getUserData");
        
        // 信息日志
        LogManager.addLog(LogManager.Level.INFO, "用户ID: 12345");
        
        // 警告日志
        LogManager.addLog(LogManager.Level.WARN, "数据缓存即将过期");
        
        // 错误日志
        LogManager.addLog(LogManager.Level.ERROR, "数据库连接失败");
        
        // 崩溃日志
        LogManager.addLog(LogManager.Level.CRASH, "应用发生严重错误");
    }
    
    /**
     * 示例5: 日志导出
     */
    public void example5_LogExport() {
        // 导出日志到文件 - 使用示例路径
        String filePath = "/data/data/webapp.newcloud.lottery.movie/files/app_logs.txt";
        boolean success = LogManager.exportLogs(filePath);
        
        if (success) {
            LogManager.addLog("日志导出成功: " + filePath);
        } else {
            LogManager.addError("日志导出失败");
        }
    }
    
    /**
     * 示例6: 日志过滤
     */
    public void example6_LogFiltering() {
        // 获取错误级别的日志
        java.util.List<String> errorLogs = LogManager.getLogsByLevel(LogManager.Level.ERROR);
        
        // 获取崩溃级别的日志
        java.util.List<String> crashLogs = LogManager.getLogsByLevel(LogManager.Level.CRASH);
        
        // 获取警告级别的日志
        java.util.List<String> warnLogs = LogManager.getLogsByLevel(LogManager.Level.WARN);
    }
    
    /**
     * 示例7: 日志管理
     */
    public void example7_LogManagement() {
        // 检查是否有日志
        if (LogManager.hasLogs()) {
            int count = LogManager.getLogCount();
            LogManager.addLog("当前有 " + count + " 条日志");
        }
        
        // 清空日志
        LogManager.clearLogs();
    }
    
    /**
     * 示例8: 在实际业务中使用
     */
    public void example8_BusinessUsage() {
        // 网络请求
        try {
            LogManager.addLog("开始网络请求: GET /api/users");
            // 模拟网络请求
            String response = simulateNetworkRequest();
            LogManager.addLog("网络请求成功，响应长度: " + response.length());
        } catch (Exception e) {
            LogManager.addError("网络请求失败: " + e.getMessage());
            LogManager.addException(e);
        }
        
        // 数据处理
        try {
            LogManager.addLog(LogManager.Level.DEBUG, "开始处理用户数据");
            // 模拟数据处理
            processData();
            LogManager.addLog(LogManager.Level.INFO, "数据处理完成");
        } catch (Exception e) {
            LogManager.addError("数据处理失败: " + e.getMessage());
            LogManager.addException(e);
        }
    }
    
    private String simulateNetworkRequest() {
        // 模拟网络请求
        return "{\"users\":[]}";
    }
    
    private void processData() {
        // 模拟数据处理
    }
    
    /**
     * 示例9: 性能监控
     */
    public void example9_PerformanceMonitoring() {
        long startTime = System.currentTimeMillis();
        
        try {
            LogManager.addLog(LogManager.Level.DEBUG, "开始执行耗时操作");
            
            // 模拟耗时操作
            simulateLongRunningOperation();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            LogManager.addLog(LogManager.Level.INFO, "耗时操作完成，耗时: " + duration + "ms");
            
            // 如果耗时过长，记录警告
            if (duration > 5000) {
                LogManager.addLog(LogManager.Level.WARN, "耗时操作超过5秒: " + duration + "ms");
            }
            
        } catch (Exception e) {
            LogManager.addError("耗时操作失败: " + e.getMessage());
            LogManager.addException(e);
        }
    }
    
    private void simulateLongRunningOperation() {
        // 模拟耗时操作
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 示例10: 用户行为追踪
     */
    public void example10_UserBehaviorTracking() {
        LogManager.addLog("用户点击: 首页");
        LogManager.addLog("用户点击: 直播");
        LogManager.addLog("用户点击: 彩票");
        LogManager.addLog("用户点击: 收藏");
        LogManager.addLog("用户点击: 我的");
        LogManager.addLog("用户点击: 站点管理");
        LogManager.addLog("用户点击: 仓库管理");
    }
}
