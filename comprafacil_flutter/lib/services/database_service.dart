import 'dart:typed_data';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../models/product_models.dart';
import '../models/user_models.dart';

class DatabaseService {
  final SupabaseClient _client = Supabase.instance.client;

  // Products
  Future<List<Product>> getProducts() async {
    final response = await _client
        .from('products')
        .select('*, product_images(*)')
        .order('created_at', ascending: false);

    return (response as List).map((json) => Product.fromJson(json)).toList();
  }

  Future<List<Category>> getCategories() async {
    final response = await _client.from('categories').select().order('name');
    return (response as List).map((json) => Category.fromJson(json)).toList();
  }

  // Cart
  Future<List<CartItem>> getCartItems(String userId) async {
    final response = await _client
        .from('cart_items')
        .select('*, products(*, product_images(*))')
        .eq('user_id', userId);

    return (response as List).map((json) => CartItem.fromJson(json)).toList();
  }

  // Orders
  Future<List<Order>> getOrders(String userId) async {
    final response = await _client
        .from('orders')
        .select()
        .eq('user_id', userId)
        .order('created_at', ascending: false);

    return (response as List).map((json) => Order.fromJson(json)).toList();
  }

  // Profile
  Future<Profile?> getProfile(String userId) async {
    final response = await _client
        .from('profiles')
        .select()
        .eq('id', userId)
        .maybeSingle();

    if (response == null) return null;
    return Profile.fromJson(response);
  }

  // App Config
  Future<Map<String, dynamic>> getAppConfig() async {
    final response = await _client.from('app_config').select();
    final Map<String, dynamic> config = {};
    for (var item in (response as List)) {
      config[item['key']] = item['value'];
    }
    return config;
  }

  // Update Profile
  Future<void> updateProfile(Profile profile) async {
    await _client.from('profiles').upsert({
      'id': profile.id,
      'full_name': profile.fullName,
      'avatar_url': profile.avatarUrl,
      'whatsapp': profile.whatsapp,
    });
  }

  // Upload Avatar
  Future<String> uploadAvatar(String userId, List<int> bytes, String fileName) async {
    final path = 'avatars/$userId-$fileName';
    await _client.storage.from('product-images').uploadBinary(path, Uint8List.fromList(bytes));
    return _client.storage.from('product-images').getPublicUrl(path);
  }
}
