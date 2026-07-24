/// 模态框 - 快速记录选择（语音/拍照/手动）
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:convert';
import 'package:image_picker/image_picker.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';
import '../../services/ai_service.dart';
import '../../services/voice_service.dart';
import '../modals/feeding_timer_modal.dart';
import '../modals/sleep_tracker_modal.dart';
import '../modals/growth_record_modal.dart';
import '../modals/milestone_modal.dart';
import '../modals/food_record_modal.dart';
import '../modals/checkup_report_modal.dart';

class AiAutoRecordModal extends StatefulWidget {
  const AiAutoRecordModal({super.key});

  @override
  State<AiAutoRecordModal> createState() => _AiAutoRecordModalState();
}

class _AiAutoRecordModalState extends State<AiAutoRecordModal>
    with SingleTickerProviderStateMixin {
  final VoiceRecordService _voiceService = VoiceRecordService();
  bool _isRecording = false;
  String? _voiceText;
  VoiceParseResult? _parsedResult;
  bool _parsing = false;
  bool _analyzingImage = false;
  String? _imageBase64;

  @override
  void dispose() {
    _voiceService.dispose();
    super.dispose();
  }

  Future<void> _toggleRecording() async {
    if (_isRecording) {
      await _voiceService.stopRecording();
      setState(() => _isRecording = false);
    } else {
      final granted = await _voiceService.checkPermission();
      if (!granted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('需要麦克风权限')));
        return;
      }
      await _voiceService.startRecording();
      setState(() => _isRecording = true);
    }
  }

  Future<void> _analyzeVoice() async {
    if (_voiceText == null || _voiceText!.isEmpty) return;
    setState(() => _parsing = true);

    final result =
        await context.read<AiService>().parseVoiceCommand(_voiceText!);
    if (mounted) {
      setState(() {
        _parsedResult = result;
        _parsing = false;
      });

      // Auto-create record if confidence is high enough
      if (result.confidence > 0.7 && result.type != 'unknown') {
        await context.read<AppState>().autoCreateFromVoice(result);
        if (mounted) Navigator.pop(context);
      }
    }
  }

  Future<void> _takePhotoForAnalysis() async {
    setState(() => _analyzingImage = true);
    try {
      final picker = ImagePicker();
      final image = await picker.pickImage(source: ImageSource.camera);
      if (image != null) {
        final bytes = await image.readAsBytes();
        setState(() => _imageBase64 = base64Encode(bytes));
      }
    } catch (_) {}
    setState(() => _analyzingImage = false);
  }

  Future<void> _analyzeImage() async {
    if (_imageBase64 == null) return;
    setState(() => _analyzingImage = true);

    final result = await context
        .read<AiService>()
        .recognizeCheckupReport(imageBase64: _imageBase64!);
    if (mounted) {
      setState(() => _analyzingImage = false);
      _showImageAnalysisResult(result);
    }
  }

  void _showImageAnalysisResult(Map<String, dynamic> result) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('识别结果'),
        content: SingleChildScrollView(
          child: Text(result['answer'] ?? result['content'] ?? '未识别到有效数据'),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: const Text('关闭'))
        ],
      ),
    );
  }

  void _openRecordModal(String type) {
    Widget modal;
    switch (type) {
      case 'feeding':
        modal = const FeedingTimerModal();
        break;
      case 'sleep':
        modal = const SleepTrackerModal();
        break;
      case 'growth':
        modal = const GrowthRecordModal();
        break;
      case 'milestone':
        modal = const MilestoneModal();
        break;
      case 'food':
        modal = const FoodRecordModal();
        break;
      case 'checkup':
        modal = const CheckupReportModal();
        break;
      default:
        return;
    }
    Navigator.pushNamed(context, '/modal/$type');
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * 0.75,
      decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      padding: EdgeInsets.only(
          top: 8,
          left: 16,
          right: 16,
          bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Handle
            Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                    color: Colors.grey[300],
                    borderRadius: BorderRadius.circular(2))),
            const SizedBox(height: 12),
            Text('智能记录', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),

            // Quick action grid
            Wrap(
              spacing: 12,
              runSpacing: 12,
              alignment: WrapAlignment.center,
              children: [
                _quickAction(Icons.mic, '语音记录', AppTheme.aiColor,
                    () => _toggleRecording()),
                _quickAction(Icons.camera_alt, '拍照识别', AppTheme.secondary,
                    _takePhotoForAnalysis),
                _quickAction(
                    Icons.local_dining,
                    '喂奶',
                    AppTheme.feedColor,
                    () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const FeedingTimerModal()))),
                _quickAction(
                    Icons.nights_stay,
                    '睡眠',
                    AppTheme.sleepColor,
                    () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const SleepTrackerModal()))),
                _quickAction(
                    Icons.trending_up,
                    '生长',
                    AppTheme.primary,
                    () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const GrowthRecordModal()))),
                _quickAction(
                    Icons.star,
                    '里程碑',
                    AppTheme.accent,
                    () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const MilestoneModal()))),
                _quickAction(
                    Icons.restaurant,
                    '辅食',
                    AppTheme.foodColor,
                    () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const FoodRecordModal()))),
                _quickAction(
                    Icons.medical_services,
                    '体检',
                    AppTheme.checkupColor,
                    () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const CheckupReportModal()))),
              ],
            ),
            const SizedBox(height: 16),

            // Voice input result area
            if (_voiceText != null) ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                    color: AppTheme.aiColor.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(12)),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(children: [
                      Icon(Icons.mic, color: AppTheme.aiColor, size: 18),
                      SizedBox(width: 8),
                      Text('识别内容:',
                          style: TextStyle(fontWeight: FontWeight.w600))
                    ]),
                    const SizedBox(height: 4),
                    Text(_voiceText!, style: const TextStyle(fontSize: 14)),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: _parsing
                                ? null
                                : () {
                                    setState(() => _voiceText = null);
                                  },
                            icon: const Icon(Icons.close),
                            label: const Text('取消'),
                            style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.grey[300]),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: _parsing ? null : _analyzeVoice,
                            icon: _parsing
                                ? const SizedBox(
                                    width: 16,
                                    height: 16,
                                    child: CircularProgressIndicator(
                                        strokeWidth: 2))
                                : const Icon(Icons.auto_awesome),
                            label: Text(_parsing ? 'AI 分析中' : 'AI 自动创建'),
                            style: ElevatedButton.styleFrom(
                                backgroundColor: AppTheme.aiColor),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
            ],

            // Photo preview and analyze
            if (_imageBase64 != null) ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                    color: AppTheme.secondary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(12)),
                child: Column(
                  children: [
                    const Text('照片已就绪',
                        style: TextStyle(fontWeight: FontWeight.w600)),
                    const SizedBox(height: 8),
                    ElevatedButton.icon(
                      onPressed: _analyzingImage ? null : _analyzeImage,
                      icon: _analyzingImage
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.camera_alt),
                      label: Text(_analyzingImage ? 'AI 识别中' : 'AI 提取数据'),
                      style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.secondary),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
            ],

            // Close button
            OutlinedButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('关闭'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _quickAction(
      IconData icon, String label, Color color, VoidCallback onTap) {
    return Material(
      color: color.withValues(alpha: 0.15),
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(fontSize: 11, color: color)),
          ]),
        ),
      ),
    );
  }
}
