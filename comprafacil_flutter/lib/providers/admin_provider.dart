import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

final isAdminModeProvider = StateNotifierProvider<AdminModeNotifier, bool>((ref) {
  return AdminModeNotifier();
});

class AdminModeNotifier extends StateNotifier<bool> {
  AdminModeNotifier() : super(false) {
    _load();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    state = prefs.getBool('is_admin_mode') ?? false;
  }

  Future<void> toggle(bool value) async {
    state = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('is_admin_mode', value);
  }
}
