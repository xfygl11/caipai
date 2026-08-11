/// 生长曲线图表 - SVG 实现（无需额外包）
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/app_state.dart';
import '../../models/models.dart';
import '../../services/api_service.dart';
import '../../config/app_config.dart';

class GrowthSubpage extends StatefulWidget {
  const GrowthSubpage({super.key});

  @override
  State<GrowthSubpage> createState() => _GrowthSubpageState();
}

class _GrowthSubpageState extends State<GrowthSubpage> {
  String _selectedMetric = 'weight';
  List<Map<String, dynamic>>? _whoData;
  List<GrowthRecord>? _records;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    try {
      final api = ApiService();
      final state = context.read<AppState>();

      final curveData = await api.getGrowthCurve(
          babyId: state.babyId, metric: _selectedMetric);
      final items = curveData['data'] as List? ?? [];
      _records = items
          .map((e) => GrowthRecord.fromJson({
                ...e,
                'created_at': DateTime.now().toIso8601String(),
              }))
          .toList();

      final whoItems = curveData['who_percentiles'] as List? ?? [];
      _whoData = whoItems.cast<Map<String, dynamic>>();
    } catch (_) {}
    if (mounted) setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppTheme.spacingLg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Metric toggle
          Row(
            children: [
              _metricButton('体重 (kg)', 'weight'),
              const SizedBox(width: AppTheme.spacingSm),
              _metricButton('身高 (cm)', 'height'),
            ],
          ),
          const SizedBox(height: AppTheme.spacingLg),

          if (_loading)
            const Center(child: CircularProgressIndicator())
          else if (_records == null || _records!.isEmpty)
            Center(
              child: Column(
                children: [
                  Icon(Icons.trending_up,
                      size: 48,
                      color: AppTheme.textSecondary.withValues(alpha: 0.3)),
                  const SizedBox(height: AppTheme.spacingMd),
                  Text('暂无${_selectedMetric == 'weight' ? '体重' : '身高'}记录',
                      style: Theme.of(context).textTheme.bodySmall),
                ],
              ),
            )
          else
            Column(
              children: [
                // Chart
                Container(
                  width: double.infinity,
                  height: 250,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(AppTheme.radiusCard),
                  ),
                  child: _buildChart(),
                ),
                const SizedBox(height: AppTheme.spacingMd),

                // Latest record summary
                if (_records!.isNotEmpty) _latestRecordSummary(_records!.first),
              ],
            ),
        ],
      ),
    );
  }

  Widget _metricButton(String label, String metric) {
    final active = _selectedMetric == metric;
    return Expanded(
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: active ? AppTheme.primary : Colors.grey[200],
          foregroundColor: active ? Colors.white : AppTheme.textPrimary,
          padding: const EdgeInsets.symmetric(vertical: 10),
        ),
        onPressed: () {
          setState(() => _selectedMetric = metric);
          _loadData();
        },
        child: Text(label),
      ),
    );
  }

  Widget _buildChart() {
    if (_records == null || _records!.isEmpty || _whoData == null) {
      return const Center(
          child: Text('暂无数据', style: TextStyle(color: Colors.grey)));
    }

    final values = _records!.map((r) {
      final birth = DateTime.parse(AppConfig.babyBirthTime);
      final ageMonths =
          ((DateTime.now().difference(r.recordDate).inDays / 30.44)
                  .clamp(0, 24.0))
              .toDouble();
      double value = _selectedMetric == 'weight'
          ? (r.weight ?? 0).toDouble()
          : (r.height ?? 0).toDouble();
      return {'age': ageMonths, 'value': value};
    }).toList();

    // Find min/max
    final allValues = [...values.map((v) => v['value'] as double)];
    if (_whoData != null) {
      for (final w in _whoData!) {
        final key = 'p$_selectedMetric';
        for (final k in ['p3', 'p15', 'p50', 'p85', 'p97']) {
          if (w[k] != null) allValues.add(w[k] as double);
        }
      }
    }

    final minVal = allValues.isEmpty
        ? 0.0
        : (allValues.reduce((a, b) => a < b ? a : b) * 0.9).toDouble();
    final maxVal = allValues.isEmpty
        ? 10.0
        : (allValues.reduce((a, b) => a > b ? a : b) * 1.1).toDouble();

    return CustomPaint(
      size: Size.infinite,
      painter: _GrowthChartPainter(
        points: values,
        whoData: _whoData ?? [],
        metric: _selectedMetric,
        minVal: minVal,
        maxVal: maxVal,
        unit: _selectedMetric == 'weight' ? 'kg' : 'cm',
      ),
    );
  }

  Widget _latestRecordSummary(GrowthRecord record) {
    return Container(
      padding: const EdgeInsets.all(AppTheme.spacingMd),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppTheme.radiusCard),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _miniStat('最近测量', record.recordDate.toString().split(' ')[0]),
          _divider(),
          _miniStat('体重', '${record.weight?.toStringAsFixed(2) ?? '--'} kg'),
          _divider(),
          _miniStat('身高', '${record.height?.toStringAsFixed(1) ?? '--'} cm'),
        ],
      ),
    );
  }

  Widget _miniStat(String label, String value) {
    return Column(children: [
      Text(value, style: Theme.of(context).textTheme.titleMedium),
      Text(label, style: Theme.of(context).textTheme.bodySmall),
    ]);
  }

  Widget _divider() =>
      Container(width: 1, height: 30, color: Colors.grey.shade300);
}

