/// 录音服务 - 语音转文字 + 自动创建记录
/// 使用 record 包进行麦克风录音，返回音频文件路径供后端处理
library;

import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:record/record.dart';

class VoiceRecordService {
  final AudioRecorder _recorder = AudioRecorder();
  String? _currentPath;

  /// 检查录音权限
  Future<bool> checkPermission() async {
    final status = await Permission.microphone.status;
    if (status.isGranted) return true;

    final result = await Permission.microphone.request();
    return result.isGranted;
  }

  /// 开始录音
  Future<bool> startRecording({String deviceId = ''}) async {
    if (!await _recorder.hasPermission()) return false;

    try {
      final dir = await getTemporaryDirectory();
      _currentPath = '${dir.path}/voice_${DateTime.now().millisecondsSinceEpoch}.ogg';

      await _recorder.start(
        const RecordConfig(
          encoder: AudioEncoder.opus,
          bitRate: 128000,
          sampleRate: 44100,
        ),
        path: _currentPath!,
      );
      return true;
    } catch (_) {
      return false;
    }
  }

  /// 停止录音，返回音频文件路径
  Future<String?> stopRecording() async {
    if (!await _recorder.isRecording()) return null;

    try {
      final path = await _recorder.stop();
      return path;
    } catch (_) {
      return null;
    }
  }

  /// 获取当前录音路径
  String? get currentPath => _currentPath;

  /// 获取录音状态
  Stream<RecordState> get onStateChanged => _recorder.onStateChanged();

  /// 是否正在录音
  Future<bool> isRecording() async {
    return await _recorder.isRecording();
  }

  /// 释放资源
  Future<void> dispose() async {
    await _recorder.stop();
    await _recorder.dispose();
  }

  /// 保存录音到本地（用于临时存储后发送给后端语音识别）
  Future<String?> saveRecording(String sourcePath) async {
    try {
      final dir = await getTemporaryDirectory();
      final fileName = 'voice_${DateTime.now().millisecondsSinceEpoch}.ogg';
      final targetPath = '${dir.path}/$fileName';
      await File(sourcePath).copy(targetPath);
      return targetPath;
    } catch (_) {
      return null;
    }
  }
}
