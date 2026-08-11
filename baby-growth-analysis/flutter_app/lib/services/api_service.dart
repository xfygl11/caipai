/// 统一 API 客户端
/// 所有后端接口调用封装在此
library;

import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/app_config.dart';
import '../models/models.dart';

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal();

  String get baseUrl => AppConfig.apiBaseUrl;

  // ========== 通用方法 ==========

  Future<Map<String, dynamic>> get(String path,
      {Map<String, String>? query}) async {
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: query);
    final response = await http.get(uri);
    if (response.statusCode == 200) {
      return json.decode(response.body) as Map<String, dynamic>;
    }
    throw ApiException(response.statusCode, response.body);
  }

  Future<List<dynamic>> getList(String path,
      {Map<String, String>? query}) async {
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: query);
    final response = await http.get(uri);
    if (response.statusCode == 200) {
      final decoded = json.decode(response.body);
      if (decoded is List) return decoded;
      return [];
    }
    throw ApiException(response.statusCode, response.body);
  }

  Future<Map<String, dynamic>> post(
      String path, Map<String, dynamic> body) async {
    final response = await http.post(
      Uri.parse('$baseUrl$path'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode(body),
    );
    if (response.statusCode == 200 || response.statusCode == 201) {
      return json.decode(response.body) as Map<String, dynamic>;
    }
    throw ApiException(response.statusCode, response.body);
  }

  Future<Map<String, dynamic>> put(
      String path, Map<String, dynamic> body) async {
    final response = await http.put(
      Uri.parse('$baseUrl$path'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode(body),
    );
    if (response.statusCode == 200) {
      return json.decode(response.body) as Map<String, dynamic>;
    }
    throw ApiException(response.statusCode, response.body);
  }

  Future<void> delete(String path) async {
    final response = await http.delete(Uri.parse('$baseUrl$path'));
    if (response.statusCode != 200) {
      throw ApiException(response.statusCode, response.body);
    }
  }

  // ========== 宝宝管理 ==========

  Future<List<Baby>> getBabies() async {
    final data = await getList('/api/babies');
    return data.map((e) => Baby.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Baby> getBaby(String babyId) async {
    final data = await get('/api/babies/$babyId');
    return Baby.fromJson(data);
  }

  Future<Baby> createBaby(Map<String, dynamic> params) async {
    final data = await post('/api/babies', params);
    return Baby.fromJson(data);
  }

  Future<Baby> updateBaby(String babyId, Map<String, dynamic> params) async {
    final data = await put('/api/babies/$babyId', params);
    return Baby.fromJson(data);
  }

  // ========== 每日日志/日记 ==========

  Future<DailyLog> createDailyLog(DailyLog log) async {
    final data = await post('/api/daily-logs', log.toJson());
    return DailyLog.fromJson(data);
  }

  Future<List<DailyLog>> getDailyLogs({String? babyId, String? date}) async {
    final data = await getList('/api/daily-logs',
        query: {'baby_id': babyId ?? '', 'date': date ?? ''});
    return data
        .map((e) => DailyLog.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<PaginatedResponse<DailyLog>> getDailyLogsTimeline(
      {String? babyId, int page = 1, int size = 20}) async {
    final data = await get('/api/daily-logs/timeline', query: {
      'baby_id': babyId ?? '',
      'page': '$page',
      'size': '$size',
    });
    return PaginatedResponse<DailyLog>.fromJson(
        data, (e) => DailyLog.fromJson(e as Map<String, dynamic>));
  }

  // ========== 里程碑 ==========

  Future<Milestone> createMilestone(Milestone m) async {
    final data = await post('/api/milestones', {
      'baby_id': m.babyId,
      'milestone_type': m.milestoneType,
      'title': m.title,
      if (m.description != null) 'description': m.description,
      if (m.happenedAt != null) 'happened_at': m.happenedAt!.toIso8601String(),
      'media_urls': m.mediaUrls,
    });
    return Milestone.fromJson(data);
  }

  Future<List<Milestone>> getMilestones({String? babyId}) async {
    final data =
        await getList('/api/milestones', query: {'baby_id': babyId ?? ''});
    return data
        .map((e) => Milestone.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // ========== 生长记录 ==========

  Future<GrowthRecord> createGrowthRecord(GrowthRecord r) async {
    final data = await post('/api/growth', r.toJson());
    return GrowthRecord.fromJson(data);
  }

  Future<GrowthRecord?> getLatestGrowth({String? babyId}) async {
    try {
      final data =
          await get('/api/growth/latest', query: {'baby_id': babyId ?? ''});
      if (!(data['found'] as bool)) return null;
      return GrowthRecord.fromJson(data);
    } catch (_) {
      return null;
    }
  }

  Future<Map<String, dynamic>> getGrowthCurve(
      {String? babyId, String metric = 'weight'}) async {
    return await get('/api/growth/curve',
        query: {'baby_id': babyId ?? '', 'metric': metric});
  }

  // ========== 喂养记录 ==========

  Future<FeedingRecord> createFeeding(FeedingRecord r) async {
    final data = await post('/api/feedings', {
      'baby_id': r.babyId,
      'feed_type': r.feedType,
      if (r.amountMl != null) 'amount_ml': r.amountMl,
      'start_time': r.startTime.toIso8601String(),
      if (r.endTime != null) 'end_time': r.endTime!.toIso8601String(),
      if (r.notes != null) 'notes': r.notes,
    });
    return FeedingRecord.fromJson(data);
  }

  Future<List<FeedingRecord>> getFeedings(
      {String? babyId, String? date}) async {
    final data = await getList('/api/feedings',
        query: {'baby_id': babyId ?? '', 'date': date ?? ''});
    return data
        .map((e) => FeedingRecord.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<Map<String, dynamic>> getFeedingStats(
      {String? babyId, String? date}) async {
    return await get('/api/feedings/stats',
        query: {'baby_id': babyId ?? '', 'date': date ?? ''});
  }

  // ========== 睡眠记录 ==========

  Future<SleepRecord> startSleep(SleepRecord r) async {
    final data = await post('/api/sleep', {
      'baby_id': r.babyId,
      'start_time': r.startTime.toIso8601String(),
      if (r.notes != null) 'notes': r.notes,
    });
    return SleepRecord.fromJson(data);
  }

  Future<SleepRecord> endSleep(String sleepId,
      {String? quality, String? notes, DateTime? endTime}) async {
    final data = await put('/api/sleep/$sleepId', {
      if (endTime != null) 'end_time': endTime.toIso8601String(),
      if (quality != null) 'quality': quality,
      if (notes != null) 'notes': notes,
    });
    return SleepRecord.fromJson(data);
  }

  Future<PaginatedResponse<SleepRecord>> getSleeps(
      {String? babyId, int page = 1}) async {
    final data = await get('/api/sleep',
        query: {'baby_id': babyId ?? '', 'page': '$page'});
    return PaginatedResponse<SleepRecord>.fromJson(
        data, (e) => SleepRecord.fromJson(e as Map<String, dynamic>));
  }

  // ========== 疫苗 ==========

  Future<List<Vaccine>> getVaccines({String? babyId, String? status}) async {
    final data = await getList('/api/vaccines', query: {
      'baby_id': babyId ?? '',
      if (status != null) 'status': status,
    });

    return data
        .map((e) => Vaccine.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<Map<String, dynamic>> getVaccineGuide() async {
    return await get('/api/vaccines/guide');
  }

  Future<Vaccine> markVaccineDone(String vaccineId,
      {DateTime? actualDate,
      String? manufacturer,
      String? batchNumber,
      String? hospital,
      String? reaction}) async {
    final data = await post('/api/vaccines/$vaccineId/mark-done', {
      if (actualDate != null)
        'actual_date': actualDate.toString().split(' ')[0],
      if (manufacturer != null) 'manufacturer': manufacturer,
      if (batchNumber != null) 'batch_number': batchNumber,
      if (hospital != null) 'hospital': hospital,
      if (reaction != null) 'reaction': reaction,
    });
    return Vaccine.fromJson(data);
  }

  // ========== 辅食 ==========

  Future<FoodRecord> createFoodRecord(FoodRecord r) async {
    final data = await post('/api/food', {
      'baby_id': r.babyId,
      'food_date': r.foodDate.toString().split(' ')[0],
      'meal_type': r.mealType,
      'food_name': r.foodName,
      if (r.foodCategory != null) 'food_category': r.foodCategory,
      'first_try': r.firstTry,
      if (r.amount != null) 'amount': r.amount,
      if (r.reaction != null) 'reaction': r.reaction,
      if (r.allergySymptom != null) 'allergy_symptom': r.allergySymptom,
      if (r.notes != null) 'notes': r.notes,
      'photos_urls': r.photosUrls,
    });
    return FoodRecord.fromJson(data);
  }

  Future<List<FoodRecord>> getFoodRecords(
      {String? babyId, int page = 1}) async {
    final data = await get('/api/food',
        query: {'baby_id': babyId ?? '', 'page': '$page'});
    return PaginatedResponse<FoodRecord>.fromJson(
        data, (e) => FoodRecord.fromJson(e as Map<String, dynamic>)).items;
  }

  // ========== 体检报告 ==========

  Future<CheckupReport> createCheckup(CheckupReport r) async {
    final data = await post('/api/checkups', {
      'baby_id': r.babyId,
      'checkup_date': r.checkupDate.toString().split(' ')[0],
      if (r.checkupType != null) 'checkup_type': r.checkupType,
      if (r.hospital != null) 'hospital': r.hospital,
      if (r.doctor != null) 'doctor': r.doctor,
      if (r.weight != null) 'weight': r.weight,
      if (r.height != null) 'height': r.height,
      if (r.headCircumference != null)
        'head_circumference': r.headCircumference,
      if (r.vision != null) 'vision': r.vision,
      if (r.hearing != null) 'hearing': r.hearing,
      if (r.teethCount != null) 'teeth_count': r.teethCount,
      if (r.bloodHemoglobin != null) 'blood_hemoglobin': r.bloodHemoglobin,
      if (r.bloodLead != null) 'blood_lead': r.bloodLead,
      if (r.vitaminD != null) 'vitamin_d': r.vitaminD,
      if (r.developmentAssessment != null)
        'development_assessment': r.developmentAssessment,
      if (r.doctorAdvice != null) 'doctor_advice': r.doctorAdvice,
      if (r.nextCheckupDate != null)
        'next_checkup_date': r.nextCheckupDate.toString().split(' ')[0],
      if (r.reportFileUrl != null) 'report_file_url': r.reportFileUrl,
    });
    return CheckupReport.fromJson(data);
  }

  Future<List<CheckupReport>> getCheckups({String? babyId}) async {
    final data =
        await getList('/api/checkups', query: {'baby_id': babyId ?? ''});
    return data
        .map((e) => CheckupReport.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // ========== 学习启蒙 ==========

  Future<PaginatedResponse<LearningPlan>> getLearningPlans(
      {String? babyId, String? category}) async {
    final data = await get('/api/learning/plans', query: {
      if (babyId != null) 'baby_id': babyId,
      if (category != null && category.isNotEmpty) 'category': category,
    });
    return PaginatedResponse<LearningPlan>.fromJson(
        data, (e) => LearningPlan.fromJson(e as Map<String, dynamic>));
  }

  // ========== AI 功能 ==========

  /// 日记生成
  Future<Map<String, dynamic>> aiGenerateDiary({
    required String babyId,
    required String keywords,
    String template = 'daily',
  }) async {
    return await post('/api/ai/diary', {
      'baby_id': babyId,
      'keywords': keywords,
      'template': template,
    });
  }

  /// 育儿问答
  Future<Map<String, dynamic>> aiAskQuestion({
    required String question,
    String? babyId,
    List<Map<String, String>>? history,
  }) async {
    return await post('/api/ai/ask', {
      if (babyId != null) 'baby_id': babyId,
      'question': question,
      if (history != null) 'history': history,
    });
  }

  /// 成长故事
  Future<Map<String, dynamic>> aiGenerateStory({
    required String babyId,
    required DateTime startDate,
    required DateTime endDate,
    String style = 'warm',
  }) async {
    return await post('/api/ai/story', {
      'baby_id': babyId,
      'start_date': startDate.toString().split(' ')[0],
      'end_date': endDate.toString().split(' ')[0],
      'style': style,
    });
  }

  /// 照片分析
  Future<Map<String, dynamic>> aiAnalyzeImage({
    required String babyId,
    required String imageBase64,
    String prompt = '',
  }) async {
    return await post('/api/ai/analyze-image', {
      if (babyId.isNotEmpty) 'baby_id': babyId,
      'image_base64': imageBase64,
      if (prompt.isNotEmpty) 'prompt': prompt,
    });
  }

  /// 智能提醒
  Future<Map<String, dynamic>> aiGetReminders({String? babyId}) async {
    return await get('/api/ai/reminders', query: {'baby_id': babyId ?? ''});
  }

  /// 疫苗问答
  Future<Map<String, dynamic>> aiVaccineAsk({
    required String vaccineName,
    required String question,
    String? babyId,
  }) async {
    return await post('/api/ai/vaccine-ask', {
      if (babyId != null) 'baby_id': babyId,
      'vaccine_name': vaccineName,
      'question': question,
    });
  }

  /// 学习建议
  Future<Map<String, dynamic>> aiLearningSuggestion({
    required String babyId,
    String category = '',
    int timeAvailable = 15,
  }) async {
    return await post('/api/ai/learning-suggestion', {
      'baby_id': babyId,
      if (category.isNotEmpty) 'category': category,
      'time_available': timeAvailable,
    });
  }

  /// 体检解读
  Future<Map<String, dynamic>> aiCheckupAnalyze({
    required String babyId,
    double? monthsOld,
    double? weight,
    double? height,
    double? headCircumference,
  }) async {
    Map<String, dynamic> body = {'baby_id': babyId};
    if (monthsOld != null) body['months_old'] = monthsOld;
    if (weight != null) body['weight'] = weight;
    if (height != null) body['height'] = height;
    if (headCircumference != null)
      body['head_circumference'] = headCircumference;
    return await post('/api/ai/checkup-analyze', body);
  }

  /// AI 分析已上传的媒体文件（通过 mediaId）
  Future<Map<String, dynamic>> aiAnalyzeMedia({required String mediaId}) async {
    return await post('/api/ai/analyze-photo', {'media_id': mediaId});
  }


  /// 辅食建议
  Future<Map<String, dynamic>> aiFoodSuggestion({
    required String babyId,
    double? monthsOld,
    String triedFoods = '',
    String allergyFoods = '',
    String season = '',
  }) async {
    Map<String, dynamic> body = {'baby_id': babyId};
    if (monthsOld != null) body['months_old'] = monthsOld;
    if (triedFoods.isNotEmpty) body['tried_foods'] = triedFoods;
    if (allergyFoods.isNotEmpty) body['allergy_foods'] = allergyFoods;
    if (season.isNotEmpty) body['season'] = season;
    return await post('/api/ai/food-suggestion', body);
  }

  // ========== AI 配置 ==========

  Future<List<AiConfig>> getAiConfigs() async {
    final data = await getList('/api/ai-configs');
    return data
        .map((e) => AiConfig.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<AiConfig> createAiConfig(AiConfig config) async {
    final data = await post('/api/ai-configs', {
      'name': config.name,
      'provider': config.provider,
      'api_base_url': config.apiBaseUrl,
      'api_key': config.apiKey ?? '',
      'model': config.model,
      if (config.visionModel != null) 'vision_model': config.visionModel,
      'temperature': config.temperature,
      'max_tokens': config.maxTokens,
      'timeout_seconds': config.timeoutSeconds,
      'is_default': config.isDefault,
      'is_active': config.isActive,
    });
    return AiConfig.fromJson(data);
  }

  Future<AiConfig> setDefaultConfig(String configId) async {
    final data = await post('/api/ai-configs/$configId/set-default', {});
    return AiConfig.fromJson(data);
  }

  Future<void> deleteAiConfig(String configId) async {
    await delete('/api/ai-configs/$configId');
  }

  Future<Map<String, dynamic>> testAiConnection({
    required String provider,
    required String apiBaseUrl,
    required String apiKey,
    required String model,
  }) async {
    return await post('/api/ai-configs/test', {
      'provider': provider,
      'api_base_url': apiBaseUrl,
      'api_key': apiKey,
      'model': model,
    });
  }

  // ========== 媒体文件 ==========

  Future<MediaFile> uploadMedia({
    required String babyId,
    required String filePath,
    String description = '',
    DateTime? takenAt,
  }) async {
    // 使用 multipart 上传
    final request = http.MultipartRequest(
      'POST',
      Uri.parse('$baseUrl/api/media/upload'),
    );
    request.fields['description'] = description;
    if (takenAt != null) {
      request.fields['taken_at'] = takenAt.toIso8601String();
    }
    request.files.add(await http.MultipartFile.fromPath('file', filePath));

    final response = await request.send();
    final responseBody = await response.stream.bytesToString();

    if (response.statusCode == 200 || response.statusCode == 201) {
      return MediaFile.fromJson(
          json.decode(responseBody) as Map<String, dynamic>);
    }
    throw ApiException(response.statusCode, responseBody);
  }

  Future<List<MediaFile>> getMedia(
      {String? babyId, String? fileType, String? month}) async {
    final data = await getList('/api/media', query: {
      if (babyId != null) 'baby_id': babyId,
      if (fileType != null) 'type': fileType,
      if (month != null) 'month': month,
    });

    return data
        .map((e) => MediaFile.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}

class ApiException implements Exception {
  final int statusCode;
  final String message;
  ApiException(this.statusCode, this.message);
  @override
  String toString() => 'ApiException($statusCode): $message';
}
