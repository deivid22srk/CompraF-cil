import 'dart:async';
import 'dart:ui';
import 'package:flutter_background_service/flutter_background_service.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'notification_service.dart';
import 'supabase_service.dart';

class BackgroundService {
  static Future<void> initialize() async {
    final service = FlutterBackgroundService();

    // Create notification channel for Android
    final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin = FlutterLocalNotificationsPlugin();

    const AndroidNotificationChannel channel = AndroidNotificationChannel(
      'background_service', // id
      'CompraFacil Service', // title
      description: 'Monitoramento de pedidos em tempo real', // description
      importance: Importance.max,
    );

    await flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(channel);

    // Small delay to ensure channel is created
    await Future.delayed(const Duration(milliseconds: 500));

    await service.configure(
      androidConfiguration: AndroidConfiguration(
        onStart: onStart,
        autoStart: true,
        isForegroundMode: true,
        notificationChannelId: 'background_service',
        initialNotificationTitle: 'CompraFacil em Execução',
        initialNotificationContent: 'Monitorando pedidos...',
        foregroundServiceNotificationId: 888,
        foregroundServiceTypes: [AndroidForegroundType.dataSync],
      ),
      iosConfiguration: IosConfiguration(
        autoStart: true,
        onForeground: onStart,
        onBackground: onIosBackground,
      ),
    );
  }

  @pragma('vm:entry-point')
  static Future<bool> onIosBackground(ServiceInstance service) async {
    return true;
  }

  @pragma('vm:entry-point')
  static void onStart(ServiceInstance service) async {
    DartPluginRegistrant.ensureInitialized();

    if (service is AndroidServiceInstance) {
      service.setAsForegroundService();
    }

    await SupabaseService.initialize();
    await NotificationService.initialize();
    final supabase = Supabase.instance.client;
    final prefs = await SharedPreferences.getInstance();

    service.on('stopService').listen((event) {
      service.stopSelf();
    });

    // Update notification periodically to keep it alive and healthy
    Timer.periodic(const Duration(minutes: 1), (timer) async {
      if (service is AndroidServiceInstance) {
        if (await service.isForegroundService()) {
          service.setForegroundNotificationInfo(
            title: "CompraFacil Ativo",
            content: "Monitorando seus pedidos...",
          );
        }
      }
    });

    // Monitor for changes in settings (we can use polling or simple event-based refresh if the app is alive)
    // For simplicity in background, we just load once or periodically check prefs.

    _setupRealtime(supabase, prefs);
  }

  static void _setupRealtime(SupabaseClient supabase, SharedPreferences prefs) {
    final userId = supabase.auth.currentUser?.id;
    final isAdminNotifEnabled = prefs.getBool('admin_notif_enabled') ?? true;
    final isUserNotifEnabled = prefs.getBool('user_notif_enabled') ?? true;
    final isAdminMode = prefs.getBool('is_admin_mode') ?? false;

    if (!isAdminNotifEnabled && !isUserNotifEnabled) return;

    supabase.channel('order_updates').onPostgresChanges(
      event: PostgresChangeEvent.all,
      schema: 'public',
      table: 'orders',
      callback: (payload) async {
        final eventType = payload.eventType;
        final newData = payload.newRecord;

        // Admin Notification: New Order (INSERT)
        if (isAdminNotifEnabled && isAdminMode && eventType == PostgresChangeEvent.insert) {
          await NotificationService.showNotification(
            id: DateTime.now().millisecondsSinceEpoch ~/ 1000,
            title: 'Novo Pedido Recebido!',
            body: 'Um novo pedido foi realizado no valor de R\$ ${newData['total_price']}',
          );
        }

        // User Notification: Status Update (UPDATE)
        if (isUserNotifEnabled && userId != null && eventType == PostgresChangeEvent.update) {
          if (newData['user_id'] == userId) {
            final oldData = payload.oldRecord;
            if (newData['status'] != oldData['status']) {
              await NotificationService.showNotification(
                id: DateTime.now().millisecondsSinceEpoch ~/ 1000,
                title: 'Atualização no seu Pedido',
                body: 'O status do seu pedido #${newData['id'].toString().substring(0, 8)} mudou para ${newData['status'].toString().toUpperCase()}',
              );
            }
          }
        }
      },
    ).subscribe();
  }
}
