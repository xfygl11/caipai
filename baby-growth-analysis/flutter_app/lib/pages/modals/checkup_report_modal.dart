/// 体检报告上传弹窗
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';
import '../../services/camera_service.dart';
import '../../services/ai_service.dart';

class CheckupReportModal extends StatefulWidget {
  const CheckupReportModal({super.key});

  @override
  State<CheckupReportModal> createState() => _CheckupReportModalState();
}

class _CheckupReportModalState extends State<CheckupReportModal> {
  String? date;
  String? weight;
  String? height;
  String? headCircumference;
  String? notes;
  bool saving = false;
  bool loadingAi = false;
  String? aiResult;

  final CameraService _camera = CameraService();

  Future<void> _pickOrCapturePhoto() async {
    final imageFile = await _camera.takePhoto();
    if (imageFile != null && mounted) {
      setState(() {}); // trigger rebuild to show preview
    }
  }

  Future<void> _analyzeWithAi() async {
    setState(() => loadingAi = true);
    try {
      final result = await context
          .read<AiService>()
          .recognizeCheckupReport(imageBase64: '');
      if (mounted) {
        setState(() {
          aiResult = result['answer'] ?? result['content'] ?? '';
          loadingAi = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => loadingAi = false);
    }
  }

  Future<void> save() async {
    setState(() => saving = true);
    final success = await context.read<AppState>().addCheckupReport(
          date: DateTime.tryParse(date ?? '') ?? DateTime.now(),
          weight: double.tryParse(weight ?? '')?.toString(),
          height: height,
          headCircumference: headCircumference,
          notes: notes,
          aiInterpretation: aiResult,
        );
    if (context.mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(success ? '体检记录已保存' : '保存失败')));
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
            Text('体检报告', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            TextField(
              decoration: const InputDecoration(
                  labelText: '检查日期',
                  prefixIcon:
                      Icon(Icons.calendar_today, color: AppTheme.checkupColor)),
              readOnly: true,
              onTap: () async {
                final picked = await showDatePicker(
                  context: context,
                  initialDate: DateTime.now(),
                  firstDate: DateTime(2020),
                  lastDate: DateTime.now(),
                );
                if (picked != null)
                  setState(() => date = picked.toString().split(' ')[0]);
              },
            ),
            const SizedBox(height: 12),
            TextField(
                decoration: const InputDecoration(
                    labelText: '体重 (kg)',
                    prefixIcon:
                        Icon(Icons.trending_up, color: AppTheme.checkupColor)),
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                onChanged: (v) => weight = v),
            const SizedBox(height: 12),
            TextField(
                decoration: const InputDecoration(
                    labelText: '身高 (cm)',
                    prefixIcon:
                        Icon(Icons.straighten, color: AppTheme.checkupColor)),
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                onChanged: (v) => height = v),
            const SizedBox(height: 12),
            TextField(
                decoration: const InputDecoration(
                    labelText: '头围 (cm)',
                    prefixIcon: Icon(Icons.circle_outlined,
                        color: AppTheme.checkupColor)),
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                onChanged: (v) => headCircumference = v),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                    child: ElevatedButton.icon(
                        onPressed: _pickOrCapturePhoto,
                        icon: const Icon(Icons.camera_alt, size: 18),
                        label: const Text('拍照'),
                        style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.checkupColor))),
                const SizedBox(width: 12),
                Expanded(
                    child: ElevatedButton.icon(
                        onPressed: loadingAi ? null : _analyzeWithAi,
                        icon: loadingAi
                            ? const SizedBox(
                                width: 16,
                                height: 16,
                                child:
                                    CircularProgressIndicator(strokeWidth: 2))
                            : const Icon(Icons.auto_awesome, size: 18),
                        label: Text(loadingAi ? '识别中' : 'AI 提取'))),
              ],
            ),
            const SizedBox(height: 12),
            if (aiResult != null) ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                    color: AppTheme.aiColor.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(12)),
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('AI 解析结果:',
                          style: TextStyle(fontWeight: FontWeight.w600)),
                      const SizedBox(height: 4),
                      Text(aiResult!, style: const TextStyle(fontSize: 13)),
                    ]),
              ),
              const SizedBox(height: 12),
            ],
            TextField(
                controller: TextEditingController(text: notes),
                maxLines: 3,
                decoration: const InputDecoration(
                    labelText: '医生备注',
                    prefixIcon:
                        Icon(Icons.note, color: AppTheme.checkupColor))),
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
