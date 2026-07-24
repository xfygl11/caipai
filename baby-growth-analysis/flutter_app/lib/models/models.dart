/// API 响应模型定义
/// 完全匹配后端 SQLAlchemy 模型的 JSON 输出格式
library;

class Baby {
  final String id;
  final String name;
  final String gender;
  final DateTime birthTime;
  final double? birthWeight;
  final double? birthHeight;
  final String? avatarUrl;
  final String? familyGroupId;
  final DateTime createdAt;
  final BabyAge age;

  Baby({
    required this.id,
    required this.name,
    required this.gender,
    required this.birthTime,
    this.birthWeight,
    this.birthHeight,
    this.avatarUrl,
    this.familyGroupId,
    required this.createdAt,
    required this.age,
  });

  factory Baby.fromJson(Map<String, dynamic> json) {
    return Baby(
      id: json['id'] as String,
      name: json['name'] as String? ?? '小公主',
      gender: json['gender'] as String? ?? 'female',
      birthTime: DateTime.parse(json['birth_time'] as String),
      birthWeight: json['birth_weight'] as double?,
      birthHeight: json['birth_height'] as double?,
      avatarUrl: json['avatar_url'] as String?,
      familyGroupId: json['family_group_id'] as String?,
      createdAt: DateTime.parse(json['created_at'] as String),
      age: json['age'] != null ? BabyAge.fromJson(json['age']) : BabyAge(),
    );
  }
}

class BabyAge {
  final int days;
  final int hours;
  final int minutes;
  final int seconds;
  final double totalDays;
  final double months;
  final String text;

  BabyAge({
    this.days = 0,
    this.hours = 0,
    this.minutes = 0,
    this.seconds = 0,
    this.totalDays = 0,
    this.months = 0,
    this.text = '出生第0天',
  });

  factory BabyAge.fromJson(Map<String, dynamic> json) {
    return BabyAge(
      days: json['days'] as int? ?? 0,
      hours: json['hours'] as int? ?? 0,
      minutes: json['minutes'] as int? ?? 0,
      seconds: json['seconds'] as int? ?? 0,
      totalDays: (json['total_days'] as num?)?.toDouble() ?? 0,
      months: (json['months'] as num?)?.toDouble() ?? 0,
      text: json['text'] as String? ?? '出生第${json['days']}天',
    );
  }
}

class DailyLog {
  final String id;
  final String babyId;
  final String? userId;
  final DateTime? logDate;
  final String? title;
  final String? content;
  final String? mood;
  final String? weather;
  final bool aiGenerated;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  DailyLog({
    required this.id,
    required this.babyId,
    this.userId,
    this.logDate,
    this.title,
    this.content,
    this.mood,
    this.weather,
    this.aiGenerated = false,
    this.createdAt,
    this.updatedAt,
  });

  factory DailyLog.fromJson(Map<String, dynamic> json) {
    return DailyLog(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      userId: json['user_id'] as String?,
      logDate: json['log_date'] != null
          ? DateTime.parse(json['log_date']).date
          : null,
      title: json['title'] as String?,
      content: json['content'] as String?,
      mood: json['mood'] as String?,
      weather: json['weather'] as String?,
      aiGenerated: json['ai_generated'] as bool? ?? false,
      createdAt: json['created_at'] != null
          ? DateTime.parse(json['created_at'])
          : null,
      updatedAt: json['updated_at'] != null
          ? DateTime.parse(json['updated_at'])
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'baby_id': babyId,
      if (logDate != null) 'log_date': logDate.toString().split(' ')[0],
      if (title != null) 'title': title,
      if (content != null) 'content': content,
      if (mood != null) 'mood': mood,
      if (weather != null) 'weather': weather,
      'ai_generated': aiGenerated,
    };
  }
}

extension on DateTime {
  DateTime get date => DateTime(year, month, day);
}

class Milestone {
  final String id;
  final String babyId;
  final String milestoneType;
  final String title;
  final String? description;
  final DateTime? happenedAt;
  final List<String> mediaUrls;
  final DateTime createdAt;

  Milestone({
    required this.id,
    required this.babyId,
    required this.milestoneType,
    required this.title,
    this.description,
    this.happenedAt,
    List<String>? mediaUrls,
    required this.createdAt,
  }) : mediaUrls = mediaUrls ?? [];

