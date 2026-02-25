import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/user_models.dart';
import '../../providers/address_provider.dart';
import '../../providers/auth_provider.dart';

class AddressesScreen extends ConsumerWidget {
  const AddressesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final addressesAsync = ref.watch(addressesProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Meus Endereços'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showAddressDialog(context, ref),
          ),
        ],
      ),
      body: addressesAsync.when(
        data: (addresses) => addresses.isEmpty
            ? Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.location_off_outlined, size: 64, color: Colors.grey[400]),
                    const SizedBox(height: 16),
                    const Text('Nenhum endereço salvo.'),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: () => _showAddressDialog(context, ref),
                      child: const Text('Adicionar Endereço'),
                    ),
                  ],
                ),
              )
            : ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: addresses.length,
                itemBuilder: (context, index) {
                  final address = addresses[index];
                  return Card(
                    margin: const EdgeInsets.only(bottom: 12),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    child: ListTile(
                      title: Text(address.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(address.addressLine),
                          Text('Tel: ${address.phone}'),
                          if (address.receiverName != null && address.receiverName!.isNotEmpty)
                            Text('Destinatário: ${address.receiverName}'),
                        ],
                      ),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            icon: const Icon(Icons.edit_outlined),
                            onPressed: () => _showAddressDialog(context, ref, address: address),
                          ),
                          IconButton(
                            icon: const Icon(Icons.delete_outline, color: Colors.red),
                            onPressed: () => _confirmDelete(context, ref, address),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, s) => Center(child: Text('Erro: $e')),
      ),
    );
  }

  void _showAddressDialog(BuildContext context, WidgetRef ref, {Address? address}) {
    final nameController = TextEditingController(text: address?.name);
    final receiverController = TextEditingController(text: address?.receiverName);
    final phoneController = TextEditingController(text: address?.phone);
    final addressLineController = TextEditingController(text: address?.addressLine);

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(address == null ? 'Novo Endereço' : 'Editar Endereço'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                decoration: const InputDecoration(labelText: 'Apelido (ex: Casa, Trabalho)'),
              ),
              TextField(
                controller: receiverController,
                decoration: const InputDecoration(labelText: 'Nome do Destinatário'),
              ),
              TextField(
                controller: phoneController,
                decoration: const InputDecoration(labelText: 'Telefone'),
                keyboardType: TextInputType.phone,
              ),
              TextField(
                controller: addressLineController,
                decoration: const InputDecoration(labelText: 'Endereço Completo'),
                maxLines: 2,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          ElevatedButton(
            onPressed: () async {
              final user = ref.read(authProvider).value;
              if (user == null) return;

              if (nameController.text.isEmpty || addressLineController.text.isEmpty || phoneController.text.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Por favor, preencha os campos obrigatórios.')),
                );
                return;
              }

              final newAddress = Address(
                id: address?.id,
                userId: user.id,
                name: nameController.text,
                receiverName: receiverController.text,
                phone: phoneController.text,
                addressLine: addressLineController.text,
              );

              await ref.read(addressesProvider.notifier).saveAddress(newAddress);
              if (context.mounted) Navigator.pop(context);
            },
            child: const Text('Salvar'),
          ),
        ],
      ),
    );
  }

  void _confirmDelete(BuildContext context, WidgetRef ref, Address address) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Excluir Endereço'),
        content: const Text('Tem certeza que deseja excluir este endereço?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(
            onPressed: () async {
              await ref.read(addressesProvider.notifier).deleteAddress(address.id!);
              if (context.mounted) Navigator.pop(context);
            },
            child: const Text('Excluir', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
