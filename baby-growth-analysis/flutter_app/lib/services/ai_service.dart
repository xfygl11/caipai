/// AI 服务层 - 封装所有 AI 功能调用
/// 支持: 日记生成 / 育儿问答 / 成长故事 / 照片分析 / 智能提醒 / 疫苗问答 / 学习建议
library;


import '../models/models.dart';
import 'api_service.dart';

class AiService {
  final ApiService _api = ApiService();

  /// 当前默认宝宝 ID（首次加载时从后端获取）
  String? _defaultBabyId;
  String? get defaultBabyId => _defaultBabyId;

  bool get isConfigured => _configs.isNotEmpty;
  List<AiConfig> _configs = [];

  Future<void> init() async {
    try {
      final babies = await _api.getBabies();
      if (babies.isNotEmpty) {
        _defaultBabyId = babies.first.id;
      }
      _configs = await _api.getAiConfigs();
    } catch (_) {
      _defaultBabyId = '';
      _configs = [];
    }
  }

  /// ========== 语音指令解析 ==========
  /// 将用户口语化输入转换为结构化记录，调用 AI 自动判断类型并提取字段
  Future<VoiceParseResult> parseVoiceCommand(String voiceText) async {
    final prompt = '''
你是宝宝成长记录助手。请分析以下家长语音输入，自动判断要创建哪种类型的记录，并提取相关字段。
支持的类型: feeding(喂养), sleep(睡眠), growth(生长数据), food(辅食), daily_log(日记关键词), milestone(里程碑)。

家长语音: "$voiceText"

请严格按照以下 JSON 格式返回（不要添加任何其他内容）：
{
  "type": "feeding|sleep|growth|food|daily_log|milestone",
  "extracted_data": { ... 根据类型提取对应字段 ... },
  "summary": "一句话总结记录内容",
  "confidence": 0.95
}
''';

    try {
      final result = await _api.aiAskQuestion(
        question: prompt,
        babyId: _defaultBabyId,
      );

      final content =
          result['answer'] as String? ?? result['content'] as String? ?? '';

      // 尝试从文本中提取 JSON
      final jsonStart = content.indexOf('{');
      final jsonEnd = content.lastIndexOf('}') + 1;

      if (jsonStart >= 0 && jsonEnd > jsonStart) {
        final jsonStr = content.substring(jsonStart, jsonEnd);
        // 返回原始结果，由 UI 层解析 JSON
        return VoiceParseResult(
          rawAnswer: jsonStr,
          confidence: 0.9,
          summary: result['answer'] as String? ??
              result['content'] as String? ??
              voiceText,
        );
      }

      return VoiceParseResult(
        rawAnswer: '',
        confidence: 0.3,
        summary: content.isNotEmpty ? content : voiceText,
      );
    } catch (e) {
      // 降级：基于规则解析
      return _fallbackParse(voiceText);
    }
  }

  /// 基于规则的降级解析
  VoiceParseResult _fallbackParse(String text) {
    final lower = text.toLowerCase();

    // 喂养
    if (lower.contains('喂') || lower.contains('奶') || lower.contains('吃')) {
      int? ml;
      try {
        final match = RegExp(r'(\d+)').firstMatch(text);
        if (match != null) ml = int.tryParse(match.group(1)!);
      } catch (_) {}
      return VoiceParseResult(
        type: 'feeding',
        extractedData: {
          'feed_type': ml != null ? 'formula' : 'breast_milk',
          'amount_ml': ml?.toDouble(),
          'notes': text,
        },
        summary: '喂养记录：$text',
        confidence: 0.7,
      );
    }

    // 睡眠
    if (lower.contains('睡') || lower.contains('醒')) {
      return VoiceParseResult(
        type: 'sleep',
        extractedData: {'notes': text},
        summary: '睡眠记录：$text',
        confidence: 0.7,
      );
    }

    // 生长
    if (lower.contains('体重') ||
        lower.contains('身高') ||
        lower.contains('重') ||
        lower.contains('高')) {
      double? weight;
      try {
        final match = RegExp(r'([\d.]+)').firstMatch(text);
        if (match != null) weight = double.tryParse(match.group(1)!);
      } catch (_) {}
      return VoiceParseResult(
        type: 'growth',
        extractedData: {'weight': weight},
        summary: '生长数据：$text',
        confidence: 0.6,
      );
    }

    // 辅食
    if (lower.contains('辅食') || lower.contains('吃了') || lower.contains('吃')) {
      return VoiceParseResult(
        type: 'food',
        extractedData: {'food_name': text, 'notes': text},
        summary: '辅食记录：$text',
        confidence: 0.7,
      );
    }

    // 日记关键词
    return VoiceParseResult(
      type: 'daily_log',
      extractedData: {'keywords': text},
      summary: '日记关键词：$text',
      confidence: 0.5,
    );
  }

