import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/user_models.dart';
import '../../providers/product_provider.dart';
import '../../widgets/cached_avatar.dart';
import 'add_edit_admin_screen.dart';

class AdminManagementScreen extends ConsumerWidget {
  const AdminManagementScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Gerenciar Admins'),
      ),
      body: FutureBuilder<List<Profile>>(
        future: ref.read(databaseServiceProvider).getAdmins(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('Erro: ${snapshot.error}'));
          }
          final admins = snapshot.data ?? [];
          return ListView.builder(
            itemCount: admins.length,
            itemBuilder: (context, index) {
              final admin = admins[index];
              return ListTile(
                leading: CachedAvatar(url: admin.avatarUrl, radius: 20),
                title: Text(admin.fullName ?? 'Sem nome'),
                subtitle: Text(admin.email ?? 'Sem email'),
                trailing: admin.role == 'main_admin'
                    ? const Icon(Icons.star, color: Colors.amber)
                    : const Icon(Icons.edit),
                onTap: admin.role == 'main_admin'
                    ? null
                    : () async {
                        await Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => AddEditAdminScreen(admin: admin)),
                        );
                        // Refresh
                        (context as Element).markNeedsBuild();
                      },
              );
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const AddEditAdminScreen()),
        ),
        child: const Icon(Icons.person_add),
      ),
    );
  }
}
