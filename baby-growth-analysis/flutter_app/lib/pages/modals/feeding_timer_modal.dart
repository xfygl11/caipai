/// 喂养计时器弹窗
library;

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';

class FeedingTimerModal extends StatefulWidget {
  const FeedingTimerModal({super.key});

  @override
  State<FeedingTimerModal> createState() => _FeedingTimerModalState();
}

class _FeedingTimerModalState extends State<FeedingTimerModal> {
  String feedType = 'breast_milk';
  double? amount;
  DateTime? startTime;
  int seconds = 0;
  Timer? timer;
  bool isRunning = false;

  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  String get formattedTime {
    final mins = seconds ~/ 60;
    final secs = seconds % 60;
    return '${mins.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}';
  }

  void toggleTimer() {
    if (isRunning) {
      timer?.cancel();
      setState(() {
        isRunning = false;
        startTime = null;
      });
    } else {
      setState(() {
        isRunning = true;
        startTime = DateTime.now();
      });
      timer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (mounted) setState(() => seconds++);
      });
    }
  }

  Future<void> save() async {
    final success = await context.read<AppState>().addFeeding(
          type: feedType,
          amount: amount,
          notes: '时长 $formattedTime',
        );
    if (context.mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(success ? '喂养记录已保存' : '保存失败')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * 0.6,
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
            Text('喂养计时器', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),

            // Type selector
            Row(
              children: [
                Expanded(child: _feedTypeChip('母乳喂养', 'breast_milk')),
                const SizedBox(width: 8),
                Expanded(child: _feedTypeChip('配方奶', 'formula')),
                const SizedBox(width: 8),
                Expanded(child: _feedTypeChip('辅食', 'solid_food')),
              ],
            ),
            const SizedBox(height: 16),

            // Amount input
            TextField(
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                  labelText: '奶量 (ml)',
                  prefixIcon: Icon(Icons.local_drink,
                      size: 20, color: AppTheme.feedColor)),
              onChanged: (v) => amount = double.tryParse(v),
            ),
            const SizedBox(height: 16),

            // Timer display
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                  color: AppTheme.feedColor.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(16)),
              child: Column(
                children: [
                  Text(formattedTime,
                      style: const TextStyle(
                          fontSize: 36,
                          fontWeight: FontWeight.bold,
                          fontFamily: 'monospace')),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      ElevatedButton.icon(
                        onPressed: toggleTimer,
                        icon: Icon(isRunning ? Icons.pause : Icons.play_arrow),
                        label: Text(isRunning ? '暂停' : '开始'),
                        style: ElevatedButton.styleFrom(
                            backgroundColor:
                                isRunning ? Colors.orange : AppTheme.primary),
                      ),
                      const SizedBox(width: 12),
                      ElevatedButton.icon(
                        onPressed: isRunning
                            ? null
                            : (startTime != null ? save : null),
                        icon: const Icon(Icons.check),
                        label: const Text('保存'),
                        style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.green),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            OutlinedButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('取消')),
          ],
        ),
      ),
    );
  }

  Widget _feedTypeChip(String label, String value) {
    final selected = feedType == value;
    return ChoiceChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) => setState(() => feedType = value),
      selectedColor: AppTheme.feedColor,
      backgroundColor: Colors.grey.shade100,
    );
  }
}
