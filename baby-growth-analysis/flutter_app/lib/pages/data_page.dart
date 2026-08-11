/// 数据中心页面 - 子 Tab 切换（生长/喂养睡眠/疫苗/学习/体检）
library;

import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../pages/data/growth_subpage.dart';
import '../pages/data/feed_sleep_subpage.dart';
import '../pages/data/vaccine_subpage.dart';
import '../pages/data/learning_subpage.dart';
import '../pages/data/checkup_subpage.dart';

class DataPage extends StatefulWidget {
  const DataPage({super.key});

  @override
  State<DataPage> createState() => _DataPageState();
}

class _DataPageState extends State<DataPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 5, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(AppTheme.spacingLg),
              child: Text('数据中心',
                  style: Theme.of(context).textTheme.headlineMedium),
            ),
            TabBar(
              controller: _tabController,
              isScrollable: true,
              labelColor: AppTheme.primary,
              unselectedLabelColor: AppTheme.textSecondary,
              indicatorColor: AppTheme.primary,
              tabs: const [
                Tab(text: '生长曲线'),
                Tab(text: '喂养睡眠'),
                Tab(text: '疫苗'),
                Tab(text: '学习启蒙'),
                Tab(text: '体检'),
              ],
            ),
            Expanded(
              child: TabBarView(
                controller: _tabController,
                children: const [
                  GrowthSubpage(),
                  FeedSleepSubpage(),
                  VaccineSubpage(),
                  LearningSubpage(),
                  CheckupSubpage(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
