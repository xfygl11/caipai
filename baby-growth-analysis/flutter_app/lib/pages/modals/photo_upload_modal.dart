/// 照片上传弹窗
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:convert';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:image_picker/image_picker.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';
import '../../services/ai_service.dart';
import '../../services/api_service.dart';

class PhotoUploadModal extends StatefulWidget {
  const PhotoUploadModal({super.key});

  @override
  State<PhotoUploadModal> createState() => _PhotoUploadModalState();
}

class _PhotoUploadModalState extends State<PhotoUploadModal> {
  String? caption;
  String? base64Image;
  bool uploading = false;
  final ImagePicker picker = ImagePicker();

  Future<void> pickImage(ImageSource source) async {
    try {
      final XFile? file = await picker.pickImage(source: source);
      if (file != null) {
        final bytes = await file.readAsBytes();
        setState(() => base64Image = base64Encode(bytes));
      }
    } catch (_) {}
  }

  Future<void> upload() async {
    if (base64Image == null) return;
    setState(() => uploading = true);

    final apiService = context.read<ApiService>();
    final babyId = context.read<AppState>().babyId;
    if (babyId.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('请先在设置中配置宝宝信息')));
      }
      setState(() => uploading = false);
      return;
    }

    // Save image to local temp file for upload
    try {
      final bytes = base64Decode(base64Image!);
      final tempDir = await getTemporaryDirectory();
      final file = await File(
              '${tempDir.path}/photo_${DateTime.now().millisecondsSinceEpoch}.jpg')
          .create();
      await file.writeAsBytes(bytes);

      final mediaFile = await apiService.uploadMedia(
        babyId: babyId,
        filePath: file.path,
        description: caption ?? '',
      );

      if (context.mounted && mediaFile.id.isNotEmpty) {
        if (context.read<AppState>().hasAiConfig) {
          final result = await context
              .read<AiService>()
              .analyzePhoto(mediaId: mediaFile.id);
          if (mounted) {
            showDialog(
                context: context,
                builder: (ctx) => AlertDialog(
                      title: const Text('AI 照片分析'),
                      content: SingleChildScrollView(
                          child: Text(result['answer'] ?? '分析完成')),
                      actions: [
                        TextButton(
                            onPressed: () => Navigator.pop(ctx),
                            child: const Text('关闭'))
                      ],
                    ));
          }
        }

        if (mounted) {
          Navigator.pop(context);
          ScaffoldMessenger.of(context)
              .showSnackBar(const SnackBar(content: Text('照片已上传')));
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('上传失败: ${e.toString()}')));
      }
    }
    setState(() => uploading = false);
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
            Text('照片记录', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton.icon(
                    onPressed: () => pickImage(ImageSource.camera),
                    icon: const Icon(Icons.camera_alt, size: 18),
                    label: const Text('拍照'),
                    style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primary)),
                ElevatedButton.icon(
                    onPressed: () => pickImage(ImageSource.gallery),
                    icon: const Icon(Icons.photo_library, size: 18),
                    label: const Text('相册'),
                    style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.secondary)),
              ],
            ),
            const SizedBox(height: 16),
            if (base64Image != null)
              Container(
                width: double.infinity,
                height: 200,
                decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.grey.shade300)),
                child: ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: Image.memory(base64Decode(base64Image!),
                        fit: BoxFit.cover)),
              )
            else ...[
              Container(
                width: double.infinity,
                height: 200,
                decoration: BoxDecoration(
                    color: Colors.grey.shade100,
                    borderRadius: BorderRadius.circular(12)),
                child: const Center(
                    child:
                        Text('选择或拍摄照片', style: TextStyle(color: Colors.grey))),
              ),
            ],
            const SizedBox(height: 16),
            TextField(
                decoration: const InputDecoration(
                    labelText: '备注（如：第一次叫妈妈）',
                    prefixIcon: Icon(Icons.note, color: AppTheme.primary)),
                onChanged: (v) => caption = v),
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
                        onPressed:
                            base64Image == null || uploading ? null : upload,
                        child: uploading
                            ? const CircularProgressIndicator()
                            : const Text('上传'))),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
