import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/product_models.dart';
import '../services/database_service.dart';

final databaseServiceProvider = Provider((ref) => DatabaseService());

final productsProvider = FutureProvider<List<Product>>((ref) async {
  final db = ref.watch(databaseServiceProvider);
  return await db.getProducts();
});

final categoriesProvider = FutureProvider<List<Category>>((ref) async {
  final db = ref.watch(databaseServiceProvider);
  return await db.getCategories();
});

final selectedCategoryProvider = StateProvider<String?>((ref) => null);

final filteredProductsProvider = Provider<AsyncValue<List<Product>>>((ref) {
  final productsAsync = ref.watch(productsProvider);
  final selectedCategory = ref.watch(selectedCategoryProvider);

  return productsAsync.whenData((products) {
    if (selectedCategory == null) return products;
    return products.where((p) => p.categoryId == selectedCategory).toList();
  });
});
