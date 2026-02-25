class Profile {
  final String id;
  final String? fullName;
  final String? avatarUrl;
  final String? whatsapp;
  final String role;

  Profile({
    required this.id,
    this.fullName,
    this.avatarUrl,
    this.whatsapp,
    this.role = 'user',
  });

  factory Profile.fromJson(Map<String, dynamic> json) {
    return Profile(
      id: json['id'],
      fullName: json['full_name'],
      avatarUrl: json['avatar_url'],
      whatsapp: json['whatsapp'],
      role: json['role'] ?? 'user',
    );
  }
}

class CartItem {
  final String? id;
  final String userId;
  final String productId;
  final int quantity;
  final Map<String, String>? selectedVariations;
  final dynamic product; // Can be Product model

  CartItem({
    this.id,
    required this.userId,
    required this.productId,
    required this.quantity,
    this.selectedVariations,
    this.product,
  });

  factory CartItem.fromJson(Map<String, dynamic> json) {
    return CartItem(
      id: json['id'],
      userId: json['user_id'],
      productId: json['product_id'],
      quantity: json['quantity'],
      selectedVariations: json['selected_variations'] != null
          ? Map<String, String>.from(json['selected_variations'])
          : null,
      product: json['products'], // Supabase join usually returns 'products'
    );
  }
}

class Address {
  final String? id;
  final String userId;
  final String name;
  final String? receiverName;
  final String phone;
  final String addressLine;
  final double? latitude;
  final double? longitude;

  Address({
    this.id,
    required this.userId,
    required this.name,
    this.receiverName,
    required this.phone,
    required this.addressLine,
    this.latitude,
    this.longitude,
  });

  factory Address.fromJson(Map<String, dynamic> json) {
    return Address(
      id: json['id'],
      userId: json['user_id'],
      name: json['name'],
      receiverName: json['receiver_name'],
      phone: json['phone'],
      addressLine: json['address_line'],
      latitude: json['latitude'] != null ? (json['latitude'] as num).toDouble() : null,
      longitude: json['longitude'] != null ? (json['longitude'] as num).toDouble() : null,
    );
  }

  Map<String, dynamic> toJson() => {
    'user_id': userId,
    'name': name,
    'receiver_name': receiverName,
    'phone': phone,
    'address_line': addressLine,
    'latitude': latitude,
    'longitude': longitude,
  };
}

class Order {
  final String? id;
  final String? userId;
  final String? customerName;
  final String whatsapp;
  final String location;
  final double totalPrice;
  final double? latitude;
  final double? longitude;
  final String paymentMethod;
  final String status;
  final DateTime? createdAt;

  Order({
    this.id,
    this.userId,
    this.customerName,
    required this.whatsapp,
    required this.location,
    required this.totalPrice,
    this.latitude,
    this.longitude,
    this.paymentMethod = 'dinheiro',
    this.status = 'pendente',
    this.createdAt,
  });

  factory Order.fromJson(Map<String, dynamic> json) {
    return Order(
      id: json['id'],
      userId: json['user_id'],
      customerName: json['customer_name'],
      whatsapp: json['whatsapp'],
      location: json['location'],
      totalPrice: (json['total_price'] as num).toDouble(),
      latitude: json['latitude'] != null ? (json['latitude'] as num).toDouble() : null,
      longitude: json['longitude'] != null ? (json['longitude'] as num).toDouble() : null,
      paymentMethod: json['payment_method'] ?? 'dinheiro',
      status: json['status'] ?? 'pendente',
      createdAt: json['created_at'] != null ? DateTime.parse(json['created_at']) : null,
    );
  }
}

class OrderItem {
  final String? id;
  final String orderId;
  final String? productId;
  final int quantity;
  final double priceAtTime;
  final Map<String, String>? selectedVariations;

  OrderItem({
    this.id,
    required this.orderId,
    this.productId,
    required this.quantity,
    required this.priceAtTime,
    this.selectedVariations,
  });
}
