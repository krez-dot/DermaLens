import 'dart:io';
import 'package:dio/dio.dart';
import '../models/detection_result.dart';
import '../utils/constants.dart';

class ApiService {
  late final Dio _dio;

  ApiService() {
    _dio = Dio(
      BaseOptions(
        baseUrl: AppConstants.baseUrl,
        connectTimeout: AppConstants.requestTimeout,
        receiveTimeout: AppConstants.requestTimeout,
        sendTimeout: AppConstants.requestTimeout,
      ),
    );
  }

  /// Sends an image to the YOLOv11 backend and returns detected skin conditions.
  Future<DetectionResponse> detectSkinCondition(File imageFile) async {
    final fileName = imageFile.path.split('/').last;
    final extension = fileName.split('.').last.toLowerCase();
    final mimeType = extension == 'png' ? 'image/png' : 'image/jpeg';

    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(
        imageFile.path,
        filename: fileName,
        contentType: DioMediaType.parse(mimeType),
      ),
    });

    try {
      final response = await _dio.post(
        AppConstants.detectEndpoint,
        data: formData,
      );
      return DetectionResponse.fromJson(
          response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      final message = _parseDioError(e);
      throw ApiException(message);
    }
  }

  /// Check if the backend is reachable.
  Future<bool> isBackendHealthy() async {
    try {
      final response = await _dio.get('/health');
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  String _parseDioError(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return 'Connection timed out. Make sure the backend is running.';
      case DioExceptionType.connectionError:
        return 'Cannot reach the server. Check your network connection.';
      case DioExceptionType.badResponse:
        final data = e.response?.data;
        if (data is Map && data.containsKey('detail')) {
          return data['detail'].toString();
        }
        return 'Server error (${e.response?.statusCode}).';
      default:
        return e.message ?? 'An unknown error occurred.';
    }
  }
}

class ApiException implements Exception {
  final String message;
  const ApiException(this.message);

  @override
  String toString() => message;
}
