import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

final authProvider = StateNotifierProvider<AuthNotifier, AsyncValue<User?>>((ref) {
  return AuthNotifier();
});

class AuthNotifier extends StateNotifier<AsyncValue<User?>> {
  AuthNotifier() : super(const AsyncValue.loading()) {
    _init();
  }

  void _init() {
    final session = Supabase.instance.client.auth.currentSession;
    state = AsyncValue.data(session?.user);

    Supabase.instance.client.auth.onAuthStateChange.listen((data) {
      final Session? session = data.session;
      state = AsyncValue.data(session?.user);
    });
  }

  Future<void> signIn(String email, String password) async {
    state = const AsyncValue.loading();
    try {
      await Supabase.instance.client.auth.signInWithPassword(email: email, password: password);
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  Future<AuthResponse?> signUp(String email, String password) async {
    state = const AsyncValue.loading();
    try {
      final response = await Supabase.instance.client.auth.signUp(email: email, password: password);
      // After signup, if email confirmation is required, the user session might be null.
      // We set state back to data(null) so it's not in loading state anymore if it didn't auto-login.
      if (response.session == null) {
        state = const AsyncValue.data(null);
      }
      return response;
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
      rethrow;
    }
  }

  Future<void> signOut() async {
    await Supabase.instance.client.auth.signOut();
  }
}
