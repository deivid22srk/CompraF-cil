import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_background_service/flutter_background_service.dart';

class SettingsState {
  final bool adminNotifEnabled;
  final bool userNotifEnabled;
  final bool backgroundServiceEnabled;

  SettingsState({
    required this.adminNotifEnabled,
    required this.userNotifEnabled,
    required this.backgroundServiceEnabled,
  });

  SettingsState copyWith({
    bool? adminNotifEnabled,
    bool? userNotifEnabled,
    bool? backgroundServiceEnabled,
  }) {
    return SettingsState(
      adminNotifEnabled: adminNotifEnabled ?? this.adminNotifEnabled,
      userNotifEnabled: userNotifEnabled ?? this.userNotifEnabled,
      backgroundServiceEnabled: backgroundServiceEnabled ?? this.backgroundServiceEnabled,
    );
  }
}

final settingsProvider = StateNotifierProvider<SettingsNotifier, SettingsState>((ref) {
  return SettingsNotifier();
});

class SettingsNotifier extends StateNotifier<SettingsState> {
  SettingsNotifier() : super(SettingsState(
    adminNotifEnabled: true,
    userNotifEnabled: true,
    backgroundServiceEnabled: true,
  )) {
    _load();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    state = SettingsState(
      adminNotifEnabled: prefs.getBool('admin_notif_enabled') ?? true,
      userNotifEnabled: prefs.getBool('user_notif_enabled') ?? true,
      backgroundServiceEnabled: prefs.getBool('background_service_enabled') ?? true,
    );
  }

  Future<void> updateAdminNotif(bool value) async {
    state = state.copyWith(adminNotifEnabled: value);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('admin_notif_enabled', value);
    await _syncBackgroundService();
  }

  Future<void> updateUserNotif(bool value) async {
    state = state.copyWith(userNotifEnabled: value);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('user_notif_enabled', value);
    await _syncBackgroundService();
  }

  Future<void> updateBackgroundService(bool value) async {
    state = state.copyWith(backgroundServiceEnabled: value);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('background_service_enabled', value);
    await _syncBackgroundService();
  }

  Future<void> _syncBackgroundService() async {
    final service = FlutterBackgroundService();
    final isRunning = await service.isRunning();

    // Service should only run if the master switch is ON
    // AND at least one type of notification is enabled.
    final shouldBeRunning = state.backgroundServiceEnabled &&
                           (state.adminNotifEnabled || state.userNotifEnabled);

    if (shouldBeRunning) {
      if (!isRunning) {
        await service.startService();
      }
    } else {
      if (isRunning) {
        service.invoke('stopService');
      }
    }
  }
}
