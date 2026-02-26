import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'services/supabase_service.dart';
// import 'services/notification_service.dart';
// import 'services/background_service.dart';
import 'theme/app_theme.dart';
import 'providers/theme_provider.dart';
import 'screens/splash_screen.dart';
import 'screens/auth/login_screen.dart';
import 'screens/home/home_screen.dart';
import 'screens/admin/admin_home_screen.dart';
import 'providers/auth_provider.dart';
import 'providers/admin_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SupabaseService.initialize();
  runApp(const ProviderScope(child: CompraFacilApp()));
}

class CompraFacilApp extends ConsumerWidget {
  const CompraFacilApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    final themeMode = ref.watch(themeProvider);

    return MaterialApp(
      title: 'CompraFacil',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: themeMode,
      home: authState.when(
        data: (user) {
          if (user == null) return const LoginScreen();
          final isAdminMode = ref.watch(isAdminModeProvider);
          return isAdminMode ? const AdminHomeScreen() : const HomeScreen();
        },
        loading: () => const SplashScreen(),
        error: (e, s) => const LoginScreen(),
      ),
    );
  }
}