  factory Milestone.fromJson(Map<String, dynamic> json) {
    return Milestone(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      milestoneType: json['milestone_type'] as String,
      title: json['title'] as String,
      description: json['description'] as String?,
      happenedAt: json['happened_at'] != null
          ? DateTime.parse(json['happened_at'])
          : null,
      mediaUrls:
          (json['media_urls'] as List?)?.map((e) => e as String).toList() ?? [],
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class GrowthRecord {
  final String id;
  final String babyId;
  final DateTime recordDate;
  final double? weight;
  final double? height;
  final double? headCircumference;
  final String? notes;
  final DateTime createdAt;

  GrowthRecord({
    required this.id,
    required this.babyId,
    required this.recordDate,
    this.weight,
    this.height,
    this.headCircumference,
    this.notes,
    required this.createdAt,
  });

  factory GrowthRecord.fromJson(Map<String, dynamic> json) {
    return GrowthRecord(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      recordDate: DateTime.parse(json['record_date']).date,
      weight: (json['weight'] as num?)?.toDouble(),
      height: (json['height'] as num?)?.toDouble(),
      headCircumference: (json['head_circumference'] as num?)?.toDouble(),
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'baby_id': babyId,
      'record_date': recordDate.toString().split(' ')[0],
      if (weight != null) 'weight': weight,
      if (height != null) 'height': height,
      if (headCircumference != null) 'head_circumference': headCircumference,
      if (notes != null) 'notes': notes,
    };
  }
}

class FeedingRecord {
  final String id;
  final String babyId;
  final String? userId;
  final String feedType;
  final double? amountMl;
  final DateTime startTime;
  final DateTime? endTime;
  final String? notes;
  final DateTime createdAt;

  FeedingRecord({
    required this.id,
    required this.babyId,
    this.userId,
    required this.feedType,
    this.amountMl,
    required this.startTime,
    this.endTime,
    this.notes,
    required this.createdAt,
  });

  factory FeedingRecord.fromJson(Map<String, dynamic> json) {
    return FeedingRecord(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      userId: json['user_id'] as String?,
      feedType: json['feed_type'] as String,
      amountMl: (json['amount_ml'] as num?)?.toDouble(),
      startTime: DateTime.parse(json['start_time'] as String),
      endTime:
          json['end_time'] != null ? DateTime.parse(json['end_time']) : null,
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class SleepRecord {
  final String id;
  final String babyId;
  final DateTime startTime;
  final DateTime? endTime;
  final String? quality;
  final String? notes;
  final int? durationMinutes;
  final DateTime createdAt;

  SleepRecord({
    required this.id,
    required this.babyId,
    required this.startTime,
    this.endTime,
    this.quality,
    this.notes,
    this.durationMinutes,
    required this.createdAt,
  });

  factory SleepRecord.fromJson(Map<String, dynamic> json) {
    return SleepRecord(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      startTime: DateTime.parse(json['start_time'] as String),
      endTime:
          json['end_time'] != null ? DateTime.parse(json['end_time']) : null,
      quality: json['quality'] as String?,
      notes: json['notes'] as String?,
      durationMinutes: json['duration_minutes'] as int?,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class Vaccine {
  final String id;
  final String babyId;
  final String vaccineName;
  final int doseNumber;
  final DateTime? dueDate;
  final DateTime? actualDate;
  final String status;
  final String vaccineType;
  final String? manufacturer;
  final String? batchNumber;
  final String? hospital;
  final String? reaction;
  final String? notes;
  final DateTime createdAt;

  Vaccine({
    required this.id,
    required this.babyId,
    required this.vaccineName,
    required this.doseNumber,
    this.dueDate,
    this.actualDate,
    required this.status,
    required this.vaccineType,
    this.manufacturer,
    this.batchNumber,
    this.hospital,
    this.reaction,
    this.notes,
    required this.createdAt,
  });

  factory Vaccine.fromJson(Map<String, dynamic> json) {
    return Vaccine(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      vaccineName: json['vaccine_name'] as String,
      doseNumber: json['dose_number'] as int? ?? 1,
      dueDate: json['due_date'] != null
          ? DateTime.parse(json['due_date']).date
          : null,
      actualDate: json['actual_date'] != null
          ? DateTime.parse(json['actual_date']).date
          : null,
      status: json['status'] as String? ?? 'pending',
      vaccineType: json['vaccine_type'] as String? ?? 'free',
      manufacturer: json['manufacturer'] as String?,
      batchNumber: json['batch_number'] as String?,
      hospital: json['hospital'] as String?,
      reaction: json['reaction'] as String?,
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class FoodRecord {
  final String id;
  final String babyId;
  final String? userId;
  final DateTime foodDate;
  final String mealType;
  final String foodName;
  final String? foodCategory;
  final bool firstTry;
  final String? amount;
  final String? reaction;
  final String? allergySymptom;
  final String? notes;
  final List<String> photosUrls;
  final DateTime createdAt;

  FoodRecord({
    required this.id,
    required this.babyId,
    this.userId,
    required this.foodDate,
    required this.mealType,
    required this.foodName,
    this.foodCategory,
    this.firstTry = false,
    this.amount,
    this.reaction,
    this.allergySymptom,
    this.notes,
    List<String>? photosUrls,
    required this.createdAt,
  }) : photosUrls = photosUrls ?? [];

  factory FoodRecord.fromJson(Map<String, dynamic> json) {
    return FoodRecord(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      userId: json['user_id'] as String?,
      foodDate: DateTime.parse(json['food_date']).date,
      mealType: json['meal_type'] as String? ?? 'snack',
      foodName: json['food_name'] as String,
      foodCategory: json['food_category'] as String?,
      firstTry: json['first_try'] as bool? ?? false,
      amount: json['amount'] as String?,
      reaction: json['reaction'] as String?,
      allergySymptom: json['allergy_symptom'] as String?,
      notes: json['notes'] as String?,
      photosUrls:
          (json['photos_urls'] as List?)?.map((e) => e as String).toList() ??
              [],
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class CheckupReport {
  final String id;
  final String babyId;
  final DateTime checkupDate;
  final String? checkupType;
  final String? hospital;
  final String? doctor;
  final double? weight;
  final double? height;
  final double? headCircumference;
  final String? vision;
  final String? hearing;
  final int? teethCount;
  final double? bloodHemoglobin;
  final double? bloodLead;
  final double? vitaminD;
  final String? developmentAssessment;
  final String? doctorAdvice;
  final DateTime? nextCheckupDate;
  final String? aiAnalysis;
  final String? reportFileUrl;
  final DateTime createdAt;

  CheckupReport({
    required this.id,
    required this.babyId,
    required this.checkupDate,
    this.checkupType,
    this.hospital,
    this.doctor,
    this.weight,
    this.height,
    this.headCircumference,
    this.vision,
    this.hearing,
    this.teethCount,
    this.bloodHemoglobin,
    this.bloodLead,
    this.vitaminD,
    this.developmentAssessment,
    this.doctorAdvice,
    this.nextCheckupDate,
    this.aiAnalysis,
    this.reportFileUrl,
    required this.createdAt,
  });

  factory CheckupReport.fromJson(Map<String, dynamic> json) {
    return CheckupReport(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      checkupDate: DateTime.parse(json['checkup_date']).date,
      checkupType: json['checkup_type'] as String?,
      hospital: json['hospital'] as String?,
      doctor: json['doctor'] as String?,
      weight: (json['weight'] as num?)?.toDouble(),
      height: (json['height'] as num?)?.toDouble(),
      headCircumference: (json['head_circumference'] as num?)?.toDouble(),
      vision: json['vision'] as String?,
      hearing: json['hearing'] as String?,
      teethCount: json['teeth_count'] as int?,
      bloodHemoglobin: (json['blood_hemoglobin'] as num?)?.toDouble(),
      bloodLead: (json['blood_lead'] as num?)?.toDouble(),
      vitaminD: (json['vitamin_d'] as num?)?.toDouble(),
      developmentAssessment: json['development_assessment'] as String?,
      doctorAdvice: json['doctor_advice'] as String?,
      nextCheckupDate: json['next_checkup_date'] != null
          ? DateTime.parse(json['next_checkup_date']).date
          : null,
      aiAnalysis: json['ai_analysis'] as String?,
      reportFileUrl: json['report_file_url'] as String?,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class LearningPlan {
  final String id;
  final String babyId;
  final String category;
  final String? subCategory;
  final String title;
  final String? description;
  final int? ageStartMonth;
  final int? ageEndMonth;
  final String difficulty;
  final String? materials;
  final String? instructions;
  final String? benefits;
  final String? tips;
  final String? frequency;
  final String? duration;
  final String? coverUrl;
  final DateTime createdAt;

  LearningPlan({
    required this.id,
    required this.babyId,
    required this.category,
    this.subCategory,
    required this.title,
    this.description,
    this.ageStartMonth,
    this.ageEndMonth,
    this.difficulty = 'easy',
    this.materials,
    this.instructions,
    this.benefits,
    this.tips,
    this.frequency,
    this.duration,
    this.coverUrl,
    required this.createdAt,
  });

  factory LearningPlan.fromJson(Map<String, dynamic> json) {
    return LearningPlan(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      category: json['category'] as String,
      subCategory: json['sub_category'] as String?,
      title: json['title'] as String,
      description: json['description'] as String?,
      ageStartMonth: json['age_start_month'] as int?,
      ageEndMonth: json['age_end_month'] as int?,
      difficulty: json['difficulty'] as String? ?? 'easy',
      materials: json['materials'] as String?,
      instructions: json['instructions'] as String?,
      benefits: json['benefits'] as String?,
      tips: json['tips'] as String?,
      frequency: json['frequency'] as String?,
      duration: json['duration'] as String?,
      coverUrl: json['cover_url'] as String?,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class MediaFile {
  final String id;
  final String babyId;
  final String fileType;
  final String fileUrl;
  final String? thumbnailUrl;
  final String? description;
  final List<String> aiTags;
  final DateTime? takenAt;
  final DateTime createdAt;

  MediaFile({
    required this.id,
    required this.babyId,
    required this.fileType,
    required this.fileUrl,
    this.thumbnailUrl,
    this.description,
    List<String>? aiTags,
    this.takenAt,
    required this.createdAt,
  }) : aiTags = aiTags ?? [];

  factory MediaFile.fromJson(Map<String, dynamic> json) {
    return MediaFile(
      id: json['id'] as String,
      babyId: json['baby_id'] as String,
      fileType: json['file_type'] as String,
      fileUrl: json['file_url'] as String,
      thumbnailUrl: json['thumbnail_url'] as String?,
      description: json['description'] as String?,
      aiTags:
          (json['ai_tags'] as List?)?.map((e) => e as String).toList() ?? [],
      takenAt:
          json['taken_at'] != null ? DateTime.parse(json['taken_at']) : null,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }
}

class AiConfig {
  final String id;
  final String? userId;
  final String name;
  final String provider;
  final String apiBaseUrl;
  final String? apiKey;
  final String model;
  final String? visionModel;
  final double temperature;
  final int maxTokens;
  final int timeoutSeconds;
  final bool isDefault;
  final bool isActive;
  final double? balanceAlert;
  final DateTime? lastUsedAt;
  final DateTime createdAt;
  final DateTime? updatedAt;

  AiConfig({
    required this.id,
    this.userId,
    required this.name,
    required this.provider,
    required this.apiBaseUrl,
    this.apiKey,
    required this.model,
    this.visionModel,
    this.temperature = 0.8,
    this.maxTokens = 2000,
    this.timeoutSeconds = 30,
    this.isDefault = false,
    this.isActive = true,
    this.balanceAlert,
    this.lastUsedAt,
    required this.createdAt,
    this.updatedAt,
  });

  factory AiConfig.fromJson(Map<String, dynamic> json) {
    return AiConfig(
      id: json['id'] as String,
      userId: json['user_id'] as String?,
      name: json['name'] as String,
      provider: json['provider'] as String,
      apiBaseUrl: json['api_base_url'] as String,
      apiKey: json['api_key'] as String?,
      model: json['model'] as String,
      visionModel: json['vision_model'] as String?,
      temperature: (json['temperature'] as num?)?.toDouble() ?? 0.8,
      maxTokens: json['max_tokens'] as int? ?? 2000,
      timeoutSeconds: json['timeout_seconds'] as int? ?? 30,
      isDefault: json['is_default'] as bool? ?? false,
      isActive: json['is_active'] as bool? ?? true,
      balanceAlert: (json['balance_alert'] as num?)?.toDouble(),
      lastUsedAt: json['last_used_at'] != null
          ? DateTime.parse(json['last_used_at'])
          : null,
      createdAt: DateTime.parse(json['created_at'] as String),
      updatedAt: json['updated_at'] != null
          ? DateTime.parse(json['updated_at'])
          : null,
    );
  }
}

// Pagination wrapper
class PaginatedResponse<T> {
  final int total;
  final int page;
  final int size;
  final List<T> items;

  PaginatedResponse(
      {required this.total,
      required this.page,
      required this.size,
      required this.items});

  factory PaginatedResponse.fromJson(
      Map<String, dynamic> json, T Function(dynamic) fromJson) {
    final items =
        (json['items'] as List?)?.map((e) => fromJson(e)).toList() ?? [];
    return PaginatedResponse(
      total: json['total'] as int? ?? items.length,
      page: json['page'] as int? ?? 1,
      size: json['size'] as int? ?? items.length,
      items: items,
    );
  }
}
