import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../models/user_models.dart';
import 'auth_provider.dart';

final ordersProvider = FutureProvider<List<Order>>((ref) async {
  final user = ref.watch(authProvider).value;
  if (user == null) return [];

  final response = await Supabase.instance.client
      .from('orders')
      .select()
      .eq('user_id', user.id)
      .order('created_at', ascending: false);

  return (response as List).map((json) => Order.fromJson(json)).toList();
});

final orderStatusProvider = StreamProvider.family<String, String>((ref, orderId) {
  return Supabase.instance.client
      .from('orders')
      .stream(primaryKey: ['id'])
      .eq('id', orderId)
      .map((data) => data.first['status'] as String);
});
