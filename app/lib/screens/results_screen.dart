import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:uuid/uuid.dart';
import '../models/detection_result.dart';
import '../models/scan_record.dart';
import '../services/storage_service.dart';
import '../utils/theme.dart';
import '../widgets/bounding_box_overlay.dart';
import '../widgets/condition_card.dart';

class ResultsScreen extends StatefulWidget {
  const ResultsScreen({super.key});

  @override
  State<ResultsScreen> createState() => _ResultsScreenState();
}

class _ResultsScreenState extends State<ResultsScreen> {
  final _storage = StorageService();
  bool _saved = false;

  @override
  Widget build(BuildContext context) {
    final args =
        ModalRoute.of(context)!.settings.arguments as Map<String, dynamic>;
    final DetectionResponse result = args['result'] as DetectionResponse;
    final File image = args['image'] as File;

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Text('Analysis Results'),
        backgroundColor: AppTheme.primary,
        foregroundColor: Colors.white,
        actions: [
          if (!_saved)
            IconButton(
              icon: const Icon(Icons.save_alt_rounded),
              tooltip: 'Save to History',
              onPressed: () => _save(result, image),
            ),
          if (_saved)
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 12),
              child: Icon(Icons.check_circle_rounded, color: Colors.white),
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          _buildImageWithBoxes(result, image),
          const SizedBox(height: 20),
          _buildSummaryBanner(result),
          const SizedBox(height: 20),
          if (result.isEmpty)
            _buildNoDetectionCard()
          else ...[
            Text(
              'Detected Conditions (${result.totalCount})',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppTheme.textPrimary,
              ),
            ),
            const SizedBox(height: 8),
            ...result.detections.asMap().entries.map(
                  (e) => ConditionCard(detection: e.value, index: e.key)
                      .animate(delay: (e.key * 80).ms)
                      .slideX(begin: 0.2, duration: 400.ms)
                      .fade(),
                ),
          ],
          const SizedBox(height: 20),
          _buildModelBadge(result),
          const SizedBox(height: 20),
          _buildActions(context, result, image),
          const SizedBox(height: 20),
          _buildDisclaimer(),
        ],
      ),
    );
  }

  Widget _buildImageWithBoxes(DetectionResponse result, File image) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(18),
      child: LayoutBuilder(
        builder: (context, constraints) {
          return AspectRatio(
            aspectRatio: result.imageWidth / result.imageHeight,
            child: Stack(
              fit: StackFit.expand,
              children: [
                Image.file(image, fit: BoxFit.fill),
                if (result.detections.isNotEmpty)
                  BoundingBoxOverlay(
                    detections: result.detections,
                    imageWidth: result.imageWidth,
                    imageHeight: result.imageHeight,
                  ),
              ],
            ),
          );
        },
      ),
    )
        .animate()
        .scale(begin: const Offset(0.95, 0.95), duration: 400.ms)
        .fade();
  }

  Widget _buildSummaryBanner(DetectionResponse result) {
    final isHealthy = result.isHealthy;
    final color = isHealthy ? AppTheme.success : AppTheme.warning;
    final icon = isHealthy
        ? Icons.check_circle_rounded
        : Icons.warning_amber_rounded;
    final text = isHealthy
        ? 'No significant conditions detected'
        : '${result.totalCount} condition${result.totalCount > 1 ? 's' : ''} detected';

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color.withAlpha(20),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: color.withAlpha(80)),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 28),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: GoogleFonts.poppins(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: AppTheme.textPrimary,
              ),
            ),
          ),
        ],
      ),
    ).animate(delay: 200.ms).fade().slideY(begin: 0.2);
  }

  Widget _buildNoDetectionCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            const Icon(Icons.sentiment_satisfied_alt_rounded,
                size: 56, color: AppTheme.success),
            const SizedBox(height: 12),
            Text(
              'Skin looks healthy!',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppTheme.textPrimary,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'No significant skin conditions were detected. '
              'Keep up with your skincare routine and sun protection.',
              textAlign: TextAlign.center,
              style: GoogleFonts.poppins(
                fontSize: 13,
                color: AppTheme.textSecondary,
                height: 1.5,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildModelBadge(DetectionResponse result) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration: BoxDecoration(
            color: AppTheme.primary.withAlpha(20),
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.smart_toy_outlined,
                  color: AppTheme.primary, size: 14),
              const SizedBox(width: 6),
              Text(
                result.modelVersion +
                    (result.isCustomModel ? ' (Custom)' : ' (Demo)'),
                style: GoogleFonts.poppins(
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.primary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildActions(
      BuildContext context, DetectionResponse result, File image) {
    return Column(
      children: [
        if (!_saved)
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => _save(result, image),
              icon: const Icon(Icons.save_alt_rounded),
              label: const Text('Save to History'),
            ),
          ),
        const SizedBox(height: 10),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: () => Navigator.pushNamedAndRemoveUntil(
              context,
              '/scan',
              (route) => route.settings.name == '/home',
            ),
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Scan Another'),
          ),
        ),
      ],
    );
  }

  Widget _buildDisclaimer() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8E1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.warning.withAlpha(80)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.info_outline_rounded,
              color: AppTheme.warning, size: 18),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'Results are AI-generated and for informational purposes only. '
              'Always consult a licensed dermatologist for medical advice.',
              style: GoogleFonts.poppins(
                fontSize: 11,
                color: AppTheme.textSecondary,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _save(DetectionResponse result, File image) async {
    final record = ScanRecord(
      id: const Uuid().v4(),
      timestamp: DateTime.now(),
      imagePath: image.path,
      detectedConditions: result.detections
          .map((d) => d.condition.name)
          .toList(),
      severities: result.detections
          .map((d) => d.condition.severity)
          .toList(),
      totalDetections: result.totalCount,
    );
    await _storage.saveScanRecord(record);
    if (!mounted) return;
    setState(() => _saved = true);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Scan saved to history',
          style: GoogleFonts.poppins(),
        ),
        backgroundColor: AppTheme.success,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }
}
