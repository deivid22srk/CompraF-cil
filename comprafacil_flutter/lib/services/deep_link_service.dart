import 'dart:async';
import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../models/product_models.dart';
import '../screens/product/product_details_screen.dart';

class DeepLinkService {
  static final DeepLinkService _instance = DeepLinkService._internal();
  factory DeepLinkService() => _instance;
  DeepLinkService._internal();

  final _appLinks = AppLinks();
  StreamSubscription<Uri>? _linkSubscription;

  void init(BuildContext context) {
    // Check initial link if app was closed
    _appLinks.getInitialLink().then((uri) {
      if (uri != null && context.mounted) {
        _handleDeepLink(context, uri);
      }
    });

    // Listen for links while app is in background/foreground
    _linkSubscription = _appLinks.uriLinkStream.listen((uri) {
      if (context.mounted) {
        _handleDeepLink(context, uri);
      }
    }, onError: (err) {
      debugPrint('Deep link error: $err');
    });
  }

  void dispose() {
    _linkSubscription?.cancel();
  }

  Future<void> _handleDeepLink(BuildContext context, Uri uri) async {
    debugPrint('Received deep link: $uri');

    String? productId;

    // Handle standard path: /product/{id}
    if (uri.pathSegments.length >= 2 && uri.pathSegments[0] == 'product') {
      productId = uri.pathSegments[1];
    }
    // Handle hash path: /#/product/{id}
    else if (uri.fragment.contains('/product/')) {
      final parts = uri.fragment.split('/');
      final index = parts.indexOf('product');
      if (index != -1 && index + 1 < parts.length) {
        productId = parts[index + 1];
      }
    }

    if (productId != null) {
      try {
        final client = Supabase.instance.client;
        final response = await client
            .from('products')
            .select('*, product_images(*)')
            .eq('id', productId)
            .single();

        if (context.mounted) {
          final product = Product.fromJson(response);
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => ProductDetailsScreen(product: product),
            ),
          );
        }
      } catch (e) {
        debugPrint('Error fetching product from deep link: $e');
      }
    }
  }
}
