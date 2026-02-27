import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/admin_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/update_dialog.dart';
import '../../services/update_service.dart';
import 'product_management_screen.dart';
import 'category_management_screen.dart';
import 'admin_settings_screen.dart';
import 'admin_management_screen.dart';
import '../profile/profile_screen.dart';
import '../../providers/profile_provider.dart';
import '../orders/orders_screen.dart';

class AdminHomeScreen extends ConsumerStatefulWidget {
  const AdminHomeScreen({super.key});

  @override
  ConsumerState<AdminHomeScreen> createState() => _AdminHomeScreenState();
}

class _AdminHomeScreenState extends ConsumerState<AdminHomeScreen> {
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    _checkForUpdates();
  }

  Future<void> _checkForUpdates() async {
    // Small delay to ensure the UI is ready
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;

    final updateInfo = await UpdateService.checkForUpdate();
    if (updateInfo != null && mounted) {
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => UpdateDialog(
          latestVersion: updateInfo['latestVersion'],
          downloadUrl: updateInfo['downloadUrl'],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final screens = [
      const AdminDashboard(),
      const OrdersScreen(), // Reuse orders screen for admin (they can see all orders anyway via Supabase if role is admin)
      const AdminSettingsScreen(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Painel Administrativo'),
        actions: [
          IconButton(
            icon: const Icon(Icons.exit_to_app),
            onPressed: () => ref.read(isAdminModeProvider.notifier).toggle(false),
            tooltip: 'Sair do Modo ADM',
          ),
        ],
      ),
      body: screens[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        selectedItemColor: AppTheme.primaryColor,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.dashboard), label: 'Painel'),
          BottomNavigationBarItem(icon: Icon(Icons.receipt_long), label: 'Pedidos'),
          BottomNavigationBarItem(icon: Icon(Icons.settings), label: 'Ajustes'),
        ],
      ),
    );
  }
}

class AdminDashboard extends ConsumerWidget {
  const AdminDashboard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(profileProvider).value;
    final isMainAdmin = profile?.role == 'main_admin';

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        if (profile?.hasPermission('manage_products') ?? false) ...[
          _buildAdminCard(
            context,
            title: 'Produtos',
            subtitle: 'Gerenciar catálogo de produtos',
            icon: Icons.inventory_2,
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ProductManagementScreen())),
          ),
          const SizedBox(height: 16),
        ],
        if (profile?.hasPermission('manage_categories') ?? false) ...[
          _buildAdminCard(
            context,
            title: 'Categorias',
            subtitle: 'Adicionar ou remover categorias',
            icon: Icons.category,
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CategoryManagementScreen())),
          ),
          const SizedBox(height: 16),
        ],
        if (profile?.hasPermission('manage_config') ?? false) ...[
          _buildAdminCard(
            context,
            title: 'Configurações do App',
            subtitle: 'Taxa de entrega e atualizações',
            icon: Icons.app_settings_alt,
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AdminSettingsScreen())),
          ),
          const SizedBox(height: 16),
        ],
        if (isMainAdmin) ...[
          _buildAdminCard(
            context,
            title: 'Administradores',
            subtitle: 'Gerenciar permissões e novos admins',
            icon: Icons.admin_panel_settings,
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AdminManagementScreen())),
          ),
          const SizedBox(height: 16),
        ],
      ],
    );
  }

  Widget _buildAdminCard(BuildContext context, {required String title, required String subtitle, required IconData icon, required VoidCallback onTap}) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Theme.of(context).cardColor,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10)],
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.primaryColor.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: AppTheme.primaryColor),
            ),
            const SizedBox(width: 20),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  Text(subtitle, style: const TextStyle(color: Colors.grey, fontSize: 12)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: Colors.grey),
          ],
        ),
      ),
    );
  }
}
