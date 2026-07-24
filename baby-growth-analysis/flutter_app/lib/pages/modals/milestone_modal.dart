import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';

class MilestoneModal extends StatefulWidget {
  const MilestoneModal({super.key});

  @override
  State<MilestoneModal> createState() => _MilestoneModalState();
}

class _MilestoneModalState extends State<MilestoneModal> {
  final TextEditingController titleCtrl = TextEditingController();
  final TextEditingController descCtrl = TextEditingController();
  String milestoneType = 'first_smile';
  bool saving = false;

  final List<Map<String, dynamic>> types = [
    {'value': 'first_smile', 'label': '第一次笑', 'icon': Icons.emoji_emotions},
    {
      'value': 'first_roll',
      'label': '第一次翻身',
      'icon': Icons.rotate_90_degrees_cw
    },
    {'value': 'first_sit', 'label': '第一次独坐', 'icon': Icons.accessibility},
    {'value': 'first_crawl', 'label': '第一次爬行', 'icon': Icons.directions_run},
    {'value': 'first_stand', 'label': '第一次站立', 'icon': Icons.child_care},
    {'value': 'first_walk', 'label': '第一次走路', 'icon': Icons.directions_walk},
    {'value': 'first_word', 'label': '第一次说话', 'icon': Icons.record_voice_over},
    {'value': 'first_dad', 'label': '第一次叫爸爸', 'icon': Icons.person},
    {'value': 'first_mom', 'label': '第一次叫妈妈', 'icon': Icons.pregnant_woman},
    {'value': 'first_tooth', 'label': '第一颗牙', 'icon': Icons.movie},
    {'value': 'first_solid', 'label': '第一次吃辅食', 'icon': Icons.restaurant},
    {'value': 'first_birthday', 'label': '第一个生日', 'icon': Icons.cake},
  ];

  @override
  void dispose() {
    titleCtrl.dispose();
    descCtrl.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() => saving = true);
    final success = await context.read<AppState>().addMilestone(
          type: milestoneType,
          title: titleCtrl.text.trim(),
          description: descCtrl.text.trim(),
        );
    if (context.mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(success ? '里程碑已保存' : '保存失败')));
    }
    setState(() => saving = false);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * 0.7,
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
            Text('记录里程碑', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  crossAxisSpacing: 8,
                  mainAxisSpacing: 8,
                  childAspectRatio: 1.2),
              itemCount: types.length,
              itemBuilder: (ctx, i) {
                final t = types[i];
                final selected = milestoneType == t['value'];
                return FilterChip(
                  label: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(t['icon'], size: 16),
                    const SizedBox(width: 4),
                    Text(t['label'])
                  ]),
                  selected: selected,
                  onSelected: (_) => setState(() => milestoneType = t['value']),
                  selectedColor: AppTheme.accent,
                  backgroundColor: Colors.grey.shade100,
                );
              },
            ),
            const SizedBox(height: 12),
            TextField(
                controller: titleCtrl,
                decoration: const InputDecoration(
                    labelText: '标题',
                    prefixIcon: Icon(Icons.edit, color: AppTheme.accent))),
            const SizedBox(height: 12),
            TextField(
                controller: descCtrl,
                maxLines: 3,
                decoration: const InputDecoration(
                    labelText: '描述',
                    prefixIcon: Icon(Icons.note, color: AppTheme.accent))),
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
