/// 相机服务 - 拍照 + 相册选择 + 图片转 base64
library;

import 'dart:convert';
import 'dart:io';
import 'package:camera/camera.dart';
import 'package:image_picker/image_picker.dart';

class CameraService {
  final ImagePicker _picker = ImagePicker();
  CameraController? _controller;
  List<CameraDescription>? _cameras;

  /// 初始化相机
  Future<bool> initCamera() async {
    try {
      _cameras = await availableCameras();
      if (_cameras == null || _cameras!.isEmpty) return false;

      _controller = CameraController(
        _cameras![0],
        ResolutionPreset.high,
        enableAudio: false,
      );
      await _controller!.initialize();
      return true;
    } catch (_) {
      return false;
    }
  }

  /// 拍照并获取 base64
  Future<String?> capturePhoto() async {
    if (_controller == null || !_controller!.value.isInitialized) return null;

    try {
      final XFile photo = await _controller!.takePicture();
      final bytes = await File(photo.path).readAsBytes();
      return base64Encode(bytes);
    } catch (_) {
      return null;
    }
  }

  /// 从相册选择图片并转为 base64
  Future<String?> pickImageFromGallery() async {
    try {
      final XFile? image = await _picker.pickImage(source: ImageSource.gallery);
      if (image == null) return null;

      final bytes = await File(image.path).readAsBytes();
      return base64Encode(bytes);
    } catch (_) {
      return null;
    }
  }

  /// 从相机拍摄图片并转为 base64
  Future<String?> takePhoto() async {
    try {
      final XFile? image = await _picker.pickImage(source: ImageSource.camera);
      if (image == null) return null;

      final bytes = await File(image.path).readAsBytes();
      return base64Encode(bytes);
    } catch (_) {
      return null;
    }
  }

  /// 释放资源
  Future<void> dispose() async {
    await _controller?.dispose();
  }

  bool get isReady => _controller != null && _controller!.value.isInitialized;
}
