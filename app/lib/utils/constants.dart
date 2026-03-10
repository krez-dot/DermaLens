class AppConstants {
  // API
  // Change this to your backend's IP or domain when deploying
  static const String baseUrl = 'http://10.0.2.2:8000'; // Android emulator localhost
  static const String detectEndpoint = '/api/v1/detect';
  static const Duration requestTimeout = Duration(seconds: 60);

  // Storage keys
  static const String scanHistoryKey = 'scan_history';

  // App info
  static const String appName = 'DermaLens';
  static const String appVersion = '1.0.0';
  static const String disclaimer =
      'DermaLens is for informational purposes only and does not provide '
      'medical advice. Always consult a qualified dermatologist for diagnosis '
      'and treatment.';

  // Limits
  static const int maxHistoryItems = 50;
  static const int imageQuality = 85;
}
