import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import '../../models/user_models.dart';
import '../../providers/address_provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/profile_provider.dart';
import '../../theme/app_theme.dart';

class AddEditAddressScreen extends ConsumerStatefulWidget {
  final Address? address;

  const AddEditAddressScreen({super.key, this.address});

  @override
  ConsumerState<AddEditAddressScreen> createState() => _AddEditAddressScreenState();
}

class _AddEditAddressScreenState extends ConsumerState<AddEditAddressScreen> {
  final _nameController = TextEditingController();
  final _receiverController = TextEditingController();
  final _phoneController = TextEditingController();
  final _addressLineController = TextEditingController();
  Position? _currentPosition;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    if (widget.address != null) {
      _nameController.text = widget.address!.name;
      _receiverController.text = widget.address!.receiverName ?? '';
      _phoneController.text = widget.address!.phone;
      _addressLineController.text = widget.address!.addressLine;
    } else {
      // Auto-fill WhatsApp from profile if available
      WidgetsBinding.instance.addPostFrameCallback((_) {
        final profile = ref.read(profileProvider).value;
        if (profile?.whatsapp != null) {
          _phoneController.text = profile!.whatsapp!;
        }
      });
    }
  }

  Future<void> _getCurrentLocation() async {
    bool serviceEnabled;
    LocationPermission permission;

    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Serviços de localização estão desativados.')),
        );
      }
      return;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Permissão de localização negada.')),
          );
        }
        return;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Permissões de localização estão permanentemente negadas. Ative nas configurações.')),
        );
      }
      return;
    }

    try {
      final position = await Geolocator.getCurrentPosition();
      setState(() => _currentPosition = position);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro ao obter localização: $e')),
        );
      }
    }
  }

  Future<void> _save() async {
    final user = ref.read(authProvider).value;
    if (user == null) return;

    if (_nameController.text.isEmpty || _addressLineController.text.isEmpty || _phoneController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Por favor, preencha os campos obrigatórios.')),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      final newAddress = Address(
        id: widget.address?.id,
        userId: user.id,
        name: _nameController.text,
        receiverName: _receiverController.text,
        phone: _phoneController.text,
        addressLine: _addressLineController.text,
        latitude: _currentPosition?.latitude ?? widget.address?.latitude,
        longitude: _currentPosition?.longitude ?? widget.address?.longitude,
      );

      await ref.read(addressesProvider.notifier).saveAddress(newAddress);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro ao salvar endereço: $e')),
        );
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.address == null ? 'Novo Endereço' : 'Editar Endereço'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Informações do Endereço', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            const Text('Atendemos apenas o Sítio Riacho dos Barreiros e locais próximos', style: TextStyle(color: Colors.red, fontSize: 12)),
            const SizedBox(height: 16),
            TextField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Apelido (ex: Casa, Trabalho)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _receiverController,
              decoration: const InputDecoration(
                labelText: 'Nome do Destinatário',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _phoneController,
              decoration: const InputDecoration(
                labelText: 'WhatsApp',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.phone,
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _addressLineController,
              decoration: const InputDecoration(
                labelText: 'Endereço Completo',
                border: OutlineInputBorder(),
                hintText: 'Rua, número, bairro, cidade',
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: _getCurrentLocation,
              icon: Icon(Icons.location_on, color: _currentPosition != null ? Colors.green : AppTheme.primaryColor),
              label: Text(_currentPosition != null ? 'Localização Capturada' : 'Capturar Localização Exata'),
              style: OutlinedButton.styleFrom(minimumSize: const Size(double.infinity, 50)),
            ),
            const SizedBox(height: 40),
            if (_isLoading)
              const Center(child: CircularProgressIndicator())
            else
              ElevatedButton(
                onPressed: _save,
                child: const Text('SALVAR ENDEREÇO'),
              ),
          ],
        ),
      ),
    );
  }
}
