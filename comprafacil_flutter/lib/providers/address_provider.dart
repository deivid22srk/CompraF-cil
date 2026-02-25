import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/user_models.dart';
import 'auth_provider.dart';
import 'product_provider.dart';

final addressesProvider = StateNotifierProvider<AddressNotifier, AsyncValue<List<Address>>>((ref) {
  final user = ref.watch(authProvider).value;
  return AddressNotifier(ref, user?.id);
});

class AddressNotifier extends StateNotifier<AsyncValue<List<Address>>> {
  final Ref _ref;
  final String? _userId;

  AddressNotifier(this._ref, this._userId) : super(const AsyncValue.loading()) {
    if (_userId != null) {
      fetchAddresses();
    } else {
      state = const AsyncValue.data([]);
    }
  }

  Future<void> fetchAddresses() async {
    if (_userId == null) return;
    state = const AsyncValue.loading();
    try {
      final db = _ref.read(databaseServiceProvider);
      final addresses = await db.getAddresses(_userId!);
      state = AsyncValue.data(addresses);
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  Future<void> saveAddress(Address address) async {
    if (_userId == null) return;
    try {
      final db = _ref.read(databaseServiceProvider);
      await db.saveAddress(address);
      await fetchAddresses();
    } catch (e) {
      rethrow;
    }
  }

  Future<void> deleteAddress(String addressId) async {
    if (_userId == null) return;
    try {
      final db = _ref.read(databaseServiceProvider);
      await db.deleteAddress(addressId);
      await fetchAddresses();
    } catch (e) {
      rethrow;
    }
  }
}
