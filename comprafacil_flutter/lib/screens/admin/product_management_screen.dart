import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../models/product_models.dart';
import '../../providers/product_provider.dart';
import '../../providers/profile_provider.dart';
import '../../theme/app_theme.dart';
import 'product_form_screen.dart';

class ProductManagementScreen extends ConsumerWidget {
  const ProductManagementScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final productsAsync = ref.watch(productsProvider);
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');

    final profile = ref.watch(profileProvider).value;
    final hasPermission = profile?.hasPermission('manage_products') ?? false;

    return Scaffold(
      appBar: AppBar(title: const Text('Gerenciar Produtos')),
      body: !hasPermission
          ? const Center(child: Text('Você não tem permissão para gerenciar produtos.'))
          : productsAsync.when(
        data: (products) => ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: products.length,
          itemBuilder: (context, index) {
            final product = products[index];
            return Card(
              margin: const EdgeInsets.only(bottom: 12),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: ListTile(
                leading: product.imageUrl != null
                  ? Image.network(product.imageUrl!, width: 50, height: 50, fit: BoxFit.cover)
                  : const Icon(Icons.image_not_supported),
                title: Text(product.name),
                subtitle: Text(currencyFormat.format(product.price)),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.edit_outlined),
                      onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ProductFormScreen(product: product))),
                    ),
                    IconButton(
                      icon: const Icon(Icons.delete_outline, color: Colors.red),
                      onPressed: () => _confirmDelete(context, ref, product),
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
      floatingActionButton: FloatingActionButton(
        onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ProductFormScreen())),
        backgroundColor: AppTheme.primaryColor,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  void _confirmDelete(BuildContext context, WidgetRef ref, Product product) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Excluir Produto'),
        content: Text('Tem certeza que deseja excluir "${product.name}"?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(
            onPressed: () async {
              final db = ref.read(databaseServiceProvider);
              await db.deleteProduct(product.id);
              ref.refresh(productsProvider);
              if (context.mounted) Navigator.pop(context);
            },
            child: const Text('Excluir', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
