/// 时间轴页面 - 混合日志流（日记+里程碑+照片）
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../providers/app_state.dart';
import '../models/models.dart';

class TimelinePage extends StatelessWidget {
  const TimelinePage({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    // 合并所有事件按时间排序
    final events = <TimelineEvent>[];

    for (final log in state.dailyLogs) {
      if (log.logDate != null) {
        events.add(TimelineEvent(date: log.logDate!, type: 'log', data: log));
      }
    }

    for (final ms in state.milestones) {
      if (ms.happenedAt != null) {
        events.add(TimelineEvent(
            date: ms.happenedAt!.date, type: 'milestone', data: ms));
      }
    }

    events.sort((a, b) => b.date.compareTo(a.date));

    // Group by date
    final grouped = <String, List<TimelineEvent>>{};
    for (final e in events) {
      final key =
          '${e.date.year}-${e.date.month.toString().padLeft(2, '0')}-${e.date.day.toString().padLeft(2, '0')}';
      grouped.putIfAbsent(key, () => []).add(e);
    }

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(AppTheme.spacingLg),
              child: Row(
                children: [
                  Text('时间轴',
                      style: Theme.of(context).textTheme.headlineMedium),
                  const Spacer(),
                  Chip(
                    label: Text('${events.length} 条',
                        style: const TextStyle(fontSize: 12)),
                    backgroundColor: AppTheme.primaryLight,
                  ),
                ],
              ),
            ),
            if (events.isEmpty)
              Expanded(
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.schedule,
                          size: 64,
                          color: AppTheme.textSecondary.withValues(alpha: 0.3)),
                      const SizedBox(height: AppTheme.spacingMd),
                      Text('还没有记录，开始记录宝宝的成长吧！',
                          style: Theme.of(context).textTheme.bodySmall),
                    ],
                  ),
                ),
              )
            else
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.symmetric(
                      horizontal: AppTheme.spacingLg),
                  children: grouped.entries.map((entry) {
                    return _dateGroup(entry.key, entry.value, context);
                  }).toList(),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _dateGroup(
      String dateKey, List<TimelineEvent> events, BuildContext context) {
    final parts = dateKey.split('-');
    final displayDate =
        '${parts[0]}年${int.parse(parts[1])}月${int.parse(parts[2])}日';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
          child:
              Text(displayDate, style: Theme.of(context).textTheme.titleSmall),
        ),
        ...events.map((e) => _eventCard(e, context)),
        const SizedBox(height: AppTheme.spacingLg),
      ],
    );
  }

  Widget _eventCard(TimelineEvent event, BuildContext context) {
    IconData icon;
    Color color;
    String title;
    String? subtitle;

    switch (event.type) {
      case 'log':
        final log = event.data as DailyLog;
        icon = log.aiGenerated ? Icons.auto_awesome : Icons.note;
        color = log.aiGenerated ? AppTheme.aiColor : AppTheme.primary;
        title = log.title ?? '未命名日记';
        subtitle = log.content?.substring(0, log.content!.length.clamp(0, 50));
        break;
      case 'milestone':
        final ms = event.data as Milestone;
        icon = Icons.star;
        color = AppTheme.accent;
        title = ms.title;
        subtitle = ms.description;
        break;
      default:
        icon = Icons.circle;
        color = AppTheme.textSecondary;
        title = '';
        subtitle = '';
    }

    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
        border: Border.all(color: color.withValues(alpha: 0.2)),
      ),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
                color: color.withValues(alpha: 0.15), shape: BoxShape.circle),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: AppTheme.spacingMd),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.titleSmall),
                if (subtitle != null && subtitle.isNotEmpty)
                  Text(subtitle,
                      style: Theme.of(context).textTheme.bodySmall,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class TimelineEvent {
  final DateTime date;
  final String type;
  final dynamic data;

  TimelineEvent({required this.date, required this.type, required this.data});
}

extension on DateTime {
  DateTime get date => DateTime(year, month, day);
}
