/// 通用组件：骨架屏、空状态、加载指示器
library;

import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';
import '../theme/app_theme.dart';

class CommonWidgets {
  CommonWidgets._();

  /// 通用骨架屏
  static Widget shimmerCard(
      {double height = 120, double width = double.infinity}) {
    return Shimmer.fromColors(
      baseColor: Colors.grey.shade300,
      highlightColor: Colors.grey.shade100,
      child: Container(
          width: width,
          height: height,
          decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(AppTheme.radiusCard))),
    );
  }

  /// 空数据状态
  static Widget emptyState(
      {String title = '暂无数据', String subtitle = '点击右上角 + 号开始记录'}) {
    return Center(
      child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        Icon(Icons.article_outlined, size: 64, color: Colors.grey.shade400),
        const SizedBox(height: 16),
        Text(title,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
        const SizedBox(height: 8),
        Text(subtitle,
            style: TextStyle(fontSize: 14, color: Colors.grey.shade500),
            textAlign: TextAlign.center),
      ]),
    );
  }

  /// 统一的加载中指示器
  static Widget loadingIndicator({Color? color}) {
    return Center(
        child: CircularProgressIndicator(color: color ?? AppTheme.primary));
  }

  /// 圆角占位容器
  static Widget placeholderBox({double height = 80, Color? bgColor}) {
    return Container(
      height: height,
      decoration: BoxDecoration(
          color: bgColor ?? Colors.grey.shade100,
          borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
      child: const Center(child: Icon(Icons.add, size: 32, color: Colors.grey)),
    );
  }
}
