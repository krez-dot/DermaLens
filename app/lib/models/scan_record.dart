import 'dart:convert';

class ScanRecord {
  final String id;
  final DateTime timestamp;
  final String imagePath;
  final List<String> detectedConditions;
  final List<String> severities;
  final int totalDetections;

  const ScanRecord({
    required this.id,
    required this.timestamp,
    required this.imagePath,
    required this.detectedConditions,
    required this.severities,
    required this.totalDetections,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'timestamp': timestamp.toIso8601String(),
        'imagePath': imagePath,
        'detectedConditions': detectedConditions,
        'severities': severities,
        'totalDetections': totalDetections,
      };

  factory ScanRecord.fromJson(Map<String, dynamic> json) {
    return ScanRecord(
      id: json['id'] as String,
      timestamp: DateTime.parse(json['timestamp'] as String),
      imagePath: json['imagePath'] as String,
      detectedConditions: (json['detectedConditions'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
      severities: (json['severities'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
      totalDetections: json['totalDetections'] as int,
    );
  }

  String toJsonString() => jsonEncode(toJson());

  static ScanRecord fromJsonString(String jsonString) =>
      ScanRecord.fromJson(jsonDecode(jsonString) as Map<String, dynamic>);

  String get highestSeverity {
    if (severities.contains('Severe')) return 'Severe';
    if (severities.contains('Moderate')) return 'Moderate';
    if (severities.contains('Mild')) return 'Mild';
    return 'None';
  }
}
