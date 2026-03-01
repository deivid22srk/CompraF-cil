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
  final _feePerKmController = TextEditingController();
  final _storeLatController = TextEditingController();
  final _storeLngController = TextEditingController();
  final _minVersionController = TextEditingController();
  final _downloadUrlController = TextEditingController();

  String _feeType = 'fixed';
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

      _feeType = config['delivery_fee_type'] ?? 'fixed';
      _deliveryFeeController.text = config['delivery_fee']?.toString() ?? '0.0';
      _feePerKmController.text = config['delivery_fee_per_km']?.toString() ?? '0.0';
      _storeLatController.text = config['store_latitude']?.toString() ?? '0.0';
      _storeLngController.text = config['store_longitude']?.toString() ?? '0.0';

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
              const Text('Cálculo de Entrega', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(height: 16),

              DropdownButtonFormField<String>(
                value: _feeType,
                decoration: const InputDecoration(labelText: 'Tipo de Taxa', border: OutlineInputBorder()),
                items: const [
                  DropdownMenuItem(value: 'fixed', child: Text('Taxa Fixa')),
                  DropdownMenuItem(value: 'per_km', child: Text('Taxa por KM')),
                ],
                onChanged: (val) {
                  if (val != null) {
                    setState(() => _feeType = val);
                    _update('delivery_fee_type', val);
                  }
                },
              ),

              const SizedBox(height: 16),

              if (_feeType == 'fixed') ...[
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _deliveryFeeController,
                        decoration: const InputDecoration(labelText: 'Valor da Taxa Fixa', border: OutlineInputBorder(), prefixText: 'R\$ '),
                        keyboardType: TextInputType.number,
                      ),
                    ),
                    const SizedBox(width: 16),
                    ElevatedButton(
                      onPressed: () {
                        final fee = double.tryParse(_deliveryFeeController.text);
                        if (fee != null) _update('delivery_fee', fee);
                      },
                      style: ElevatedButton.styleFrom(minimumSize: const Size(80, 50)),
                      child: const Text('Salvar'),
                    ),
                  ],
                ),
              ] else ...[
                TextField(
                  controller: _feePerKmController,
                  decoration: const InputDecoration(labelText: 'Valor por KM', border: OutlineInputBorder(), prefixText: 'R\$ '),
                  keyboardType: TextInputType.number,
                ),
                const SizedBox(height: 16),
                const Text('Localização da Loja (Para cálculo de distância)', style: TextStyle(fontSize: 12, color: Colors.grey)),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _storeLatController,
                        decoration: const InputDecoration(labelText: 'Latitude', border: OutlineInputBorder()),
                        keyboardType: TextInputType.number,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _storeLngController,
                        decoration: const InputDecoration(labelText: 'Longitude', border: OutlineInputBorder()),
                        keyboardType: TextInputType.number,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: () async {
                    final km = double.tryParse(_feePerKmController.text);
                    final lat = double.tryParse(_storeLatController.text);
                    final lng = double.tryParse(_storeLngController.text);
                    if (km != null) await _update('delivery_fee_per_km', km);
                    if (lat != null) await _update('store_latitude', lat);
                    if (lng != null) await _update('store_longitude', lng);
                  },
                  style: ElevatedButton.styleFrom(minimumSize: const Size(double.infinity, 50)),
                  child: const Text('SALVAR CONFIGURAÇÕES DE KM'),
                ),
              ],
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
                  // Ensure latest_version is updated for the update check to trigger
                  await _update('latest_version', _minVersionController.text);
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
          );
  }
}
