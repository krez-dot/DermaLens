class BoundingBox {
  final double x1;
  final double y1;
  final double x2;
  final double y2;

  const BoundingBox({
    required this.x1,
    required this.y1,
    required this.x2,
    required this.y2,
  });

  factory BoundingBox.fromJson(Map<String, dynamic> json) {
    return BoundingBox(
      x1: (json['x1'] as num).toDouble(),
      y1: (json['y1'] as num).toDouble(),
      x2: (json['x2'] as num).toDouble(),
      y2: (json['y2'] as num).toDouble(),
    );
  }

  double get width => x2 - x1;
  double get height => y2 - y1;
  double get centerX => (x1 + x2) / 2;
  double get centerY => (y1 + y2) / 2;
}

class ConditionInfo {
  final String name;
  final String severity;
  final String description;
  final String recommendation;

  const ConditionInfo({
    required this.name,
    required this.severity,
    required this.description,
    required this.recommendation,
  });

  factory ConditionInfo.fromJson(Map<String, dynamic> json) {
    return ConditionInfo(
      name: json['name'] as String,
      severity: json['severity'] as String,
      description: json['description'] as String,
      recommendation: (json['recommendation'] as String?) ??
          'Please consult a dermatologist.',
    );
  }
}

class Detection {
  final BoundingBox bbox;
  final double confidence;
  final int classId;
  final ConditionInfo condition;
  final double imageWidth;
  final double imageHeight;

  const Detection({
    required this.bbox,
    required this.confidence,
    required this.classId,
    required this.condition,
    required this.imageWidth,
    required this.imageHeight,
  });

  factory Detection.fromJson(Map<String, dynamic> json) {
    return Detection(
      bbox: BoundingBox.fromJson(json['bbox'] as Map<String, dynamic>),
      confidence: (json['confidence'] as num).toDouble(),
      classId: json['class_id'] as int,
      condition: ConditionInfo.fromJson(json['condition'] as Map<String, dynamic>),
      imageWidth: (json['image_width'] as num).toDouble(),
      imageHeight: (json['image_height'] as num).toDouble(),
    );
  }

  String get confidencePercent =>
      '${(confidence * 100).toStringAsFixed(1)}%';
}

class DetectionResponse {
  final List<Detection> detections;
  final int totalCount;
  final String modelVersion;
  final bool isCustomModel;
  final double imageWidth;
  final double imageHeight;

  const DetectionResponse({
    required this.detections,
    required this.totalCount,
    required this.modelVersion,
    required this.isCustomModel,
    required this.imageWidth,
    required this.imageHeight,
  });

  factory DetectionResponse.fromJson(Map<String, dynamic> json) {
    return DetectionResponse(
      detections: (json['detections'] as List<dynamic>)
          .map((d) => Detection.fromJson(d as Map<String, dynamic>))
          .toList(),
      totalCount: json['total_count'] as int,
      modelVersion: json['model_version'] as String,
      isCustomModel: json['is_custom_model'] as bool,
      imageWidth: (json['image_width'] as num).toDouble(),
      imageHeight: (json['image_height'] as num).toDouble(),
    );
  }

  bool get isEmpty => detections.isEmpty;
  bool get isHealthy =>
      detections.isEmpty ||
      detections.every((d) => d.condition.severity.toLowerCase() == 'none');
}
