/// 体检报告子页面
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';

class CheckupSubpage extends StatelessWidget {
  const CheckupSubpage({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (state.checkups.length >= 2) ...[
            _trendCard(state.checkups, context),
            const SizedBox(height: AppTheme.spacingLg),
          ],
          _sectionTitle(context, '体检记录'),
          if (state.checkups.isEmpty)
            Center(
                child: Text('暂无体检记录',
                    style: Theme.of(context).textTheme.bodySmall))
          else
            ...state.checkups.map((c) => _checkupCard(c, context)),
        ],
      ),
    );
  }

  Widget _trendCard(List<dynamic> checkups, BuildContext context) {
    if (checkups.length < 2) return const SizedBox();
    final first = checkups.last;
    final last = checkups.first;

    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
          color: AppTheme.secondary.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('成长趋势（首次 vs 最新）', style: Theme.of(context).textTheme.titleSmall),
          const SizedBox(height: AppTheme.spacingSm),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _trendItem('体重', first.weight, last.weight, 'kg', context),
              _trendItem('身高', first.height, last.height, 'cm', context),
              _trendItem('头围', first.headCircumference, last.headCircumference,
                  'cm', context),
            ],
          ),
        ],
      ),
    );
  }

  Widget _trendItem(String label, dynamic firstVal, dynamic lastVal,
      String unit, BuildContext context) {
    final diff = ((lastVal as num?)?.toDouble() ?? 0) -
        ((firstVal as num?)?.toDouble() ?? 0);
    return Column(
      children: [
        Text(label, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 4),
        Text(
            '${lastVal?.toStringAsFixed(1) ?? '--'}${diff > 0 ? ' ↗+${diff.toStringAsFixed(1)}' : ''}',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        Text(unit, style: Theme.of(context).textTheme.bodySmall),
      ],
    );
  }

  Widget _sectionTitle(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingMd),
      child: Text(title,
          style: Theme.of(context).textTheme.titleLarge!.copyWith(
              color: AppTheme.textPrimary, fontWeight: FontWeight.w700)),
    );
  }

  Widget _checkupCard(dynamic c, BuildContext context) {
    final typeNames = {
      'newborn': '新生儿',
      '1month': '满月',
      '3month': '3月',
      '6month': '6月',
      '9month': '9月',
      '1year': '1岁',
      'annual': '年度'
    };

    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.medical_services,
                  color: AppTheme.checkupColor, size: 20),
              const SizedBox(width: 8),
              Expanded(
                  child: Text(
                      '${typeNames[c.checkupType] ?? c.checkupType}体检 · ${c.checkupDate.toString().substring(0, 10)}',
                      style: const TextStyle(fontWeight: FontWeight.w600))),
              if (c.aiAnalysis != null)
                Chip(
                  backgroundColor: AppTheme.aiColor.withValues(alpha: 0.15),
                  visualDensity: VisualDensity.compact,
                  padding: EdgeInsets.zero,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  label: const Text('AI已解读', style: TextStyle(fontSize: 10)),
                ),
            ],
          ),
          const SizedBox(height: AppTheme.spacingSm),
          Wrap(
            spacing: 12,
            runSpacing: 4,
            children: [
              if (c.weight != null) Text('体重: ${c.weight}kg'),
              if (c.height != null) Text('身高: ${c.height}cm'),
              if (c.headCircumference != null)
                Text('头围: ${c.headCircumference}cm'),
              if (c.teethCount != null) Text('牙齿: ${c.teethCount}颗'),
            ],
          ),
          if (c.doctorAdvice != null && c.doctorAdvice!.isNotEmpty) ...[
            const SizedBox(height: AppTheme.spacingSm),
            Text('医生建议：${c.doctorAdvice}',
                style: const TextStyle(fontSize: 12, color: AppTheme.warning)),
          ],
        ],
      ),
    );
  }
}
