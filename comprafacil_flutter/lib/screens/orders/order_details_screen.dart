import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../models/user_models.dart';
import '../../providers/order_provider.dart';
import '../../providers/admin_provider.dart';
import '../../providers/product_provider.dart';
import '../../theme/app_theme.dart';
import '../admin/delivery_map_screen.dart';

class OrderDetailsScreen extends ConsumerWidget {
  final Order order;

  const OrderDetailsScreen({super.key, required this.order});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isAdminMode = ref.watch(isAdminModeProvider);
    final historyAsync = ref.watch(orderStatusHistoryProvider(order.id ?? ''));
    final itemsAsync = ref.watch(orderItemsProvider(order.id ?? ''));
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final dateFormat = DateFormat('dd/MM/yyyy HH:mm');

    return Scaffold(
      appBar: AppBar(title: Text('Pedido #${order.id?.substring(0, 8)}')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildInfoCard(context, currencyFormat, dateFormat),
            const SizedBox(height: 24),
            const Text('Itens do Pedido', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            itemsAsync.when(
              data: (items) => Column(
                children: items.map((item) {
                  final product = item['products'];
                  return ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text('${item['quantity']}x ${product['name']}'),
                    subtitle: item['selected_variations'] != null
                        ? Text((item['selected_variations'] as Map).values.join(', '))
                        : null,
                    trailing: Text(currencyFormat.format(item['price_at_time'] * item['quantity'])),
                  );
                }).toList(),
              ),
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, s) => Text('Erro ao carregar itens: $e'),
            ),
            if (isAdminMode) ...[
              const SizedBox(height: 24),
              const Text('Gerenciar Pedido', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: order.status.toLowerCase(),
                decoration: const InputDecoration(labelText: 'Mudar Status', border: OutlineInputBorder()),
                items: ['pendente', 'aceito', 'em entrega', 'concluído', 'cancelado'].map((s) => DropdownMenuItem(
                  value: s,
                  child: Text(s.toUpperCase()),
                )).toList(),
                onChanged: (newStatus) async {
                  if (newStatus != null) {
                    final db = ref.read(databaseServiceProvider);
                    await db.updateOrderStatus(order.id!, newStatus);
                    ref.refresh(orderStatusHistoryProvider(order.id!));
                    ref.refresh(ordersProvider);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Status atualizado!')));
                    }
                  }
                },
              ),
              const SizedBox(height: 16),
              if (order.latitude != null && order.longitude != null) ...[
                ElevatedButton.icon(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => DeliveryMapScreen(order: order)),
                  ),
                  icon: const Icon(Icons.map),
                  label: const Text('VER MAPA E ROTA DE ENTREGA'),
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size(double.infinity, 50),
                    backgroundColor: Colors.blue,
                    foregroundColor: Colors.white,
                  ),
                ),
                const SizedBox(height: 16),
                ElevatedButton.icon(
                  onPressed: () async {
                    final url = 'google.navigation:q=${order.latitude},${order.longitude}';
                    final uri = Uri.parse(url);
                    if (await canLaunchUrl(uri)) {
                      await launchUrl(uri);
                    } else {
                      final webUrl = 'https://www.google.com/maps/dir/?api=1&destination=${order.latitude},${order.longitude}';
                      await launchUrl(Uri.parse(webUrl), mode: LaunchMode.externalApplication);
                    }
                  },
                  icon: const Icon(Icons.navigation),
                  label: const Text('ABRIR NO GOOGLE MAPS'),
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size(double.infinity, 50),
                    backgroundColor: Colors.green,
                    foregroundColor: Colors.white,
                  ),
                ),
              ],
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: () => _confirmDelete(context, ref),
                icon: const Icon(Icons.delete_forever),
                label: const Text('EXCLUIR PEDIDO'),
                style: ElevatedButton.styleFrom(
                  minimumSize: const Size(double.infinity, 50),
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                ),
              ),
            ],
            const Divider(height: 32),
            const Text('Histórico do Pedido', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            historyAsync.when(
              data: (history) => history.isEmpty
                  ? const Text('Nenhum histórico disponível.')
                  : ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: history.length,
                      itemBuilder: (context, index) {
                        final item = history[index];
                        final isLast = index == history.length - 1;
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Column(
                              children: [
                                Container(
                                  width: 12,
                                  height: 12,
                                  decoration: BoxDecoration(
                                    color: isLast ? AppTheme.primaryColor : Colors.grey,
                                    shape: BoxShape.circle,
                                  ),
                                ),
                                if (!isLast)
                                  Container(
                                    width: 2,
                                    height: 40,
                                    color: Colors.grey[300],
                                  ),
                              ],
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    item.status.toUpperCase(),
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      color: isLast ? AppTheme.primaryColor : Colors.black87,
                                    ),
                                  ),
                                  Text(
                                    dateFormat.format(item.createdAt),
                                    style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                                  ),
                                  if (item.notes != null && item.notes!.isNotEmpty)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 4),
                                      child: Text(item.notes!, style: const TextStyle(fontSize: 14)),
                                    ),
                                  const SizedBox(height: 16),
                                ],
                              ),
                            ),
                          ],
                        );
                      },
                    ),
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, s) => Text('Erro ao carregar histórico: $e'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoCard(BuildContext context, NumberFormat currencyFormat, DateFormat dateFormat) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10)],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildInfoRow('Status Atual', order.status.toUpperCase(), isStatus: true),
          const Divider(height: 24),
          _buildInfoRow('Cliente', order.customerName ?? 'N/A'),
          _buildInfoRow('WhatsApp', order.whatsapp),
          const SizedBox(height: 8),
          Center(
            child: ElevatedButton.icon(
              onPressed: () async {
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
              icon: const Icon(Icons.message, size: 18),
              label: const Text('ABRIR WHATSAPP'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
                minimumSize: const Size(double.infinity, 40),
              ),
            ),
          ),
          const Divider(height: 24),
          _buildInfoRow('Data', order.createdAt != null ? dateFormat.format(order.createdAt!) : '-'),
          _buildInfoRow('Total', currencyFormat.format(order.totalPrice), isBold: true),
          _buildInfoRow('Pagamento', order.paymentMethod.toUpperCase()),
          _buildInfoRow('Endereço', order.location),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value, {bool isStatus = false, bool isBold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.grey)),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontWeight: isBold || isStatus ? FontWeight.bold : FontWeight.normal,
                color: isStatus ? _getStatusColor(value) : null,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _confirmDelete(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Excluir Pedido'),
        content: const Text('Tem certeza que deseja excluir este pedido permanentemente?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(
            onPressed: () async {
              final db = ref.read(databaseServiceProvider);
              await db.deleteOrder(order.id!);
              ref.refresh(ordersProvider);
              if (context.mounted) {
                Navigator.pop(context); // Close dialog
                Navigator.pop(context); // Go back to orders list
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Pedido excluído.')));
              }
            },
            child: const Text('Excluir', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  Color _getStatusColor(String status) {
    switch (status.toLowerCase()) {
      case 'pendente': return Colors.orange;
      case 'aceito': return Colors.blue;
      case 'em entrega': return Colors.purple;
      case 'concluído': return Colors.green;
      case 'cancelado': return Colors.red;
      default: return Colors.grey;
    }
  }
}