class _GrowthChartPainter extends CustomPainter {
  final List<Map<String, dynamic>> points;
  final List<Map<String, dynamic>> whoData;
  final String metric;
  final double minVal;
  final double maxVal;
  final String unit;

  _GrowthChartPainter({
    required this.points,
    required this.whoData,
    required this.metric,
    required this.minVal,
    required this.maxVal,
    required this.unit,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (points.isEmpty) return;

    const padding = EdgeInsets.all(10);
    final chartWidth = size.width - padding.left - padding.right;
    final chartHeight = size.height - padding.top - padding.bottom;

    // Axes
    canvas.drawLine(
      Offset(padding.left, padding.top),
      Offset(padding.left, size.height - padding.bottom),
      Paint()
        ..color = Colors.grey.shade300
        ..strokeWidth = 1,
    );
    canvas.drawLine(
      Offset(padding.left, size.height - padding.bottom),
      Offset(size.width - padding.right, size.height - padding.bottom),
      Paint()
        ..color = Colors.grey.shade300
        ..strokeWidth = 1,
    );

    // Scale mapping
    double mapX(double ageMonths) {
      return padding.left + (ageMonths / 24) * chartWidth;
    }

    double mapY(double value) {
      return size.height -
          padding.bottom -
          ((value - minVal) / (maxVal - minVal)) * chartHeight;
    }

    // WHO percentile bands (background)
    if (whoData.isNotEmpty) {
      // P3 line
      Path p3Path = Path();
      Path p50Path = Path();
      Path p97Path = Path();

      bool firstP3 = true, firstP50 = true, firstP97 = true;

      for (final w in whoData) {
        final age = (w['age_months'] as num?)?.toDouble() ?? 0;
        final p3 = w['p3'] as double?;
        final p50 = w['p50'] as double?;
        final p97 = w['p97'] as double?;

        if (p3 != null) {
          if (firstP3) {
            p3Path.moveTo(mapX(age), mapY(p3));
            firstP3 = false;
          } else {
            p3Path.lineTo(mapX(age), mapY(p3));
          }
        }
        if (p50 != null) {
          if (firstP50) {
            p50Path.moveTo(mapX(age), mapY(p50));
            firstP50 = false;
          } else {
            p50Path.lineTo(mapX(age), mapY(p50));
          }
        }
        if (p97 != null) {
          if (firstP97) {
            p97Path.moveTo(mapX(age), mapY(p97));
            firstP97 = false;
          } else {
            p97Path.lineTo(mapX(age), mapY(p97));
          }
        }
      }

      // Draw WHO lines
      canvas.drawPath(
          p3Path,
          Paint()
            ..color = Colors.grey.shade300
            ..strokeWidth = 1
            ..style = PaintingStyle.stroke);
      canvas.drawPath(
          p50Path,
          Paint()
            ..color = Colors.grey.shade400
            ..strokeWidth = 1.5
            ..style = PaintingStyle.stroke);
      canvas.drawPath(
          p97Path,
          Paint()
            ..color = Colors.grey.shade300
            ..strokeWidth = 1
            ..style = PaintingStyle.stroke);
    }

    // Actual data points
    final dataPath = Path();
    bool firstData = true;

    for (final p in points) {
      final age = p['age_months'] as double? ?? 0;
      final value = p['value'] as double? ?? 0;
      final x = mapX(age);
      final y = mapY(value);

      if (firstData) {
        dataPath.moveTo(x, y);
        firstData = false;
      } else {
        dataPath.lineTo(x, y);
      }

      // Draw point
      canvas.drawCircle(
          Offset(x, y),
          4,
          Paint()
            ..color = AppTheme.primary
            ..style = PaintingStyle.fill);
      canvas.drawCircle(
          Offset(x, y),
          6,
          Paint()
            ..color = AppTheme.primary.withValues(alpha: 0.3)
            ..style = PaintingStyle.stroke);
    }

    // Draw connecting line
    if (!firstData) {
      canvas.drawPath(
          dataPath,
          Paint()
            ..color = AppTheme.primary
            ..strokeWidth = 2
            ..style = PaintingStyle.stroke);
    }

    // Axis labels
    final textPainter = TextPainter(
      textDirection: TextDirection.ltr,
    );

    // Y-axis labels
    for (int i = 0; i <= 4; i++) {
      final val = minVal + (maxVal - minVal) * i / 4;
      textPainter.text = TextSpan(
        text: val.toStringAsFixed(1),
        style: const TextStyle(fontSize: 10, color: Colors.grey),
      );
      textPainter.layout();
      textPainter.paint(canvas, Offset(2, mapY(val) - 5));
    }

    // X-axis label
    textPainter.text = const TextSpan(
        text: '月龄', style: TextStyle(fontSize: 10, color: Colors.grey));
    textPainter.layout();
    textPainter.paint(canvas,
        Offset(size.width / 2 - textPainter.width / 2, size.height - 4));

    // Unit label
    textPainter.text = TextSpan(
        text: unit, style: const TextStyle(fontSize: 10, color: Colors.grey));
    textPainter.layout();
    textPainter.paint(canvas, Offset(padding.left + 4, padding.top - 4));
  }

  @override
  bool shouldRepaint(covariant _GrowthChartPainter oldDelegate) {
    return oldDelegate.points != points || oldDelegate.whoData != whoData;
  }
}
