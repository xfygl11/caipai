/// 睡眠追踪弹窗
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';

class SleepTrackerModal extends StatefulWidget {
  const SleepTrackerModal({super.key});

  @override
  State<SleepTrackerModal> createState() => _SleepTrackerModalState();
}

class _SleepTrackerModalState extends State<SleepTrackerModal> {
  DateTime? startTime;
  int seconds = 0;
  bool isRunning = false;
  String quality = 'normal';
  final TextEditingController notesController = TextEditingController();

  @override
  void dispose() {
    notesController.dispose();
    super.dispose();
  }

  String get formattedTime {
    final h = seconds ~/ 3600;
    final m = (seconds % 3600) ~/ 60;
    final s = seconds % 60;
    return '${h.toString().padLeft(2, '0')}:${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
  }

  Future<void> toggleTimer() async {
    if (isRunning) {
      setState(() {
        isRunning = false;
        final duration = Duration(seconds: seconds);
        final end = startTime!.add(duration);
        context
            .read<AppState>()
            .addFeeding(type: 'sleep', notes: '$formattedTime · $quality');
        Navigator.pop(context);
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('睡眠记录已保存')));
      });
    } else {
      setState(() {
        isRunning = true;
        startTime = DateTime.now();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * 0.55,
      decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      padding: EdgeInsets.only(
          top: 8,
          left: 16,
          right: 16,
          bottom: MediaQuery.of(context).viewInsets.bottom + 16),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                    color: Colors.grey[300],
                    borderRadius: BorderRadius.circular(2))),
            const SizedBox(height: 12),
            Text('睡眠追踪', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),

            // Timer display
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                  color: AppTheme.sleepColor.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(16)),
              child: Text(formattedTime,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                      fontSize: 40,
                      fontWeight: FontWeight.bold,
                      fontFamily: 'monospace')),
            ),
            const SizedBox(height: 16),

            // Quality selector
            DropdownButtonFormField<String>(
              initialValue: quality,
              decoration: const InputDecoration(
                  labelText: '睡眠质量',
                  prefixIcon: Icon(Icons.sentiment_satisfied_alt,
                      color: AppTheme.sleepColor)),
              items: const [
                DropdownMenuItem(value: 'good', child: Text('好')),
                DropdownMenuItem(value: 'normal', child: Text('一般')),
                DropdownMenuItem(value: 'poor', child: Text('差')),
              ],
              onChanged: (v) => setState(() => quality = v!),
            ),
            const SizedBox(height: 12),

            TextField(
                controller: notesController,
                decoration: const InputDecoration(
                    labelText: '备注',
                    prefixIcon: Icon(Icons.note, color: AppTheme.sleepColor))),
            const SizedBox(height: 16),

            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: toggleTimer,
                    icon: Icon(isRunning ? Icons.pause : Icons.play_arrow),
                    label: Text(isRunning ? '暂停' : '开始'),
                    style: ElevatedButton.styleFrom(
                        backgroundColor:
                            isRunning ? Colors.orange : AppTheme.sleepColor),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            OutlinedButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('取消')),
          ],
        ),
      ),
    );
  }
}
