class Category {
  final String id;
  final String name;
  final String? iconUrl;
  final DateTime? createdAt;

  Category({
    required this.id,
    required this.name,
    this.iconUrl,
    this.createdAt,
  });

  factory Category.fromJson(Map<String, dynamic> json) {
    return Category(
      id: json['id'],
      name: json['name'],
      iconUrl: json['icon_url'],
      createdAt: json['created_at'] != null ? DateTime.parse(json['created_at']) : null,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'icon_url': iconUrl,
  };
}

class ProductVariation {
  final String name;
  final List<String> values;

  ProductVariation({required this.name, required this.values});

  factory ProductVariation.fromJson(Map<String, dynamic> json) {
    return ProductVariation(
      name: json['name'],
      values: List<String>.from(json['values']),
    );
  }

  Map<String, dynamic> toJson() => {
    'name': name,
    'values': values,
  };
}

class ProductImage {
  final String id;
  final String productId;
  final String imageUrl;

  ProductImage({required this.id, required this.productId, required this.imageUrl});

  factory ProductImage.fromJson(Map<String, dynamic> json) {
    return ProductImage(
      id: json['id'],
      productId: json['product_id'],
      imageUrl: json['image_url'],
    );
  }
}

class Product {
  final String id;
  final String name;
  final String? description;
  final double price;
  final String? imageUrl;
  final int stockQuantity;
  final String? soldBy;
  final String? categoryId;
  final List<ProductImage>? images;
  final List<ProductVariation>? variations;

  Product({
    required this.id,
    required this.name,
    this.description,
    required this.price,
    this.imageUrl,
    this.stockQuantity = 0,
    this.soldBy,
    this.categoryId,
    this.images,
    this.variations,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: json['id'],
      name: json['name'],
      description: json['description'],
      price: (json['price'] as num).toDouble(),
      imageUrl: json['image_url'],
      stockQuantity: json['stock_quantity'] ?? 0,
      soldBy: json['sold_by'],
      categoryId: json['category_id'],
      images: json['product_images'] != null
          ? (json['product_images'] as List).map((i) => ProductImage.fromJson(i)).toList()
          : (json['images'] != null
              ? (json['images'] as List).map((i) => ProductImage.fromJson(i)).toList()
              : null),
      variations: json['variations'] != null
          ? (json['variations'] as List).map((i) => ProductVariation.fromJson(i)).toList()
          : null,
    );
  }
}
