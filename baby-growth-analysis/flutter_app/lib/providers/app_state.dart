/// 状态管理 - Provider 全局 store
library;

import 'package:flutter/foundation.dart';
import '../models/models.dart';
import '../services/api_service.dart';
import '../services/ai_service.dart';

class AppState with ChangeNotifier {
  late final ApiService _api;
  late final AiService _ai;

  // Active baby tracking
  String? get activeBabyId => _baby?.id;

  AppState(ApiService apiService) {
    _api = apiService;
    _ai = AiService();
  }

  set apiService(ApiService service) {
    _api = service;
  }

  AiService get ai => _ai;

  // ========== 核心数据 ==========
  Baby? _baby;
  Baby? get baby => _baby;
  String get babyId => _baby?.id ?? '';
  String get babyName => _baby?.name ?? '小公主';
  String get babyGender => _baby?.gender ?? 'female';

  List<DailyLog> _dailyLogs = [];
  List<DailyLog> get dailyLogs => _dailyLogs;

  List<Milestone> _milestones = [];
  List<Milestone> get milestones => _milestones;

  List<GrowthRecord> _growthRecords = [];
  List<GrowthRecord> get growthRecords => _growthRecords;

  List<FeedingRecord> _feedings = [];
  List<FeedingRecord> get feedings => _feedings;

  List<SleepRecord> _sleeps = [];
  List<SleepRecord> get sleeps => _sleeps;

  List<Vaccine> _vaccines = [];
  List<Vaccine> get vaccines => _vaccines;

  List<FoodRecord> _foods = [];
  List<FoodRecord> get foods => _foods;

  List<CheckupReport> _checkups = [];
  List<CheckupReport> get checkups => _checkups;

  List<LearningPlan> _learningPlans = [];
  List<LearningPlan> get learningPlans => _learningPlans;

  final List<MediaFile> _mediaFiles = [];
  List<MediaFile> get mediaFiles => _mediaFiles;

  List<AiConfig> _aiConfigs = [];
  List<AiConfig> get aiConfigs => _aiConfigs;

  bool _loading = false;
  bool get loading => _loading;

  String? _error;
  String? get error => _error;

  // AI 对话历史
  final List<Map<String, String>> _chatHistory = [];
  List<Map<String, String>> get chatHistory => List.unmodifiable(_chatHistory);

  bool get hasAiConfig => _ai.isConfigured;

