/// 设置页面 - 宝宝信息 + AI 配置
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../providers/app_state.dart';
import '../services/api_service.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('设置'), elevation: 0),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppTheme.spacingLg),
        child: Column(
          children: [
            // Baby Info Card
            _babyInfoCard(context),
            const SizedBox(height: AppTheme.spacingMd),

            // AI Config Card
            _aiConfigCard(context),
            const SizedBox(height: AppTheme.spacingMd),

            // About
            Container(
              padding: const EdgeInsets.all(AppTheme.spacingLg),
              decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
              child: Column(
                children: [
                  Row(children: [
                    const Icon(Icons.info_outline,
                        color: AppTheme.textSecondary),
                    const SizedBox(width: 12),
                    Text('关于', style: Theme.of(context).textTheme.titleSmall)
                  ]),
                  const SizedBox(height: AppTheme.spacingSm),
                  const Text('成长印记 v1.0.0', textAlign: TextAlign.center),
                  Text('用 AI 记录宝宝每一个珍贵瞬间',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodySmall),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _babyInfoCard(BuildContext context) {
    final state = context.watch<AppState>();
    final baby = state.baby;

    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            const Icon(Icons.child_care, color: AppTheme.primary),
            const SizedBox(width: 8),
            Text('宝宝信息', style: Theme.of(context).textTheme.titleSmall)
          ]),
          const SizedBox(height: AppTheme.spacingMd),
          if (baby != null) ...[
            _infoRow('名字', baby.name),
            _infoRow('性别', baby.gender == 'female' ? '女' : '男'),
            _infoRow('出生日期',
                '${baby.birthTime.year}-${baby.birthTime.month.toString().padLeft(2, '0')}-${baby.birthTime.day.toString().padLeft(2, '0')}'),
            _infoRow('年龄', baby.age.text),
          ],
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(children: [
        Text('$label: ',
            style:
                const TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
        Expanded(child: Text(value, style: const TextStyle(fontSize: 14))),
      ]),
    );
  }

  Widget _aiConfigCard(BuildContext context) {
    final state = context.watch<AppState>();
    final configs = state.aiConfigs;

    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(AppTheme.radiusCard)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            const Icon(Icons.auto_awesome, color: AppTheme.aiColor),
            const SizedBox(width: 8),
            Text('AI 配置管理', style: Theme.of(context).textTheme.titleSmall)
          ]),
          const SizedBox(height: AppTheme.spacingMd),
          if (configs.isEmpty)
            Center(
                child: Text('尚未配置 AI 服务商',
                    style: Theme.of(context).textTheme.bodySmall))
          else ...[
            Text(
                '当前默认: ${configs.where((c) => c.isDefault).map((c) => c.name).firstOrNull ?? configs.first.name}',
                style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: AppTheme.spacingSm),
            ...configs.map((c) => _configTile(c, context)),
          ],
          const SizedBox(height: AppTheme.spacingMd),
          ElevatedButton.icon(
            onPressed: () => Navigator.pushNamed(context, '/ai-config'),
            icon: const Icon(Icons.add),
            label: const Text('添加 AI 配置'),
          ),
        ],
      ),
    );
  }

  Widget _configTile(dynamic config, BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
        color: config.isDefault
            ? AppTheme.aiColor.withValues(alpha: 0.1)
            : Colors.grey.shade50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
            color: config.isDefault ? AppTheme.aiColor : Colors.transparent),
      ),
      child: Row(
        children: [
          Icon(Icons.auto_awesome,
              color:
                  config.isDefault ? AppTheme.aiColor : AppTheme.textSecondary,
              size: 20),
          const SizedBox(width: AppTheme.spacingMd),
          Expanded(
              child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(config.name,
                  style: const TextStyle(fontWeight: FontWeight.w600)),
              Text('${config.provider} · ${config.model}',
                  style: Theme.of(context).textTheme.bodySmall),
            ],
          )),
          if (!config.isDefault)
            IconButton(
                icon: const Icon(Icons.star_border, size: 20),
                onPressed: () async {
                  await ApiService().setDefaultConfig(config.id);
                  if (context.mounted) context.read<AppState>().loadAiConfigs();
                }),
        ],
      ),
    );
  }
}
