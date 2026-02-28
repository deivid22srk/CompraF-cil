import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/user_models.dart';
import '../../providers/product_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/cached_avatar.dart';

class AddEditAdminScreen extends ConsumerStatefulWidget {
  final Profile? admin;

  const AddEditAdminScreen({super.key, this.admin});

  @override
  ConsumerState<AddEditAdminScreen> createState() => _AddEditAdminScreenState();
}

class _AddEditAdminScreenState extends ConsumerState<AddEditAdminScreen> {
  final _emailController = TextEditingController();
  Profile? _foundUser;
  bool _isSearching = false;
  bool _isSaving = false;

  late Map<String, bool> _permissions;

  final Map<String, String> _permissionLabels = {
    'manage_products': 'Criar / Remover Produtos',
    'manage_categories': 'Criar / Remover Categorias',
    'manage_orders': 'Ver / Aceitar / Remover Pedidos',
    'manage_config': 'Editar Atualização / Taxa de Entrega',
  };

  @override
  void initState() {
    super.initState();
    if (widget.admin != null) {
      _foundUser = widget.admin;
      _emailController.text = widget.admin!.email ?? '';
      _permissions = Map<String, bool>.from(widget.admin!.permissions);
    } else {
      _permissions = {
        'manage_products': false,
        'manage_categories': false,
        'manage_orders': false,
        'manage_config': false,
      };
    }
  }

  Future<void> _searchUser() async {
    final email = _emailController.text.trim();
    if (email.isEmpty) return;

    setState(() => _isSearching = true);
    try {
      final user = await ref.read(databaseServiceProvider).getProfileByEmail(email);
      setState(() {
        _foundUser = user;
        if (user != null && user.role == 'admin') {
           _permissions = Map<String, bool>.from(user.permissions);
        }
      });
      if (user == null && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Usuário não encontrado.')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro na busca: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSearching = false);
    }
  }

  Future<void> _save() async {
    if (_foundUser == null) return;

    setState(() => _isSaving = true);
    try {
      await ref.read(databaseServiceProvider).updateAdminPermissions(
        _foundUser!.id,
        'admin',
        _permissions,
      );
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Permissões atualizadas!')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro ao salvar: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _removeAdmin() async {
     if (_foundUser == null) return;

     setState(() => _isSaving = true);
     try {
       await ref.read(databaseServiceProvider).updateAdminPermissions(
         _foundUser!.id,
         'user',
         {},
       );
       if (mounted) {
         Navigator.pop(context);
         ScaffoldMessenger.of(context).showSnackBar(
           const SnackBar(content: Text('Privilégios de admin removidos.')),
         );
       }
     } catch (e) {
       if (mounted) {
         ScaffoldMessenger.of(context).showSnackBar(
           SnackBar(content: Text('Erro ao remover: $e')),
         );
       }
     } finally {
       if (mounted) setState(() => _isSaving = false);
     }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.admin == null ? 'Adicionar Admin' : 'Editar Permissões'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (widget.admin == null) ...[
              const Text('Buscar usuário por email', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _emailController,
                      decoration: const InputDecoration(
                        hintText: 'exemplo@email.com',
                        border: OutlineInputBorder(),
                      ),
                      keyboardType: TextInputType.emailAddress,
                    ),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: _isSearching ? null : _searchUser,
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(50, 50),
                      padding: EdgeInsets.zero,
                    ),
                    child: _isSearching
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.search),
                  ),
                ],
              ),
              const SizedBox(height: 24),
            ],
            if (_foundUser != null) ...[
              Card(
                child: ListTile(
                  leading: CachedAvatar(url: _foundUser!.avatarUrl, radius: 20),
                  title: Text(_foundUser!.fullName ?? 'Sem nome'),
                  subtitle: Text(_foundUser!.email ?? ''),
                ),
              ),
              const SizedBox(height: 24),
              const Text('Permissões', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(height: 16),
              ..._permissionLabels.entries.map((entry) {
                return CheckboxListTile(
                  title: Text(entry.value),
                  value: _permissions[entry.key] ?? false,
                  activeColor: AppTheme.primaryColor,
                  onChanged: (val) {
                    setState(() => _permissions[entry.key] = val ?? false);
                  },
                );
              }),
              const SizedBox(height: 40),
              if (_isSaving)
                const Center(child: CircularProgressIndicator())
              else ...[
                ElevatedButton(
                  onPressed: _save,
                  child: const Text('SALVAR PERMISSÕES'),
                ),
                if (widget.admin != null) ...[
                  const SizedBox(height: 16),
                  TextButton(
                    onPressed: _removeAdmin,
                    child: const Text('REMOVER CARGO DE ADMIN', style: TextStyle(color: Colors.red)),
                  ),
                ],
              ],
            ],
          ],
        ),
      ),
    );
  }
}
