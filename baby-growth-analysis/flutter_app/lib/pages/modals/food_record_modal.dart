/// 辅食记录弹窗
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';

class FoodRecordModal extends StatefulWidget {
  const FoodRecordModal({super.key});

  @override
  State<FoodRecordModal> createState() => _FoodRecordModalState();
}

class _FoodRecordModalState extends State<FoodRecordModal> {
  String foodType = 'cereal';
  double? amount;
  final TextEditingController notesCtrl = TextEditingController();
  bool saving = false;

  @override
  void dispose() {
    notesCtrl.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() => saving = true);
    final success = await context.read<AppState>().addFoodRecord(
          type: foodType,
          amount: amount,
          notes: notesCtrl.text.trim(),
        );
    if (context.mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(success ? '辅食记录已保存' : '保存失败')));
    }
    setState(() => saving = false);
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
            Text('记录辅食', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),

            // Food type chips
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children:
                  ['cereal', 'fruit', 'vegetable', 'meat', 'soup'].map((t) {
                final label = {
                  'cereal': '米粉/麦粉',
                  'fruit': '水果泥',
                  'vegetable': '蔬菜泥',
                  'meat': '肉泥',
                  'soup': '米汤/粥'
                }[t]!;
                final selected = foodType == t;
                return FilterChip(
                  label: Text(label),
                  selected: selected,
                  onSelected: (_) => setState(() => foodType = t),
                  selectedColor: AppTheme.foodColor,
                  backgroundColor: Colors.grey.shade100,
                );
              }).toList(),
            ),
            const SizedBox(height: 12),

            TextField(
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                  labelText: '食用量 (g)',
                  prefixIcon:
                      Icon(Icons.restaurant, color: AppTheme.foodColor)),
              onChanged: (v) => amount = double.tryParse(v),
            ),
            const SizedBox(height: 12),
            TextField(
                controller: notesCtrl,
                decoration: const InputDecoration(
                    labelText: '备注（如过敏情况）',
                    prefixIcon: Icon(Icons.note, color: AppTheme.foodColor))),
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
