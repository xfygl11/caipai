/// 生长记录弹窗
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';

class GrowthRecordModal extends StatefulWidget {
  const GrowthRecordModal({super.key});

  @override
  State<GrowthRecordModal> createState() => _GrowthRecordModalState();
}

class _GrowthRecordModalState extends State<GrowthRecordModal> {
  final weightCtrl = TextEditingController();
  final heightCtrl = TextEditingController();
  final headCtrl = TextEditingController();
  bool saving = false;

  @override
  void dispose() {
    weightCtrl.dispose();
    heightCtrl.dispose();
    headCtrl.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() => saving = true);
    final success = await context.read<AppState>().addGrowthRecord(
          weight: double.tryParse(weightCtrl.text),
          height: double.tryParse(heightCtrl.text),
          headCirc: double.tryParse(headCtrl.text),
        );
    if (context.mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(success ? '记录已保存' : '保存失败')));
    }
    setState(() => saving = false);
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
            Text('记录生长数据', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            TextField(
              controller: weightCtrl,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                  labelText: '体重 (kg)',
                  prefixIcon: Icon(Icons.trending_up, color: AppTheme.primary)),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: heightCtrl,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                  labelText: '身高 (cm)',
                  prefixIcon: Icon(Icons.straighten, color: AppTheme.primary)),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: headCtrl,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                  labelText: '头围 (cm)',
                  prefixIcon:
                      Icon(Icons.circle_outlined, color: AppTheme.primary)),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                    child: OutlinedButton(
                        onPressed: () => Navigator.pop(context),
                        child: const Text('取消'))),
                const SizedBox(width: 12),
                Expanded(
                    child: ElevatedButton(
                        onPressed: saving ? null : save,
                        child: saving
                            ? const CircularProgressIndicator()
                            : const Text('保存'))),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
