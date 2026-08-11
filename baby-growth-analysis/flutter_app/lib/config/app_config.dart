// Flutter 配置文件 - API 地址
// APP 直接对接已有后端服务

class AppConfig {
  /// 后端 API 基础地址（固定 localhost:8000）
  static const String apiBaseUrl = 'http://localhost:8000';

  /// 静态文件基础路径
  static const String mediaBase = 'http://localhost:8000';

  /// 宝宝出生日期（2026-07-22 11:42:54 CST）
  static const String babyBirthTime = '2026-07-22T11:42:54+08:00';
  static const String babyName = '小公主';
}
