/// 疫苗子页面
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';
import '../../services/api_service.dart';
import '../../models/models.dart';

class VaccineSubpage extends StatelessWidget {
  const VaccineSubpage({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final vaccines = state.vaccines;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Summary
          Container(
            padding: const EdgeInsets.all(AppTheme.spacingMd),
            decoration: BoxDecoration(
                color: AppTheme.vaccineColor.withValues(alpha: 0.2),
                borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _summaryStat('总数', '${vaccines.length}', AppTheme.vaccineColor),
                _summaryStat(
                    '已接种',
                    '${vaccines.where((v) => v.status == 'done').length}',
                    Colors.green),
                _summaryStat(
                    '待接种',
                    '${vaccines.where((v) => v.status == 'pending').length}',
                    AppTheme.warning),
              ],
            ),
          ),
          const SizedBox(height: AppTheme.spacingLg),

          _sectionTitle('疫苗接种记录', context),
          if (vaccines.isEmpty)
            Center(
                child: Text('暂无疫苗记录',
                    style: Theme.of(context).textTheme.bodySmall))
          else
            ...vaccines.map((v) => _vaccineCard(v, context)),
        ],
      ),
    );
  }

  Widget _summaryStat(String label, String value, Color color) {
    return Column(
      children: [
        Text(value,
            style: TextStyle(
                fontSize: 24, fontWeight: FontWeight.bold, color: color)),
        Text(label, style: TextStyle(fontSize: 12, color: color)),
      ],
    );
  }

  Widget _sectionTitle(String title, BuildContext ctx) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      child: Text(title, style: Theme.of(ctx).textTheme.titleSmall),
    );
  }

  Widget _vaccineCard(Vaccine v, BuildContext context) {
    final done = v.status == 'done';
    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
        border: Border.all(
            color: done
                ? Colors.green.withValues(alpha: 0.3)
                : AppTheme.warning.withValues(alpha: 0.3)),
      ),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: done
                  ? Colors.green.withValues(alpha: 0.15)
                  : AppTheme.vaccineColor.withValues(alpha: 0.3),
              shape: BoxShape.circle,
            ),
            child: Icon(done ? Icons.check_circle : Icons.schedule,
                color: done ? Colors.green : AppTheme.vaccineColor, size: 20),
          ),
          const SizedBox(width: AppTheme.spacingMd),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(v.vaccineName,
                        style: const TextStyle(fontWeight: FontWeight.w600)),
                    const SizedBox(width: 8),
                    Chip(
                      label: Text(done ? '已完成' : '待接种',
                          style: const TextStyle(fontSize: 10)),
                      backgroundColor: done
                          ? Colors.green.withValues(alpha: 0.15)
                          : AppTheme.warning.withValues(alpha: 0.15),
                      visualDensity: VisualDensity.compact,
                    ),
                  ],
                ),
                Text(
                    '第${v.doseNumber}剂 · ${v.dueDate != null ? '${v.dueDate!.month}/${v.dueDate!.day}' : '时间待定'}',
                    style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
          if (!done)
            IconButton(
              icon: const Icon(Icons.check, color: Colors.green),
              onPressed: () => _markDone(context, v),
            ),
        ],
      ),
    );
  }

  void _markDone(BuildContext context, Vaccine vaccine) async {
    try {
      final apiService = context.read<ApiService>();
      await apiService.markVaccineDone(vaccine.id,
          actualDate: DateTime.now().date);
      if (context.mounted) {
        context.read<AppState>().loadVaccines();
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('已标记为完成')));
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('操作失败')));
      }
    }
  }
}

extension on DateTime {
  DateTime get date => DateTime(year, month, day);
}
