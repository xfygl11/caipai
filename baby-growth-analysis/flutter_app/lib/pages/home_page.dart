/// 首页 - 今日概览 + 快捷操作
library;

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../providers/app_state.dart';
import '../models/models.dart';
import '../services/ai_service.dart';
import '../config/app_config.dart';
import './modals/feeding_timer_modal.dart';
import './modals/sleep_tracker_modal.dart';
import './modals/growth_record_modal.dart';
import './modals/milestone_modal.dart';
import './modals/food_record_modal.dart';
import './modals/checkup_modal.dart';
import './modals/photo_upload_modal.dart';
import './modals/ai_auto_record_modal.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  DateTime now = DateTime.now();

  @override
  void initState() {
    super.initState();
    _updateAge();
    Timer.periodic(const Duration(seconds: 1), (_) => _updateAge());
  }

  void _updateAge() {
    if (!mounted) return;
    setState(() => now = DateTime.now());
  }

  String _calculateAge() {
    final birthTime = DateTime.parse(AppConfig.babyBirthTime);
    final diff = now.difference(birthTime);
    final days = diff.inDays;
    final hours = diff.inHours % 24;
    final mins = diff.inMinutes % 60;
    final secs = diff.inSeconds % 60;
    return '出生第 $days 天 $hours时$mins分$secs秒';
  }

  @override
  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final baby = state.baby;
    final babyName = baby?.name ?? AppConfig.babyName;

    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async => state.init(),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(AppTheme.spacingLg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    Container(
                      width: 56,
                      height: 56,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: LinearGradient(
                          colors: [AppTheme.primary, AppTheme.accent],
                        ),
                      ),
                      child: const Icon(Icons.child_care,
                          color: Colors.white, size: 32),
                    ),
                    const SizedBox(width: AppTheme.spacingMd),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(babyName,
                              style:
                                  Theme.of(context).textTheme.headlineMedium),
                          Text(_calculateAge(),
                              style: Theme.of(context).textTheme.bodySmall),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.settings_outlined),
                      onPressed: () =>
                          Navigator.pushNamed(context, '/settings'),
                    ),
                  ],
                ),
                const SizedBox(height: AppTheme.spacingXl),

                // Today Overview Cards
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    Expanded(
                      child: _overviewCard(
                        icon: Icons.local_dining,
                        color: AppTheme.feedColor,
                        value:
                            '${state.feedings.where((f) => f.startTime.day == now.day).length}',
                        label: '喂养(次)',
                      ),
                    ),
                    const SizedBox(width: AppTheme.spacingSm),
                    Expanded(
                      child: _overviewCard(
                        icon: Icons.nightlight_round,
                        color: AppTheme.sleepColor,
                        value: '${_totalSleepMinutes(state)}分钟',
                        label: '睡眠时长',
                      ),
                    ),
                    const SizedBox(width: AppTheme.spacingSm),
                    Expanded(
                      child: _overviewCard(
                        icon: Icons.star,
                        color: AppTheme.secondary,
                        value: '${state.milestones.length}',
                        label: '里程碑',
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppTheme.spacingMd),

                // AI Reminders Card
                Consumer<AiService>(
                    builder: (_, __, ___) => _buildRemindersCard(context)),

                // Quick Actions Grid
                _buildQuickActions(context),
                const SizedBox(height: AppTheme.spacingMd),

                // Latest Diaries
                if (state.dailyLogs.isNotEmpty) ...[
                  _sectionHeader('最近日记'),
                  ...state.dailyLogs
                      .take(3)
                      .map((log) => _diaryCard(log, context)),
                  const SizedBox(height: AppTheme.spacingMd),
                ],

                // Latest Growth
                if (state.growthRecords.isNotEmpty) ...[
                  _sectionHeader('最新生长数据'),
                  _growthSummaryCard(state.growthRecords.first),
                  const SizedBox(height: AppTheme.spacingMd),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _overviewCard({
    required IconData icon,
    required Color color,
    required String value,
    required String label,
  }) {
    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
          color: color.withValues(alpha: 0.3),
          borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          Icon(icon, color: color, size: 28),
          const SizedBox(height: 4),
          Text(value,
              style: TextStyle(
                  fontSize: 18, fontWeight: FontWeight.bold, color: color)),
          Text(label,
              style:
                  const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
        ],
      ),
    );
  }

  int _totalSleepMinutes(AppState state) {
    int total = 0;
    for (final s in state.sleeps
        .where((rec) => rec.startTime.day == DateTime.now().day)) {
      if (s.endTime != null) {
        total += s.endTime!.difference(s.startTime).inMinutes;
      } else if (s.durationMinutes != null) {
        total += s.durationMinutes!;
      }
    }
    return total;
  }

  Widget _buildRemindersCard(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      decoration: BoxDecoration(
        color: AppTheme.aiColor.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
        border: Border.all(color: AppTheme.aiColor.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: const BoxDecoration(
                color: AppTheme.aiColor, shape: BoxShape.circle),
            child:
                const Icon(Icons.auto_awesome, color: Colors.white, size: 20),
          ),
          const SizedBox(width: AppTheme.spacingMd),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                Text('AI 智能提醒', style: Theme.of(context).textTheme.titleSmall),
                const SizedBox(height: 2),
                Text('距离上次喂奶已过一段时间，准备下一次喂养吧~',
                    style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActions(BuildContext context) {
    final actions = [
      {
        'icon': 'record_voice',
        'label': '语音记录',
        'color': AppTheme.aiColor,
        'action': '_openVoiceRecord'
      },
      {
        'icon': 'camera_alt',
        'label': '拍照识别',
        'color': AppTheme.secondary,
        'action': '_openPhotoUpload'
      },
      {
        'icon': 'local_dining',
        'label': '喂奶',
        'color': AppTheme.feedColor,
        'action': '_openFeedingTimer'
      },
      {
        'icon': 'nights_stay',
        'label': '睡眠',
        'color': AppTheme.sleepColor,
        'action': '_openSleepTracker'
      },
      {
        'icon': 'trending_up',
        'label': '生长',
        'color': AppTheme.primary,
        'action': '_openGrowthRecord'
      },
      {
        'icon': 'star',
        'label': '里程碑',
        'color': AppTheme.accent,
        'action': '_openMilestone'
      },
      {
        'icon': 'restaurant',
        'label': '辅食',
        'color': AppTheme.foodColor,
        'action': '_openFoodRecord'
      },
      {
        'icon': 'medical_services',
        'label': '体检',
        'color': AppTheme.checkupColor,
        'action': '_openCheckup'
      },
    ];

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 4,
        crossAxisSpacing: AppTheme.spacingMd,
        mainAxisSpacing: AppTheme.spacingMd,
        childAspectRatio: 0.85,
      ),
      itemCount: actions.length,
      itemBuilder: (context, index) {
        final action = actions[index];
        return _quickActionTile(
          icon: action['icon'] as String,
          label: action['label'] as String,
          color: action['color'] as Color,
          onTap: () {
            switch (action['action']) {
              case '_openVoiceRecord':
                _openVoiceRecord(context);
                break;
              case '_openPhotoUpload':
                _openPhotoUpload(context);
                break;
              case '_openFeedingTimer':
                _openFeedingTimer(context);
                break;
              case '_openSleepTracker':
                _openSleepTracker(context);
                break;
              case '_openGrowthRecord':
                _openGrowthRecord(context);
                break;
              case '_openMilestone':
                _openMilestone(context);
                break;
              case '_openFoodRecord':
                _openFoodRecord(context);
                break;
              case '_openCheckup':
                _openCheckup(context);
                break;
            }
          },
        );
      },
    );
  }

  Widget _quickActionTile(
      {required String icon,
      required String label,
      required Color color,
      required VoidCallback onTap}) {
    final iconMap = {
      'record_voice': Icons.mic,
      'camera_alt': Icons.camera_alt,
      'local_dining': Icons.local_dining,
      'nights_stay': Icons.nights_stay,
      'trending_up': Icons.trending_up,
      'star': Icons.star,
      'restaurant': Icons.restaurant,
      'medical_services': Icons.medical_services,
    };

    return Material(
      color: color.withValues(alpha: 0.3),
      borderRadius: BorderRadius.circular(AppTheme.radiusCard),
      child: InkWell(
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(iconMap[icon] ?? Icons.circle, color: color, size: 28),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(fontSize: 12, color: color)),
          ],
        ),
      ),
    );
  }

  Widget _sectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      child: Text(title, style: Theme.of(context).textTheme.titleMedium),
    );
  }

  Widget _diaryCard(DailyLog log, BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              if (log.aiGenerated) ...[
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                      color: AppTheme.aiColor.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8)),
                  child: const Text('AI',
                      style: TextStyle(fontSize: 10, color: AppTheme.aiColor)),
                ),
                const SizedBox(width: AppTheme.spacingSm),
              ],
              Expanded(
                  child: Text(log.title ?? '',
                      style: Theme.of(context).textTheme.titleSmall)),
              Text(
                  log.logDate != null
                      ? '${log.logDate!.month}/${log.logDate!.day}'
                      : '',
                  style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
          if (log.content != null && log.content!.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: AppTheme.spacingSm),
              child: Text(
                log.content!.length > 100
                    ? '${log.content!.substring(0, 100)}...'
                    : log.content!,
                style: Theme.of(context).textTheme.bodyMedium,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
        ],
      ),
    );
  }

  Widget _growthSummaryCard(GrowthRecord record) {
    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _growthItem('体重', record.weight?.toStringAsFixed(2) ?? '--', 'kg'),
          _growthItem('身高', record.height?.toStringAsFixed(1) ?? '--', 'cm'),
          _growthItem(
              '头围', record.headCircumference?.toStringAsFixed(1) ?? '--', 'cm'),
        ],
      ),
    );
  }

  Widget _growthItem(String label, String value, String unit) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        Text(value, style: Theme.of(context).textTheme.titleLarge),
        Text('$label ($unit)', style: Theme.of(context).textTheme.bodySmall),
      ],
    );
  }

  // ========== 打开各弹窗 ==========

  void _openVoiceRecord(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const AiAutoRecordModal(),
    );
  }

  void _openPhotoUpload(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const PhotoUploadModal(),
    );
  }

  void _openFeedingTimer(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const FeedingTimerModal(),
    );
  }

  void _openSleepTracker(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const SleepTrackerModal(),
    );
  }

  void _openGrowthRecord(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const GrowthRecordModal(),
    );
  }

  void _openMilestone(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const MilestoneModal(),
    );
  }

  void _openFoodRecord(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const FoodRecordModal(),
    );
  }

  void _openCheckup(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => const CheckupReportModal(),
    );
  }
}
