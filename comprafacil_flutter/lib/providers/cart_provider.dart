import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../models/product_models.dart';
import '../models/user_models.dart';
import 'auth_provider.dart';

final cartProvider = StateNotifierProvider<CartNotifier, AsyncValue<List<CartItem>>>((ref) {
  final user = ref.watch(authProvider).value;
  return CartNotifier(user?.id);
});

class CartNotifier extends StateNotifier<AsyncValue<List<CartItem>>> {
  final String? userId;
  final _client = Supabase.instance.client;

  CartNotifier(this.userId) : super(const AsyncValue.loading()) {
    if (userId != null) {
      fetchCart();
    } else {
      state = const AsyncValue.data([]);
    }
  }

  Future<void> fetchCart() async {
    if (userId == null) return;
    try {
      final response = await _client
          .from('cart_items')
          .select('*, products(*, product_images(*))')
          .eq('user_id', userId!);

      final items = (response as List).map((json) => CartItem.fromJson(json)).toList();
      state = AsyncValue.data(items);
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  Future<void> addToCart(Product product, {int quantity = 1, Map<String, String>? variations}) async {
    if (userId == null) return;

    try {
      // Check if already in cart
      final existing = state.value?.where(
        (item) => item.productId == product.id && _mapEquals(item.selectedVariations, variations)
      ).firstOrNull;

      int currentInCart = existing?.quantity ?? 0;
      if (currentInCart + quantity > product.stockQuantity) {
        throw Exception('Limite de estoque atingido');
      }

      if (existing != null) {
        await _client
            .from('cart_items')
            .update({'quantity': currentInCart + quantity})
            .eq('id', existing.id!);
      } else {
        await _client.from('cart_items').insert({
          'user_id': userId,
          'product_id': product.id,
          'quantity': quantity,
          'selected_variations': variations,
        });
      }
      fetchCart();
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateQuantity(String itemId, int newQuantity, int stockQuantity) async {
    try {
      if (newQuantity <= 0) {
        await _client.from('cart_items').delete().eq('id', itemId);
      } else {
        if (newQuantity > stockQuantity) {
           throw Exception('Limite de estoque atingido');
        }
        await _client.from('cart_items').update({'quantity': newQuantity}).eq('id', itemId);
      }
      fetchCart();
    } catch (e) {
      rethrow;
    }
  }

  Future<void> clearCart() async {
    if (userId == null) return;
    try {
      await _client.from('cart_items').delete().eq('user_id', userId!);
      state = const AsyncValue.data([]);
    } catch (e) {}
  }

  bool _mapEquals(Map? a, Map? b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    if (a.length != b.length) return false;
    for (final key in a.keys) {
      if (b[key] != a[key]) return false;
    }
    return true;
  }
}

final cartTotalProvider = Provider<double>((ref) {
  final cartAsync = ref.watch(cartProvider);
  return cartAsync.maybeWhen(
    data: (items) => items.fold(0.0, (sum, item) {
      final product = Product.fromJson(item.product);
      return sum + (product.price * item.quantity);
    }),
    orElse: () => 0.0,
  );
});
