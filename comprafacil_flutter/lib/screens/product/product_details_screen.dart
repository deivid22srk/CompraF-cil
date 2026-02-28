import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:intl/intl.dart';
import 'package:share_plus/share_plus.dart';
import '../cart/cart_screen.dart';
import 'zoom_image_screen.dart';
import '../../models/product_models.dart';
import '../../providers/cart_provider.dart';
import '../../theme/app_theme.dart';

class ProductDetailsScreen extends ConsumerStatefulWidget {
  final Product product;

  const ProductDetailsScreen({super.key, required this.product});

  @override
  ConsumerState<ProductDetailsScreen> createState() => _ProductDetailsScreenState();
}

class _ProductDetailsScreenState extends ConsumerState<ProductDetailsScreen> {
  int _quantity = 1;
  Map<String, String> _selectedVariations = {};
  int _currentImageIndex = 0;

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 400,
            pinned: true,
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                children: [
                  Builder(
                    builder: (context) {
                      final allImages = [
                        if (widget.product.imageUrl != null) widget.product.imageUrl!,
                        if (widget.product.images != null)
                          ...widget.product.images!.map((i) => i.imageUrl),
                      ].toSet().toList();

                      if (allImages.isEmpty) {
                        return Container(color: Colors.grey[200]);
                      }

                      return GestureDetector(
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => ZoomImageScreen(
                              images: allImages,
                              initialIndex: _currentImageIndex,
                            ),
                          ),
                        ),
                        child: PageView.builder(
                          itemCount: allImages.length,
                          onPageChanged: (index) => setState(() => _currentImageIndex = index),
                          itemBuilder: (context, index) {
                            return CachedNetworkImage(
                              imageUrl: allImages[index],
                              fit: BoxFit.cover,
                            );
                          },
                        ),
                      );
                    },
                  ),
                  Positioned(
                    bottom: 20,
                    right: 20,
                    child: Builder(
                      builder: (context) {
                        final allImagesCount = [
                          if (widget.product.imageUrl != null) widget.product.imageUrl!,
                          if (widget.product.images != null)
                            ...widget.product.images!.map((i) => i.imageUrl),
                        ].toSet().length;

                        if (allImagesCount <= 1) return const SizedBox.shrink();

                        return Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.black54,
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            '${_currentImageIndex + 1}/$allImagesCount',
                            style: const TextStyle(color: Colors.white, fontSize: 12),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
            backgroundColor: Colors.white,
            leading: Padding(
              padding: const EdgeInsets.all(8.0),
              child: CircleAvatar(
                backgroundColor: Colors.white,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back, color: Colors.black),
                  onPressed: () => Navigator.pop(context),
                ),
              ),
            ),
            actions: [
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: CircleAvatar(
                  backgroundColor: Colors.white,
                  child: IconButton(
                    icon: const Icon(Icons.share, color: Colors.black),
                    onPressed: () {
                      final productUrl = 'https://comprafacil.ct.ws/#/product/${widget.product.id}';
                      Share.share(
                        'Confira este produto no CompraFácil: ${widget.product.name}\n\n$productUrl',
                        subject: widget.product.name,
                      );
                    },
                  ),
                ),
              ),
            ],
          ),
          SliverToBoxAdapter(
            child: Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Theme.of(context).scaffoldBackgroundColor,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(40)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              widget.product.name,
                              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            Text(
                              'Vendido por ${widget.product.soldBy ?? 'CompraFácil'}',
                              style: TextStyle(color: Colors.grey[600]),
                            ),
                          ],
                        ),
                      ),
                      Text(
                        currencyFormat.format(widget.product.price),
                        style: const TextStyle(
                          color: AppTheme.primaryColor,
                          fontSize: 24,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  const Text(
                    'Descrição',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    widget.product.description ?? 'Sem descrição disponível.',
                    style: TextStyle(color: Colors.grey[700], height: 1.5),
                  ),
                  const SizedBox(height: 32),
                  if (widget.product.variations != null)
                    ...widget.product.variations!.map((v) => Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(v.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 12,
                          children: v.values.map((val) {
                            final isSelected = _selectedVariations[v.name] == val;
                            return GestureDetector(
                              onTap: () => setState(() => _selectedVariations[v.name] = val),
                              child: Chip(
                                label: Text(val),
                                backgroundColor: isSelected ? AppTheme.primaryColor : Theme.of(context).cardColor,
                                labelStyle: TextStyle(color: isSelected ? Colors.white : Theme.of(context).textTheme.bodyLarge?.color),
                              ),
                            );
                          }).toList(),
                        ),
                        const SizedBox(height: 16),
                      ],
                    )),
                  Row(
                    children: [
                      Container(
                        decoration: BoxDecoration(
                          color: Theme.of(context).cardColor,
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Row(
                          children: [
                            IconButton(
                              onPressed: () => setState(() => _quantity = _quantity > 1 ? _quantity - 1 : 1),
                              icon: const Icon(Icons.remove),
                            ),
                            Text(
                              '$_quantity',
                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                            ),
                            IconButton(
                              onPressed: () {
                                if (_quantity < widget.product.stockQuantity) {
                                  setState(() => _quantity++);
                                } else {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('Limite de estoque atingido')),
                                  );
                                }
                              },
                              icon: const Icon(Icons.add),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 100),
                ],
              ),
            ),
          ),
        ],
      ),
      bottomSheet: Container(
        padding: EdgeInsets.only(
          left: 24,
          right: 24,
          top: 24,
          bottom: MediaQuery.of(context).padding.bottom + 24,
        ),
        decoration: BoxDecoration(
          color: Theme.of(context).cardColor,
          boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 10)],
        ),
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () async {
                  try {
                    await ref.read(cartProvider.notifier).addToCart(
                      widget.product,
                      quantity: _quantity,
                      variations: _selectedVariations,
                    );
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Adicionado ao carrinho!')),
                      );
                    }
                  } catch (e) {
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
                      );
                    }
                  }
                },
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppTheme.primaryColor),
                  minimumSize: const Size(double.infinity, 56),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                ),
                child: const Text('CARRINHO'),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: ElevatedButton(
                onPressed: () async {
                  try {
                    await ref.read(cartProvider.notifier).addToCart(
                      widget.product,
                      quantity: _quantity,
                      variations: _selectedVariations,
                    );
                    if (mounted) {
                      Navigator.push(context, MaterialPageRoute(builder: (_) => const CartScreen()));
                    }
                  } catch (e) {
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
                      );
                    }
                  }
                },
                child: const Text('COMPRAR AGORA'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