  // ========== 初始化 ==========
  Future<void> init() async {
    _loading = true;
    notifyListeners();

    try {
      await _ai.init();

      // 获取宝宝信息
      final babies = await _api.getBabies();
      if (babies.isNotEmpty) {
        _baby = babies.first;
      }

      // 并行加载所有数据
      await Future.wait([
        loadDailyLogs(),
        loadMilestones(),
        loadGrowthRecords(),
        loadFeedings(),
        loadSleeps(),
        loadVaccines(),
        loadFoods(),
        loadCheckups(),
        loadLearningPlans(),
        loadAiConfigs(),
      ]);
    } catch (e) {
      _error = e.toString();
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  // ========== 数据加载 ==========

  Future<void> loadDailyLogs() async {
    try {
      _dailyLogs = await _api.getDailyLogs(babyId: babyId);
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadMilestones() async {
    try {
      _milestones = await _api.getMilestones(babyId: babyId);
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadGrowthRecords() async {
    try {
      final data = await _api.getGrowthCurve(babyId: babyId, metric: 'weight');
      final items = data['data'] as List? ?? [];
      _growthRecords = items
          .map((e) => GrowthRecord.fromJson({
                'id': '',
                'baby_id': babyId,
                'record_date': e['record_date'],
                'weight': e['value'],
                'height': null,
                'head_circumference': null,
                'notes': null,
                'created_at': DateTime.now().toIso8601String(),
              }))
          .toList();
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadFeedings() async {
    try {
      _feedings = await _api.getFeedings(babyId: babyId);
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadSleeps() async {
    try {
      final response = await _api.getSleeps(babyId: babyId);
      _sleeps = response.items;
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadVaccines() async {
    try {
      _vaccines = await _api.getVaccines(babyId: babyId);
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadFoods() async {
    try {
      _foods = await _api.getFoodRecords(babyId: babyId);
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadCheckups() async {
    try {
      _checkups = await _api.getCheckups(babyId: babyId);
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadLearningPlans() async {
    try {
      final response = await _api.getLearningPlans(babyId: babyId);
      _learningPlans = response.items;
      notifyListeners();
    } catch (_) {}
  }

  Future<void> loadAiConfigs() async {
    try {
      _aiConfigs = await _api.getAiConfigs();
      notifyListeners();
    } catch (_) {}
  }

  // ========== 操作 ==========

  Future<bool> addDailyLog({String? title, String? content}) async {
    try {
      final log = DailyLog(
        id: '',
        babyId: babyId,
        title: title ?? '',
        content: content ?? '',
        logDate: DateTime.now().date,
      );
      await _api.createDailyLog(log);
      _dailyLogs.insert(0, log);
      notifyListeners();
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    }
  }

  Future<bool> addGrowthRecord(
      {double? weight, double? height, double? headCirc}) async {
    try {
      final now = DateTime.now();
      final record = GrowthRecord(
        id: '',
        babyId: babyId,
        recordDate: now.date,
        weight: weight,
        height: height,
        headCircumference: headCirc,
        createdAt: now,
      );
      await _api.createGrowthRecord(record);
      notifyListeners();
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    }
  }

  Future<bool> addFeeding(
      {required String type, double? amount, String? notes}) async {
    try {
      final now = DateTime.now();
      final record = FeedingRecord(
        id: '',
        babyId: babyId,
        feedType: type,
        startTime: now,
        amountMl: amount,
        notes: notes,
        createdAt: now,
      );
      await _api.createFeeding(record);
      notifyListeners();
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    }
  }

  Future<bool> addMilestone(
      {required String type,
      required String title,
      String? description}) async {
    try {
      final m = Milestone(
        id: '',
        babyId: babyId,
        milestoneType: type,
        title: title,
        description: description,
        happenedAt: DateTime.now(),
        createdAt: DateTime.now(),
      );
      await _api.createMilestone(m);
      _milestones.insert(0, m);
      notifyListeners();
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    }
  }

  // ========== AI 聊天 ==========

  Future<String?> sendMessage(String message) async {
    try {
      _chatHistory.add({'role': 'user', 'content': message});
      final answer = await _ai.askQuestion(message,
          history: _chatHistory.take(10).toList());
      if (answer != null) {
        _chatHistory.add({'role': 'assistant', 'content': answer});
      }
      notifyListeners();
      return answer;
    } catch (e) {
      _error = e.toString();
      return null;
    }
  }

  clearChatHistory() {
    _chatHistory.clear();
    notifyListeners();
  }

  /// 根据语音解析结果自动创建记录
  Future<bool> autoCreateFromVoice(VoiceParseResult result) async {
    switch (result.type) {
      case 'feeding':
        final data = result.extractedData ?? {};
        return await addFeeding(
          type: data['feed_type'] as String? ?? 'breast_milk',
          amount: data['amount_ml'] as double?,
          notes: data['notes'] as String?,
        );
      case 'growth':
        final data = result.extractedData ?? {};
        return await addGrowthRecord(
          weight: data['weight'] as double?,
          height: data['height'] as double?,
          headCirc: data['head_circumference'] as double?,
        );
      case 'daily_log':
        final data = result.extractedData ?? {};
        final keywords = data['keywords'] as String? ?? result.summary;
        final diaryResult = await _ai.generateDiary(keywords: keywords);
        final title = diaryResult['title'] as String? ?? keywords;
        final content = diaryResult['content'] as String? ?? keywords;
        return await addDailyLog(title: title, content: content);
      case 'food':
        final data = result.extractedData ?? {};
        return await addFoodRecord(
          type: data['food_type'] as String? ?? 'cereal',
          amount: data['amount_g'] as double?,
          notes: data['notes'] as String?,
        );
      default:
        _error = '暂不支持的记录类型: ${result.type}';
        return false;
    }
  }

  Future<bool> addFoodRecord(
      {String? type, double? amount, String? notes}) async {
    try {
      final babyId = _baby?.id ?? '';
      if (babyId.isEmpty) {
        _error = '请先在设置中配置宝宝信息';
        return false;
      }
      final now = DateTime.now();
      final t = type ?? 'cereal';
      final record = FoodRecord(
        id: '',
        babyId: babyId,
        userId: null,
        foodDate: now,
        mealType: t == 'cereal'
            ? 'breakfast'
            : t == 'fruit'
                ? 'snack'
                : t == 'vegetable'
                    ? 'lunch'
                    : t == 'meat'
                        ? 'dinner'
                        : 'snack',
        foodName: {
              'cereal': '米粉',
              'fruit': '水果泥',
              'vegetable': '蔬菜泥',
              'meat': '肉泥',
              'soup': '米汤/粥'
            }[t] ??
            '辅食',
        foodCategory: null,
        firstTry: false,
        amount: amount?.toString(),
        reaction: null,
        allergySymptom: null,
        notes: notes,
        photosUrls: [],
        createdAt: now,
      );
      await _api.createFoodRecord(record);
      await loadFoods();
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    }
  }

  Future<bool> addCheckupReport(
      {DateTime? date,
      String? weight,
      String? height,
      String? headCircumference,
      String? notes,
      String? aiInterpretation}) async {
    try {
      final babyId = _baby?.id ?? '';
      if (babyId.isEmpty) {
        _error = '请先在设置中配置宝宝信息';
        return false;
      }
      final now = date ?? DateTime.now();
      final report = CheckupReport(
        id: '',
        babyId: babyId,
        checkupDate: now,
        checkupType: 'regular',
        hospital: null,
        doctor: null,
        weight: double.tryParse(weight ?? ''),
        height: double.tryParse(height ?? ''),
        headCircumference: double.tryParse(headCircumference ?? ''),
        vision: null,
        hearing: null,
        teethCount: null,
        bloodHemoglobin: null,
        bloodLead: null,
        vitaminD: null,
        developmentAssessment: null,
        doctorAdvice: aiInterpretation,
        nextCheckupDate: null,
        reportFileUrl: null,
        createdAt: now,
      );
      await _api.createCheckup(report);
      await loadCheckups();
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    }
  }

  /// 加载所有数据（一次性调用）
  Future<void> loadAllData() async {
    try {
      await Future.wait([
        loadDailyLogs(),
        loadMilestones(),
        loadGrowthRecords(),
        loadFeedings(),
        loadSleeps(),
        loadVaccines(),
        loadFoods(),
        loadCheckups(),
        loadLearningPlans(),
        loadAiConfigs(),
      ]);
    } catch (e) {
      _error = '数据加载失败: $e';
    }
  }
}

/// 日期扩展工具
extension on DateTime {
  DateTime get date => DateTime(year, month, day);
}
