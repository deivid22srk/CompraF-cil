import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dart:convert';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user_models.dart';
import 'auth_provider.dart';
import 'product_provider.dart';
import 'admin_provider.dart';

final ordersProvider = FutureProvider<List<Order>>((ref) async {
  final user = ref.watch(authProvider).value;
  if (user == null) return [];

  final isAdminMode = ref.watch(isAdminModeProvider);

  var query = Supabase.instance.client.from('orders').select();

  if (!isAdminMode) {
    query = query.eq('user_id', user.id);
  }

  try {
    final response = await query.order('created_at', ascending: false);
    final orders = (response as List).map((json) => Order.fromJson(json)).toList();

    // Cache for offline use
    final prefs = await SharedPreferences.getInstance();
    final ordersJson = orders.map((o) => {
      'id': o.id,
      'user_id': o.userId,
      'customer_name': o.customerName,
      'whatsapp': o.whatsapp,
      'location': o.location,
      'total_price': o.totalPrice,
      'latitude': o.latitude,
      'longitude': o.longitude,
      'payment_method': o.paymentMethod,
      'status': o.status,
      'created_at': o.createdAt?.toIso8601String(),
    }).toList();
    await prefs.setString('cached_orders', jsonEncode(ordersJson));

    return orders;
  } catch (e) {
    // If offline, try to load from cache
    final prefs = await SharedPreferences.getInstance();
    final cached = prefs.getString('cached_orders');
    if (cached != null) {
      final List decoded = jsonDecode(cached);
      return decoded.map((json) => Order.fromJson(json)).toList();
    }
    rethrow;
  }
});

final orderStatusProvider = StreamProvider.family<String, String>((ref, orderId) {
  return Supabase.instance.client
      .from('orders')
      .stream(primaryKey: ['id'])
      .eq('id', orderId)
      .map((data) => data.first['status'] as String);
});

final orderStatusHistoryProvider = FutureProvider.family<List<OrderStatusHistory>, String>((ref, orderId) async {
  final db = ref.read(databaseServiceProvider);
  try {
    final history = await db.getOrderStatusHistory(orderId);
    final prefs = await SharedPreferences.getInstance();
    final historyJson = history.map((h) => {
      'id': h.id,
      'order_id': h.orderId,
      'status': h.status,
      'notes': h.notes,
      'created_at': h.createdAt.toIso8601String(),
    }).toList();
    await prefs.setString('cached_history_$orderId', jsonEncode(historyJson));
    return history;
  } catch (e) {
    final prefs = await SharedPreferences.getInstance();
    final cached = prefs.getString('cached_history_$orderId');
    if (cached != null) {
      final List decoded = jsonDecode(cached);
      return decoded.map((json) => OrderStatusHistory.fromJson(json)).toList();
    }
    rethrow;
  }
});

final orderItemsProvider = FutureProvider.family<List<Map<String, dynamic>>, String>((ref, orderId) async {
  final db = ref.read(databaseServiceProvider);
  try {
    final items = await db.getOrderItemsWithProducts(orderId);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('cached_items_$orderId', jsonEncode(items));
    return items;
  } catch (e) {
    final prefs = await SharedPreferences.getInstance();
    final cached = prefs.getString('cached_items_$orderId');
    if (cached != null) {
      return List<Map<String, dynamic>>.from(jsonDecode(cached));
    }
    rethrow;
  }
});
