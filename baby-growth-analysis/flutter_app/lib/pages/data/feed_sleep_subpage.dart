/// 喂养睡眠子页面
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';
import '../../services/api_service.dart';
import '../../models/models.dart';

class FeedSleepSubpage extends StatefulWidget {
  const FeedSleepSubpage({super.key});

  @override
  State<FeedSleepSubpage> createState() => _FeedSleepSubpageState();
}

class _FeedSleepSubpageState extends State<FeedSleepSubpage> {
  Map<String, dynamic>? _feedStats;
  Map<String, dynamic>? _sleepStats;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadStats();
  }

  Future<void> _loadStats() async {
    try {
      final api = ApiService();
      final state = context.read<AppState>();
      final today = DateTime.now().toString().split(' ')[0];

      try {
        final feedData =
            await api.getFeedingStats(babyId: state.babyId, date: today);
        setState(() => _feedStats = feedData);
      } catch (_) {}

      // Sleep stats from records
      setState(() {
        int totalMin = 0;
        for (final s in state.sleeps
            .where((rec) => rec.startTime.day == DateTime.now().day)) {
          if (s.endTime != null) {
            totalMin += s.endTime!.difference(s.startTime).inMinutes;
          } else if (s.durationMinutes != null) totalMin += s.durationMinutes!;
        }
        _sleepStats = {
          'total_minutes': totalMin,
          'total_count': state.sleeps
              .where((s) => s.startTime.day == DateTime.now().day)
              .length
        };
      });
    } catch (_) {}
    if (mounted) setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      child: Column(
        children: [
          // Feeding summary
          _summaryCard(
            title: '今日喂养',
            color: AppTheme.feedColor,
            icon: Icons.local_dining,
            items: [
              _statItem('次数',
                  '${_feedStats?['total_count'] ?? state.feedings.where((f) => f.startTime.day == DateTime.now().day).length}'),
              _statItem('总量',
                  '${(_feedStats?['total_ml'] as num?)?.toInt() ?? state.feedings.where((f) => f.startTime.day == DateTime.now().day).fold<double>(0, (sum, f) => sum + (f.amountMl ?? 0)).toInt()}ml'),
            ],
          ),
          const SizedBox(height: AppTheme.spacingMd),

          // Sleep summary
          _summaryCard(
            title: '今日睡眠',
            color: AppTheme.sleepColor,
            icon: Icons.nights_stay,
            items: [
              _statItem('次数', '${_sleepStats?['total_count'] ?? 0}'),
              _statItem('时长', '${_sleepStats?['total_minutes'] ?? 0}分钟'),
            ],
          ),
          const SizedBox(height: AppTheme.spacingLg),

          // Recent feedings
          _sectionTitle('最近喂养记录'),
          ...state.feedings.take(5).map((f) => _feedingRow(f, context)),

          if (state.feedings.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: AppTheme.spacingXl),
              child: Center(
                  child: Text('今天还没有喂养记录哦~',
                      style: Theme.of(context).textTheme.bodySmall)),
            ),

          const SizedBox(height: AppTheme.spacingLg),

          // Recent sleeps
          _sectionTitle('最近睡眠记录'),
          ...state.sleeps.take(5).map((s) => _sleepRow(s, context)),
        ],
      ),
    );
  }

  Widget _summaryCard(
      {required String title,
      required Color color,
      required IconData icon,
      required List<Widget> items}) {
    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            Icon(icon, size: 20, color: color),
            const SizedBox(width: 8),
            Text(title,
                style: TextStyle(
                    fontSize: 16, fontWeight: FontWeight.w600, color: color))
          ]),
          const SizedBox(height: AppTheme.spacingMd),
          Row(children: items),
        ],
      ),
    );
  }

  Widget _statItem(String label, String value) {
    return Expanded(
      child: Column(
        children: [
          Text(value,
              style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.grey[700])),
          Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        ],
      ),
    );
  }

  Widget _sectionTitle(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      child: Text(title, style: Theme.of(context).textTheme.titleSmall),
    );
  }

  Widget _feedingRow(FeedingRecord f, BuildContext context) {
    final typeMap = {'breast_milk': '母乳', 'formula': '配方奶', 'solid_food': '辅食'};
    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
          color: Colors.white, borderRadius: BorderRadius.circular(12)),
      child: Row(
        children: [
          const Icon(Icons.local_dining, color: AppTheme.feedColor, size: 20),
          const SizedBox(width: AppTheme.spacingMd),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                    '${typeMap[f.feedType] ?? f.feedType} ${f.amountMl != null ? '${f.amountMl!.toInt()}ml' : ''}',
                    style: const TextStyle(fontWeight: FontWeight.w500)),
                Text(f.startTime.toString().substring(11, 16),
                    style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _sleepRow(SleepRecord s, BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
          color: Colors.white, borderRadius: BorderRadius.circular(12)),
      child: Row(
        children: [
          const Icon(Icons.nights_stay, color: AppTheme.sleepColor, size: 20),
          const SizedBox(width: AppTheme.spacingMd),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                    '${s.startTime.toString().substring(11, 16)} - ${s.endTime != null ? s.endTime!.toString().substring(11, 16) : '至今'}',
                    style: const TextStyle(fontWeight: FontWeight.w500)),
                Text(
                    '${s.durationMinutes ?? 0}分钟 · ${s.quality == 'good' ? '质量良好' : s.quality == 'poor' ? '睡眠较差' : '一般'}',
                    style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
