// 成长印记 - 主题色配置，与 Web 版保持一致
import 'package:flutter/material.dart';

class AppTheme {
  // ========== 色彩体系 ==========
  static const Color primary = Color(0xFFFF9AA2); // 蜜桃粉
  static const Color primaryLight = Color(0xFFFFD1D9); // 主色浅
  static const Color secondary = Color(0xFFB5EAD7); // 薄荷绿
  static const Color accent = Color(0xFFFFB7B2); // 珊瑚粉
  static const Color aiColor = Color(0xFF7B68EE); // AI色 薰衣草紫
  static const Color feedColor = Color(0xFFFFDAC1); // 喂养色 杏色
  static const Color sleepColor = Color(0xFFC7CEEA); // 睡眠色 薰衣草蓝
  static const Color vaccineColor = Color(0xFFA8D8EA); // 疫苗色 天空蓝
  static const Color learnColor = Color(0xFFE2F0CB); // 学习色 嫩叶绿
  static const Color foodColor = Color(0xFFFFE5B4); // 辅食色 蜜橙
  static const Color checkupColor = Color(0xFFFFB6C1); // 体检色 浅粉红
  static const Color warning = Color(0xFFFF6B6B); // 警告色
  static const Color background = Color(0xFFFFF8F0); // 奶油白背景
  static const Color textPrimary = Color(0xFF4A3728); // 暖棕正文
  static const Color textSecondary = Color(0xFF8B7E6E); // 辅助文字

  // ========== 圆角 ==========
  static const double radiusCard = 16;
  static const double radiusButton = 12;
  static const double radiusTag = 20;

  // ========== 间距 ==========
  static const double spacingXs = 4;
  static const double spacingSm = 8;
  static const double spacingMd = 12;
  static const double spacingLg = 16;
  static const double spacingXl = 24;

  // ========== 主题数据 ==========
  static ThemeData get theme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      colorScheme: const ColorScheme.light(
        primary: primary,
        secondary: secondary,
        surface: background,
        error: warning,
      ),
      scaffoldBackgroundColor: background,

      // 卡片
      cardTheme: CardThemeData(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radiusCard),
        ),
        color: Colors.white,
      ),

      // 按钮
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radiusButton),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),

      // outlined button
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: textPrimary,
          side: const BorderSide(color: primary, width: 1.5),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radiusButton),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        ),
      ),

      // 文本
      textTheme: const TextTheme(
        displayLarge: TextStyle(
            fontSize: 28, fontWeight: FontWeight.bold, color: textPrimary),
        headlineLarge: TextStyle(
            fontSize: 24, fontWeight: FontWeight.bold, color: textPrimary),
        headlineMedium: TextStyle(
            fontSize: 20, fontWeight: FontWeight.w600, color: textPrimary),
        titleLarge: TextStyle(
            fontSize: 18, fontWeight: FontWeight.w600, color: textPrimary),
        titleMedium: TextStyle(
            fontSize: 16, fontWeight: FontWeight.w600, color: textPrimary),
        bodyLarge: TextStyle(
            fontSize: 14, fontWeight: FontWeight.normal, color: textPrimary),
        bodyMedium: TextStyle(
            fontSize: 14, fontWeight: FontWeight.normal, color: textPrimary),
        bodySmall: TextStyle(
            fontSize: 12, fontWeight: FontWeight.normal, color: textSecondary),
        labelLarge: TextStyle(
            fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white),
      ),

      // 导航栏（底部Tab）
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: Colors.white,
        selectedItemColor: primary,
        unselectedItemColor: textSecondary,
        type: BottomNavigationBarType.fixed,
        elevation: 8,
      ),

      // input decoration
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radiusButton),
          borderSide: const BorderSide(color: Colors.grey, width: 1),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radiusButton),
          borderSide: const BorderSide(color: Colors.grey, width: 1),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radiusButton),
          borderSide: const BorderSide(color: primary, width: 2),
        ),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      ),

      // checkbox
      checkboxTheme: CheckboxThemeData(
        fillColor: WidgetStateProperty.all(primary),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
      ),

      // chip
      chipTheme: ChipThemeData(
        backgroundColor: primaryLight,
        selectedColor: primary,
        deleteIconColor: textSecondary,
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radiusTag)),
      ),
    );
  }
}
