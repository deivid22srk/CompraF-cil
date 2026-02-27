import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/product_provider.dart';
import '../../providers/admin_provider.dart';
import '../../providers/profile_provider.dart';

class AdminSettingsScreen extends ConsumerStatefulWidget {
  const AdminSettingsScreen({super.key});

  @override
  ConsumerState<AdminSettingsScreen> createState() => _AdminSettingsScreenState();
}

class _AdminSettingsScreenState extends ConsumerState<AdminSettingsScreen> {
  final _deliveryFeeController = TextEditingController();
  final _minVersionController = TextEditingController();
  final _downloadUrlController = TextEditingController();
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadConfig();
  }

  Future<void> _loadConfig() async {
    setState(() => _isLoading = true);
    try {
      final db = ref.read(databaseServiceProvider);
      final config = await db.getAppConfig();
      _deliveryFeeController.text = config['delivery_fee']?.toString() ?? '0.0';
      _minVersionController.text = config['min_version'] ?? '1.0';
      _downloadUrlController.text = config['download_url'] ?? '';
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _update(String key, dynamic value) async {
    setState(() => _isLoading = true);
    try {
      final db = ref.read(databaseServiceProvider);
      await db.updateAppConfig(key, value);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Configuração atualizada!')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erro: $e')));
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profile = ref.watch(profileProvider).value;
    final hasPermission = profile?.hasPermission('manage_config') ?? false;

    return _isLoading
        ? const Center(child: CircularProgressIndicator())
        : !hasPermission
          ? const Center(child: Text('Você não tem permissão para gerenciar configurações.'))
          : ListView(
            padding: const EdgeInsets.all(24),
            children: [
              const Text('Taxa de Entrega', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _deliveryFeeController,
                      decoration: const InputDecoration(border: OutlineInputBorder(), prefixText: 'R\$ '),
                      keyboardType: TextInputType.number,
                    ),
                  ),
                  const SizedBox(width: 16),
                  ElevatedButton(
                    onPressed: () {
                      final fee = double.tryParse(_deliveryFeeController.text);
                      if (fee != null) {
                        _update('delivery_fee', fee);
                      }
                    },
                    style: ElevatedButton.styleFrom(minimumSize: const Size(80, 50)),
                    child: const Text('Salvar'),
                  ),
                ],
              ),
              const SizedBox(height: 40),
              const Text('Versão Mínima e Download', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(height: 16),
              TextField(
                controller: _minVersionController,
                decoration: const InputDecoration(labelText: 'Versão Mínima (ex: 1.1)', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _downloadUrlController,
                decoration: const InputDecoration(labelText: 'URL de Download', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () async {
                  await _update('min_version', _minVersionController.text);
                  await _update('download_url', _downloadUrlController.text);
                },
                child: const Text('ENVIAR ATUALIZAÇÃO'),
              ),
              const SizedBox(height: 40),
              const Divider(),
              const SizedBox(height: 20),
              Center(
                child: TextButton.icon(
                  onPressed: () => ref.read(isAdminModeProvider.notifier).toggle(false),
                  icon: const Icon(Icons.exit_to_app),
                  label: const Text('Sair do Modo Admin'),
                ),
              ),
            ],
          ),
    );
  }
}
