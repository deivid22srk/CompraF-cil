import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../models/user_models.dart';
import '../../providers/order_provider.dart';
import '../../theme/app_theme.dart';

class OrderDetailsScreen extends ConsumerWidget {
  final Order order;

  const OrderDetailsScreen({super.key, required this.order});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
