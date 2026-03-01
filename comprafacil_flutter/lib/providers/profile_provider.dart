import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/user_models.dart';
import 'auth_provider.dart';
import 'product_provider.dart';

final profileProvider = StateNotifierProvider<ProfileNotifier, AsyncValue<Profile?>>((ref) {
  final user = ref.watch(authProvider).value;
  return ProfileNotifier(ref, user?.id);
});

class ProfileNotifier extends StateNotifier<AsyncValue<Profile?>> {
  final Ref _ref;
  final String? _userId;

  ProfileNotifier(this._ref, this._userId) : super(const AsyncValue.loading()) {
    if (_userId != null) {
      fetchProfile();
    } else {
      state = const AsyncValue.data(null);
    }
  }

  Future<void> fetchProfile() async {
    if (_userId == null) return;
    state = const AsyncValue.loading();
    try {
      final db = _ref.read(databaseServiceProvider);
      var profile = await db.getProfile(_userId!);

      // Auto-update email from auth if missing in profile
      final user = _ref.read(authProvider).value;
      if (profile != null && profile.email == null && user?.email != null) {
        profile = Profile(
          id: profile.id,
          fullName: profile.fullName,
          avatarUrl: profile.avatarUrl,
          whatsapp: profile.whatsapp,
          email: user!.email,
          role: profile.role,
          permissions: profile.permissions,
        );
        await db.updateProfile(profile);
      }

      state = AsyncValue.data(profile);
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  Future<void> updateProfile({String? fullName, String? avatarUrl, String? whatsapp, String? email}) async {
    if (_userId == null || state.value == null) return;
    try {
      final db = _ref.read(databaseServiceProvider);
      final updatedProfile = Profile(
        id: _userId!,
        fullName: fullName ?? state.value!.fullName,
        avatarUrl: avatarUrl ?? state.value!.avatarUrl,
        whatsapp: whatsapp ?? state.value!.whatsapp,
        email: email ?? state.value!.email,
        role: state.value!.role,
        permissions: state.value!.permissions,
      );
      await db.updateProfile(updatedProfile);
      state = AsyncValue.data(updatedProfile);
    } catch (e) {
      rethrow;
    }
  }

  Future<void> uploadAvatar(List<int> bytes, String fileName) async {
    if (_userId == null) return;
    try {
      final db = _ref.read(databaseServiceProvider);
      final publicUrl = await db.uploadAvatar(_userId!, bytes, fileName);
      await updateProfile(avatarUrl: publicUrl);
    } catch (e) {
      rethrow;
    }
  }
}
