import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/scan_record.dart';
import '../utils/constants.dart';

class StorageService {
  Future<List<ScanRecord>> getScanHistory() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonList = prefs.getStringList(AppConstants.scanHistoryKey) ?? [];
    return jsonList.map((s) => ScanRecord.fromJsonString(s)).toList();
  }

  Future<void> saveScanRecord(ScanRecord record) async {
    final prefs = await SharedPreferences.getInstance();
    final existing = prefs.getStringList(AppConstants.scanHistoryKey) ?? [];

    // Insert newest first, cap at max items
    existing.insert(0, record.toJsonString());
    if (existing.length > AppConstants.maxHistoryItems) {
      existing.removeRange(AppConstants.maxHistoryItems, existing.length);
    }

    await prefs.setStringList(AppConstants.scanHistoryKey, existing);
  }

  Future<void> deleteScanRecord(String id) async {
    final prefs = await SharedPreferences.getInstance();
    final existing = prefs.getStringList(AppConstants.scanHistoryKey) ?? [];
    existing.removeWhere((s) {
      final map = jsonDecode(s) as Map<String, dynamic>;
      return map['id'] == id;
    });
    await prefs.setStringList(AppConstants.scanHistoryKey, existing);
  }

  Future<void> clearHistory() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(AppConstants.scanHistoryKey);
  }
}
