import 'package:flutter/material.dart';
import '../models/detection_result.dart';
import '../utils/theme.dart';

class BoundingBoxOverlay extends StatelessWidget {
  final List<Detection> detections;
  final double imageWidth;
  final double imageHeight;

  const BoundingBoxOverlay({
    super.key,
    required this.detections,
    required this.imageWidth,
    required this.imageHeight,
  });

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _BoundingBoxPainter(
        detections: detections,
        imageWidth: imageWidth,
        imageHeight: imageHeight,
      ),
    );
  }
}

class _BoundingBoxPainter extends CustomPainter {
  final List<Detection> detections;
  final double imageWidth;
  final double imageHeight;

  _BoundingBoxPainter({
    required this.detections,
    required this.imageWidth,
    required this.imageHeight,
  });

  @override
  void paint(Canvas canvas, Size size) {
    for (final detection in detections) {
      final color = AppTheme.severityColor(detection.condition.severity);

      // Scale bounding box from image coords to display coords
      final scaleX = size.width / imageWidth;
      final scaleY = size.height / imageHeight;

      final rect = Rect.fromLTRB(
        detection.bbox.x1 * scaleX,
        detection.bbox.y1 * scaleY,
        detection.bbox.x2 * scaleX,
        detection.bbox.y2 * scaleY,
      );

      // Draw box
      final boxPaint = Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.5;
      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, const Radius.circular(4)),
        boxPaint,
      );

      // Draw label background
      const labelPadding = 4.0;
      final label =
          '${detection.condition.name} ${detection.confidencePercent}';
      final textPainter = TextPainter(
        text: TextSpan(
          text: label,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 11,
            fontWeight: FontWeight.w600,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();

      final labelWidth = textPainter.width + labelPadding * 2;
      final labelHeight = textPainter.height + labelPadding * 2;

      // Position label above box, clamp to canvas bounds
      double labelTop = rect.top - labelHeight;
      if (labelTop < 0) labelTop = rect.top;
      double labelLeft = rect.left;
      if (labelLeft + labelWidth > size.width) {
        labelLeft = size.width - labelWidth;
      }

      final labelRect = Rect.fromLTWH(labelLeft, labelTop, labelWidth, labelHeight);
      final bgPaint = Paint()..color = color;
      canvas.drawRRect(
        RRect.fromRectAndCorners(
          labelRect,
          topLeft: const Radius.circular(4),
          topRight: const Radius.circular(4),
          bottomRight: const Radius.circular(4),
        ),
        bgPaint,
      );

      textPainter.paint(
        canvas,
        Offset(labelLeft + labelPadding, labelTop + labelPadding),
      );
    }
  }

  @override
  bool shouldRepaint(covariant _BoundingBoxPainter oldDelegate) =>
      oldDelegate.detections != detections;
}
