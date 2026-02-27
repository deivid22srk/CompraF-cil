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

  // Addresses
  Future<List<Address>> getAddresses(String userId) async {
    final response = await _client
        .from('addresses')
        .select()
        .eq('user_id', userId)
        .order('created_at', ascending: false);

    return (response as List).map((json) => Address.fromJson(json)).toList();
  }

  Future<void> saveAddress(Address address) async {
    final data = address.toJson();
    if (address.id != null) {
      await _client.from('addresses').update(data).eq('id', address.id!);
    } else {
      await _client.from('addresses').insert(data);
    }
  }

  Future<void> deleteAddress(String addressId) async {
    await _client.from('addresses').delete().eq('id', addressId);
  }

  // Order History
  Future<List<OrderStatusHistory>> getOrderStatusHistory(String orderId) async {
    final response = await _client
        .from('order_status_history')
        .select()
        .eq('order_id', orderId)
        .order('created_at', ascending: true);

    return (response as List).map((json) => OrderStatusHistory.fromJson(json)).toList();
  }

  Future<List<Map<String, dynamic>>> getOrderItemsWithProducts(String orderId) async {
    final response = await _client
        .from('order_items')
        .select('*, products(*)')
        .eq('order_id', orderId);

    return List<Map<String, dynamic>>.from(response);
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

  Future<List<Profile>> getAdmins() async {
    final response = await _client
        .from('profiles')
        .select()
        .inFilter('role', ['admin', 'main_admin'])
        .order('full_name');

    return (response as List).map((json) => Profile.fromJson(json)).toList();
  }

  Future<Profile?> getProfileByEmail(String email) async {
    final response = await _client
        .from('profiles')
        .select()
        .eq('email', email)
        .maybeSingle();

    if (response == null) return null;
    return Profile.fromJson(response);
  }

  Future<void> updateAdminPermissions(String userId, String role, Map<String, bool> permissions) async {
    await _client.from('profiles').update({
      'role': role,
      'permissions': permissions,
    }).eq('id', userId);
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
      'email': profile.email,
    });
  }

  // Upload Avatar
  Future<String> uploadAvatar(String userId, List<int> bytes, String fileName) async {
    final path = 'avatars/$userId-$fileName';
    await _client.storage.from('product-images').uploadBinary(path, Uint8List.fromList(bytes));
    return _client.storage.from('product-images').getPublicUrl(path);
  }

  // Admin Methods
  Future<void> saveProduct(Map<String, dynamic> productData, {List<String>? additionalImages}) async {
    final response = await _client.from('products').upsert(productData).select().single();
    final productId = response['id'];

    if (additionalImages != null) {
      // Clear old images and insert new ones (simple sync approach)
      await _client.from('product_images').delete().eq('product_id', productId);

      final imagesToInsert = additionalImages.map((url) => {
        'product_id': productId,
        'image_url': url,
      }).toList();

      if (imagesToInsert.isNotEmpty) {
        await _client.from('product_images').insert(imagesToInsert);
      }
    }
  }

  Future<void> deleteProduct(String productId) async {
    await _client.from('products').delete().eq('id', productId);
  }

  Future<void> saveCategory(Map<String, dynamic> categoryData) async {
    if (categoryData['id'] != null) {
      await _client.from('categories').update(categoryData).eq('id', categoryData['id']);
    } else {
      await _client.from('categories').insert(categoryData);
    }
  }

  Future<void> deleteCategory(String categoryId) async {
    await _client.from('categories').delete().eq('id', categoryId);
  }

  Future<void> deleteOrder(String orderId) async {
    await _client.from('orders').delete().eq('id', orderId);
  }

  Future<void> updateAppConfig(String key, dynamic value) async {
    await _client.from('app_config').update({'value': value}).eq('key', key);
  }

  Future<String> uploadProductImage(List<int> bytes, String fileName) async {
    final path = 'products/$fileName';
    await _client.storage.from('product-images').uploadBinary(path, Uint8List.fromList(bytes));
    return _client.storage.from('product-images').getPublicUrl(path);
  }

  Future<void> updateOrderStatus(String orderId, String status, {String? notes}) async {
    await _client.from('orders').update({'status': status}).eq('id', orderId);
    await _client.from('order_status_history').insert({
      'order_id': orderId,
      'status': status,
      'notes': notes,
    });
  }
}
