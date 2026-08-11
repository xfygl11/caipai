/// 成长印记 APP 入口文件
library;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'theme/app_theme.dart';
import 'providers/app_state.dart';
import 'services/api_service.dart';
import 'pages/home_page.dart';
import 'pages/timeline_page.dart';
import 'pages/ai_assistant_page.dart';
import 'pages/data_page.dart';
import 'pages/settings_page.dart';
import 'pages/modals/feeding_timer_modal.dart';
import 'pages/modals/sleep_tracker_modal.dart';
import 'pages/modals/growth_record_modal.dart';
import 'pages/modals/milestone_modal.dart';
import 'pages/modals/food_record_modal.dart';
import 'pages/modals/checkup_report_modal.dart';
import 'pages/modals/photo_upload_modal.dart';
import 'pages/modals/ai_auto_record_modal.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.dark,
  ));
  runApp(const BabyGrowthApp());
}

class BabyGrowthApp extends StatelessWidget {
  const BabyGrowthApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<ApiService>(create: (_) => ApiService()),
        ChangeNotifierProxyProvider<ApiService, AppState>(
          create: (ctx) => AppState(ApiService()),
          update: (ctx, apiService, prev) =>
              (prev ?? AppState(apiService))..apiService = apiService,
        ),
      ],
      child: MaterialApp(
        title: '成长印记',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.theme,
        home: const MainScreen(),
        routes: {
          '/modal/feeding': (_) => const FeedingTimerModal(),
          '/modal/sleep': (_) => const SleepTrackerModal(),
          '/modal/growth': (_) => const GrowthRecordModal(),
          '/modal/milestone': (_) => const MilestoneModal(),
          '/modal/food': (_) => const FoodRecordModal(),
          '/modal/checkup': (_) => const CheckupReportModal(),
          '/modal/photo': (_) => const PhotoUploadModal(),
          '/modal/voice': (_) => const AiAutoRecordModal(),
        },
      ),
    );
  }
}

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;

  final List<Widget> _pages = const [
    HomePage(),
    TimelinePage(),
    AiAssistantPage(),
    DataPage(),
    SettingsPage(),
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().loadAllData();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _currentIndex, children: _pages),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (i) => setState(() => _currentIndex = i),
        type: BottomNavigationBarType.fixed,
        selectedItemColor: AppTheme.primary,
        unselectedItemColor: Colors.grey,
        items: const [
          BottomNavigationBarItem(
              icon: Icon(Icons.home_outlined),
              activeIcon: Icon(Icons.home),
              label: '首页'),
          BottomNavigationBarItem(
              icon: Icon(Icons.timeline_outlined),
              activeIcon: Icon(Icons.timeline),
              label: '时间轴'),
          BottomNavigationBarItem(
              icon: Icon(Icons.psychology_outlined),
              activeIcon: Icon(Icons.auto_awesome),
              label: 'AI 助手'),
          BottomNavigationBarItem(
              icon: Icon(Icons.bar_chart_outlined),
              activeIcon: Icon(Icons.bar_chart),
              label: '数据中心'),
          BottomNavigationBarItem(
              icon: Icon(Icons.settings_outlined),
              activeIcon: Icon(Icons.settings),
              label: '设置'),
        ],
      ),
    );
  }
}
