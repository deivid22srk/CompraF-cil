import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../providers/order_provider.dart';
import '../../providers/admin_provider.dart';
import '../../providers/product_provider.dart';
import '../../providers/profile_provider.dart';
import '../../models/user_models.dart';
import '../../theme/app_theme.dart';
import 'order_details_screen.dart';

class OrdersScreen extends ConsumerWidget {
  const OrdersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isAdminMode = ref.watch(isAdminModeProvider);
    final ordersAsync = ref.watch(ordersProvider);
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final dateFormat = DateFormat('dd/MM/yyyy HH:mm');

    final profile = ref.watch(profileProvider).value;
    final hasOrderPermission = profile?.hasPermission('manage_orders') ?? false;

    final content = (isAdminMode && !hasOrderPermission)
          ? const Center(child: Text('Você não tem permissão para gerenciar pedidos.'))
          : ordersAsync.when(
        data: (orders) => orders.isEmpty
            ? const Center(child: Text('Você ainda não fez nenhum pedido.'))
            : ListView.builder(
                padding: const EdgeInsets.all(20),
                itemCount: orders.length,
                itemBuilder: (context, index) {
                  final Order order = orders[index];
                  return InkWell(
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => OrderDetailsScreen(order: order)),
                    ),
                    borderRadius: BorderRadius.circular(20),
                    child: Container(
                      margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Theme.of(context).cardColor,
                      borderRadius: BorderRadius.circular(20),
                      boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 10)],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Pedido #${order.id?.substring(0, 8)}',
                              style: const TextStyle(fontWeight: FontWeight.bold),
                            ),
                            Row(
                              children: [
                                _buildStatusBadge(order.status),
                                if (isAdminMode)
                                  IconButton(
                                    icon: const Icon(Icons.delete_outline, color: Colors.red, size: 20),
                                    onPressed: () => _confirmDelete(context, ref, order),
                                  ),
                              ],
                            ),
                          ],
                        ),
                        const Divider(height: 24),
                        if (isAdminMode) ...[
                          Text('Cliente: ${order.customerName ?? 'N/A'}', style: const TextStyle(fontWeight: FontWeight.bold)),
                          Row(
                            children: [
                              Text('WhatsApp: ${order.whatsapp}'),
                              const SizedBox(width: 8),
                              InkWell(
                                onTap: () async {
                                  String phone = order.whatsapp.replaceAll(RegExp(r'[^0-9]'), '');
                                  if (phone.length == 11 && !phone.startsWith('55')) {
                                    phone = '55$phone';
                                  }
                                  final whatsappUrl = 'https://wa.me/$phone';
                                  final uri = Uri.parse(whatsappUrl);
                                  if (await canLaunchUrl(uri)) {
                                    await launchUrl(uri, mode: LaunchMode.externalApplication);
                                  }
                                },
                                child: const Icon(Icons.message, color: Colors.green, size: 18),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                        ],
                        Text('Data: ${order.createdAt != null ? dateFormat.format(order.createdAt!) : '-'}'),
                        const SizedBox(height: 8),
                        Text(
                          'Total: ${currencyFormat.format(order.totalPrice)}',
                          style: const TextStyle(color: AppTheme.primaryColor, fontWeight: FontWeight.bold),
                        ),
                        ],
                      ),
                    ),
                  );
                },
              ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, s) => Center(child: Text('Erro: $e')),
      );

    if (isAdminMode) return content;

    return Scaffold(
      appBar: AppBar(title: const Text('Meus Pedidos')),
      body: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            color: Colors.amber[50],
            child: const Row(
              children: [
                Icon(Icons.info_outline, size: 16, color: Colors.amber),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'O histórico de pedidos é mantido por 30 dias.',
                    style: TextStyle(fontSize: 12, color: Colors.amber, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
          Expanded(child: content),
        ],
      ),
    );
  }

  void _confirmDelete(BuildContext context, WidgetRef ref, Order order) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Excluir Pedido'),
        content: Text('Deseja excluir o pedido #${order.id?.substring(0, 8)}?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(
            onPressed: () async {
              final db = ref.read(databaseServiceProvider);
              await db.deleteOrder(order.id!);
              ref.refresh(ordersProvider);
              if (context.mounted) {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Pedido excluído.')));
              }
            },
            child: const Text('Excluir', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusBadge(String status) {
    Color color;
    switch (status.toLowerCase()) {
      case 'pendente': color = Colors.orange; break;
      case 'aceito': color = Colors.blue; break;
      case 'em entrega': color = Colors.purple; break;
      case 'concluído': color = Colors.green; break;
      case 'cancelado': color = Colors.red; break;
      default: color = Colors.grey;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color),
      ),
      child: Text(
        status.toUpperCase(),
        style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.bold),
      ),
    );
  }
}
