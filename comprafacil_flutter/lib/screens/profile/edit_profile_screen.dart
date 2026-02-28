import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../../providers/profile_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/cached_avatar.dart';

class EditProfileScreen extends ConsumerStatefulWidget {
  const EditProfileScreen({super.key});

  @override
  ConsumerState<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends ConsumerState<EditProfileScreen> {
  final _nameController = TextEditingController();
  final _whatsappController = TextEditingController();
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    final profile = ref.read(profileProvider).value;
    if (profile != null) {
      _nameController.text = profile.fullName ?? '';
      String whatsapp = profile.whatsapp ?? '';
      if (whatsapp.startsWith('+55')) {
        whatsapp = whatsapp.replaceFirst('+55', '').trim();
      }
      _whatsappController.text = whatsapp;
    }
  }

  Future<void> _pickImage() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.gallery, imageQuality: 70);
    if (image != null) {
      setState(() => _isLoading = true);
      try {
        final bytes = await image.readAsBytes();
        await ref.read(profileProvider.notifier).uploadAvatar(bytes, image.name);
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erro ao carregar imagem: $e')));
        }
      } finally {
        if (mounted) setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _saveProfile() async {
    setState(() => _isLoading = true);
    try {
      String whatsapp = _whatsappController.text.trim();
      if (whatsapp.isNotEmpty) {
        whatsapp = '+55 $whatsapp';
      }
      await ref.read(profileProvider.notifier).updateProfile(
        fullName: _nameController.text,
        whatsapp: whatsapp,
      );
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Perfil atualizado!')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erro ao salvar: $e')));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profileAsync = ref.watch(profileProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Editar Perfil')),
      body: profileAsync.when(
        data: (profile) => SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Stack(
                children: [
                  CachedAvatar(
                    url: profile?.avatarUrl,
                    radius: 60,
                    placeholderIcon: Icons.person,
                  ),
                  Positioned(
                    bottom: 0,
                    right: 0,
                    child: CircleAvatar(
                      backgroundColor: AppTheme.primaryColor,
                      child: IconButton(
                        icon: const Icon(Icons.camera_alt, color: Colors.white),
                        onPressed: _pickImage,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 32),
              TextField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Nome Completo', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _whatsappController,
                decoration: const InputDecoration(
                  labelText: 'WhatsApp',
                  border: OutlineInputBorder(),
                  prefixText: '+55 ',
                ),
                keyboardType: TextInputType.phone,
              ),
              const SizedBox(height: 40),
              if (_isLoading)
                const CircularProgressIndicator()
              else
                ElevatedButton(
                  onPressed: _saveProfile,
                  child: const Text('SALVAR ALTERAÇÕES'),
                ),
            ],
          ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, s) => Center(child: Text('Erro: $e')),
      ),
    );
  }
}
