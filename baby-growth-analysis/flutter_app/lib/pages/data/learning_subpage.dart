/// 学习启蒙子页面
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';

class LearningSubpage extends StatelessWidget {
  const LearningSubpage({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // AI Suggestion button
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(AppTheme.spacingMd),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                  colors: [AppTheme.aiColor, AppTheme.primary]),
              borderRadius: BorderRadius.circular(AppTheme.radiusCard),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.auto_awesome, color: Colors.white, size: 24),
                const SizedBox(width: AppTheme.spacingSm),
                Text('AI 定制亲子活动',
                    style: Theme.of(context).textTheme.labelLarge),
              ],
            ),
          ),
          const SizedBox(height: AppTheme.spacingLg),

          _sectionTitle('学习计划', context),
          if (state.learningPlans.isEmpty)
            Center(
                child: Text('暂无学习计划，试试 AI 推荐吧~',
                    style: Theme.of(context).textTheme.bodySmall))
          else
            ...state.learningPlans
                .take(8)
                .map((plan) => _learningCard(plan, context)),
        ],
      ),
    );
  }

  Widget _sectionTitle(String title, BuildContext ctx) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      child: Text(title, style: Theme.of(ctx).textTheme.titleSmall),
    );
  }

  Widget _learningCard(dynamic plan, BuildContext context) {
    final categoryColors = {
      'visual': AppTheme.secondary,
      'auditory': AppTheme.sleepColor,
      'motor': AppTheme.primary,
      'language': AppTheme.foodColor,
      'social': AppTheme.feedColor,
      'fine_motor': AppTheme.checkupColor,
      'cognitive': AppTheme.aiColor,
      'art': AppTheme.accent,
    };
    final color = categoryColors[plan.category] ?? AppTheme.primary;
    final categoryNames = {
      'visual': '视觉',
      'auditory': '听觉',
      'motor': '大运动',
      'language': '语言',
      'social': '社交',
      'fine_motor': '精细动作',
      'cognitive': '认知',
      'art': '艺术'
    };

    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
                color: color.withValues(alpha: 0.2),
                borderRadius: BorderRadius.circular(12)),
            child: Icon(Icons.lightbulb, color: color, size: 24),
          ),
          const SizedBox(width: AppTheme.spacingMd),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(plan.title,
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 4),
                Row(children: [
                  Chip(
                      backgroundColor: color.withValues(alpha: 0.15),
                      visualDensity: VisualDensity.compact,
                      padding: EdgeInsets.zero,
                      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      label: Text(categoryNames[plan.category] ?? plan.category,
                          style: const TextStyle(fontSize: 10))),
                  const SizedBox(width: 4),
                  if (plan.duration != null)
                    Text(plan.duration!,
                        style: Theme.of(context).textTheme.bodySmall),
                ]),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