  /// ========== 拍照识别/OCR ==========
  /// 识别体检报告上的数据并自动填充
  Future<Map<String, dynamic>> recognizeCheckupReport({
    required String imageBase64,
    String prompt = '',
  }) async {
    final effectivePrompt = prompt.isNotEmpty
        ? prompt
        : '''请识别这张图片中的体检数据，提取以下字段并以JSON格式返回：
- weight: 体重(kg)
- height: 身高(cm)
- head_circumference: 头围(cm)
- teeth_count: 牙齿数量
- blood_hemoglobin: 血红蛋白(g/L)
- vision: 视力
- hearing: 听力
- doctor_advice: 医生建议

只返回JSON，不要其他文字。''';

    try {
      return await _api.aiAnalyzeImage(
        babyId: _defaultBabyId ?? '',
        imageBase64: imageBase64,
        prompt: effectivePrompt,
      );
    } catch (e) {
      return {'error': 'AI模型暂时不可用，请手动录入数据', 'fallback': true};
    }
  }

  /// ========== 辅助方法集合 ==========

  /// AI生成日记
  Future<Map<String, dynamic>> generateDiary({
    required String keywords,
    String template = 'daily',
  }) async {
    return await _api.aiGenerateDiary(
      babyId: _defaultBabyId ?? '',
      keywords: keywords,
      template: template,
    );
  }

  /// AI育儿问答
  Future<String?> askQuestion(String question,
      {List<Map<String, String>>? history}) async {
    try {
      final result = await _api.aiAskQuestion(
        question: question,
        babyId: _defaultBabyId,
        history: history,
      );
      return result['answer'] as String? ?? result['content'] as String?;
    } catch (_) {
      return null;
    }
  }

  /// AI成长故事
  Future<Map<String, dynamic>> generateStory({
    required DateTime startDate,
    required DateTime endDate,
    String style = 'warm',
  }) async {
    return await _api.aiGenerateStory(
      babyId: _defaultBabyId ?? '',
      startDate: startDate,
      endDate: endDate,
      style: style,
    );
  }

  /// AI智能提醒
  Future<Map<String, dynamic>> getReminders() async {
    try {
      return await _api.aiGetReminders(babyId: _defaultBabyId);
    } catch (_) {
      return {'heading': '', 'items': []};
    }
  }

  /// AI疫苗问答
  Future<String?> vaccineAsk(String vaccineName, String question) async {
    try {
      final result = await _api.aiVaccineAsk(
        vaccineName: vaccineName,
        question: question,
        babyId: _defaultBabyId,
      );
      return result['answer'] as String?;
    } catch (_) {
      return null;
    }
  }

  /// AI学习建议
  Future<Map<String, dynamic>> learningSuggestion(
      {String category = ''}) async {
    try {
      return await _api.aiLearningSuggestion(
        babyId: _defaultBabyId ?? '',
        category: category,
      );
    } catch (_) {
      return {'suggestions': []};
    }
  }

  /// AI辅食建议
  Future<Map<String, dynamic>> foodSuggestion(
      {double? monthsOld,
      String triedFoods = '',
      String allergyFoods = ''}) async {
    try {
      return await _api.aiFoodSuggestion(
        babyId: _defaultBabyId ?? '',
        monthsOld: monthsOld,
        triedFoods: triedFoods,
        allergyFoods: allergyFoods,
      );
    } catch (_) {
      return {'meals': []};
    }
  }

  /// AI体检解读
  Future<String?> analyzeCheckup({
    required double monthsOld,
    required double weight,
    required double height,
    double? headCircumference,
  }) async {
    try {
      final result = await _api.aiCheckupAnalyze(
        babyId: _defaultBabyId ?? '',
        monthsOld: monthsOld,
        weight: weight,
        height: height,
        headCircumference: headCircumference,
      );
      return result['analysis'] is Map
          ? (result['analysis'] as Map)['summary'] as String?
          : (result['analysis'] as String?);
    } catch (_) {
      return null;
    }
  }

  /// AI 分析已上传的照片（通过 mediaId）
  Future<Map<String, dynamic>> analyzePhoto({required String mediaId}) async {
    return await _api.aiAnalyzeMedia(mediaId: mediaId);
  }
}

/// 语音解析结果
class VoiceParseResult {
  final String type;
  final Map<String, dynamic>? extractedData;
  final String summary;
  final double confidence;
  final String rawAnswer;

  VoiceParseResult({
    this.type = 'unknown',
    this.extractedData,
    required this.summary,
    this.confidence = 0.0,
    this.rawAnswer = '',
  });
}
