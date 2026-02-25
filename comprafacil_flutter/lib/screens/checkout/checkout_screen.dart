import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../../providers/cart_provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/address_provider.dart';
import '../../theme/app_theme.dart';
import '../../models/product_models.dart';

class CheckoutScreen extends ConsumerStatefulWidget {
  const CheckoutScreen({super.key});

  @override
  ConsumerState<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends ConsumerState<CheckoutScreen> {
  final _whatsappController = TextEditingController();
  final _addressController = TextEditingController();
  final _customerNameController = TextEditingController();
  String _paymentMethod = 'dinheiro';
  Position? _currentPosition;
  bool _isLoading = false;

  Future<void> _getCurrentLocation() async {
    bool serviceEnabled;
    LocationPermission permission;

    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Serviços de localização estão desativados.')),
        );
      }
      return;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Permissão de localização negada.')),
          );
        }
        return;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Permissões de localização estão permanentemente negadas. Ative nas configurações.')),
        );
      }
      return;
    }

    try {
      final position = await Geolocator.getCurrentPosition();
      setState(() => _currentPosition = position);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro ao obter localização: $e')),
        );
      }
    }
  }

  Future<void> _submitOrder() async {
    final user = ref.read(authProvider).value;
    final cartItems = ref.read(cartProvider).value;
    final total = ref.read(cartTotalProvider);

    if (user == null || cartItems == null || cartItems.isEmpty) return;

    setState(() => _isLoading = true);

    try {
      final orderResponse = await Supabase.instance.client.from('orders').insert({
        'user_id': user.id,
        'customer_name': _customerNameController.text,
        'whatsapp': _whatsappController.text,
        'location': _addressController.text,
        'total_price': total,
        'latitude': _currentPosition?.latitude,
        'longitude': _currentPosition?.longitude,
        'payment_method': _paymentMethod,
        'status': 'pendente',
      }).select().single();

      final orderId = orderResponse['id'];

      final orderItems = cartItems.map((item) {
        final product = Product.fromJson(item.product);
        return {
          'order_id': orderId,
          'product_id': item.productId,
          'quantity': item.quantity,
          'price_at_time': product.price,
          'selected_variations': item.selectedVariations,
        };
      }).toList();

      await Supabase.instance.client.from('order_items').insert(orderItems);

      await ref.read(cartProvider.notifier).clearCart();

      if (mounted) {
        Navigator.popUntil(context, (route) => route.isFirst);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Pedido realizado com sucesso!')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro ao realizar pedido: $e')),
        );
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final addressesAsync = ref.watch(addressesProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Checkout')),
      body: SingleChildScrollView(
        padding: EdgeInsets.only(
          left: 24,
          right: 24,
          top: 24,
          bottom: MediaQuery.of(context).padding.bottom + 24,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Informações de Entrega', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            const SizedBox(height: 16),
            addressesAsync.when(
              data: (addresses) => addresses.isEmpty
                  ? const SizedBox.shrink()
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Selecionar Endereço Salvo', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
                        const SizedBox(height: 8),
                        DropdownButtonFormField<Address>(
                          decoration: const InputDecoration(
                            border: OutlineInputBorder(),
                            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          ),
                          hint: const Text('Escolha um endereço'),
                          items: addresses.map((addr) => DropdownMenuItem(
                            value: addr,
                            child: Text(addr.name),
                          )).toList(),
                          onChanged: (addr) {
                            if (addr != null) {
                              setState(() {
                                _addressController.text = addr.addressLine;
                                if (addr.receiverName != null && addr.receiverName!.isNotEmpty) {
                                  _customerNameController.text = addr.receiverName!;
                                }
                                _whatsappController.text = addr.phone;
                              });
                            }
                          },
                        ),
                        const SizedBox(height: 16),
                        const Center(child: Text('OU PREENCHA ABAIXO', style: TextStyle(fontSize: 10, color: Colors.grey))),
                        const SizedBox(height: 16),
                      ],
                    ),
              loading: () => const LinearProgressIndicator(),
              error: (e, s) => Text('Erro ao carregar endereços: $e', style: const TextStyle(color: Colors.red, fontSize: 12)),
            ),
            TextField(
              controller: _customerNameController,
              decoration: const InputDecoration(labelText: 'Seu Nome', border: OutlineInputBorder()),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _whatsappController,
              decoration: const InputDecoration(labelText: 'WhatsApp', border: OutlineInputBorder()),
              keyboardType: TextInputType.phone,
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _addressController,
              decoration: const InputDecoration(
                labelText: 'Endereço Completo',
                border: OutlineInputBorder(),
                hintText: 'Rua, número, bairro, cidade',
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: _getCurrentLocation,
              icon: Icon(Icons.location_on, color: _currentPosition != null ? Colors.green : AppTheme.primaryColor),
              label: Text(_currentPosition != null ? 'Localização Capturada' : 'Capturar Localização Exata'),
              style: OutlinedButton.styleFrom(minimumSize: const Size(double.infinity, 50)),
            ),
            const SizedBox(height: 32),
            const Text('Forma de Pagamento', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            const SizedBox(height: 8),
            ListTile(
              title: const Text('Dinheiro'),
              leading: Radio<String>(
                value: 'dinheiro',
                groupValue: _paymentMethod,
                onChanged: (v) => setState(() => _paymentMethod = v!),
              ),
            ),
            ListTile(
              title: const Text('Pix'),
              leading: Radio<String>(
                value: 'pix',
                groupValue: _paymentMethod,
                onChanged: (v) => setState(() => _paymentMethod = v!),
              ),
            ),
            const SizedBox(height: 40),
            if (_isLoading)
              const Center(child: CircularProgressIndicator())
            else
              ElevatedButton(
                onPressed: _submitOrder,
                child: const Text('CONFIRMAR PEDIDO'),
              ),
          ],
        ),
      ),
    );
  }
}
