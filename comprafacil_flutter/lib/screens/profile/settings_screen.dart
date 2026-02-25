import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/settings_provider.dart';
import '../../providers/profile_provider.dart';
import '../../theme/app_theme.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsProvider);
    final profile = ref.watch(profileProvider).value;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Configurações'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildSectionTitle('Notificações'),
            Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.notifications_outlined),
                    title: const Text('Notificações de Pedido'),
                    subtitle: const Text('Receba atualizações sobre seus pedidos'),
                    trailing: Switch(
                      value: settings.userNotifEnabled,
                      onChanged: (val) => ref.read(settingsProvider.notifier).updateUserNotif(val),
                      activeColor: AppTheme.primaryColor,
                    ),
                  ),
                  if (profile?.role == 'admin') ...[
                    const Divider(indent: 50),
                    ListTile(
                      leading: const Icon(Icons.notifications_active_outlined),
                      title: const Text('Notificações de Novos Pedidos (ADM)'),
                      subtitle: const Text('Alertas de novos pedidos realizados'),
                      trailing: Switch(
                        value: settings.adminNotifEnabled,
                        onChanged: (val) => ref.read(settingsProvider.notifier).updateAdminNotif(val),
                        activeColor: AppTheme.primaryColor,
                      ),
                    ),
                  ],
                  const Divider(indent: 50),
                  ListTile(
                    leading: const Icon(Icons.run_circle_outlined),
                    title: const Text('Serviço de Verificação'),
                    subtitle: const Text('Mantém o app monitorando pedidos em segundo plano'),
                    trailing: Switch(
                      value: settings.backgroundServiceEnabled,
                      onChanged: (val) => ref.read(settingsProvider.notifier).updateBackgroundService(val),
                      activeColor: AppTheme.primaryColor,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Text(
                'Nota: O Serviço de Verificação é necessário para receber notificações mesmo quando o aplicativo estiver fechado.',
                style: TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 16, bottom: 8, top: 16),
      child: Text(
        title.toUpperCase(),
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          color: Colors.grey,
          letterSpacing: 1.2,
        ),
      ),
    );
  }
}
