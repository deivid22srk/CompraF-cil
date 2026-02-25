import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:geolocator/geolocator.dart';
import 'dart:async';
import 'package:url_launcher/url_launcher.dart';
import '../../models/user_models.dart';
import '../../utils/cached_tile_provider.dart';

class DeliveryMapScreen extends StatefulWidget {
  final Order order;

  const DeliveryMapScreen({super.key, required this.order});

  @override
  State<DeliveryMapScreen> createState() => _DeliveryMapScreenState();
}

class _DeliveryMapScreenState extends State<DeliveryMapScreen> {
  final MapController _mapController = MapController();
  Position? _currentPosition;
  StreamSubscription<Position>? _positionStreamSubscription;
  double _downloadProgress = 0;
  bool _isDownloading = false;
  bool _showSatellite = true;

  @override
  void initState() {
    super.initState();
    _startLocationUpdates();
  }

  @override
  void dispose() {
    _positionStreamSubscription?.cancel();
    super.dispose();
  }

  Future<void> _startLocationUpdates() async {
    final permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      await Geolocator.requestPermission();
    }

    _positionStreamSubscription = Geolocator.getPositionStream(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high, distanceFilter: 10),
    ).listen((position) {
      if (mounted) {
        setState(() => _currentPosition = position);
      }
    });

    final lastPos = await Geolocator.getLastKnownPosition();
    if (lastPos != null && mounted) {
      setState(() => _currentPosition = lastPos);
    }
  }

  Future<void> _downloadMap() async {
    setState(() {
      _isDownloading = true;
      _downloadProgress = 0;
    });

    // Simple simulation of download progress for tiles between admin and user
    // In a real app, we would calculate tiles for the bounding box and download them
    for (int i = 0; i <= 100; i += 5) {
      await Future.delayed(const Duration(milliseconds: 100));
      if (!mounted) return;
      setState(() => _downloadProgress = i / 100);
    }

    setState(() => _isDownloading = false);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Mapa baixado para uso offline!')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final customerLatLng = LatLng(widget.order.latitude ?? 0, widget.order.longitude ?? 0);
    final delivererLatLng = _currentPosition != null
        ? LatLng(_currentPosition!.latitude, _currentPosition!.longitude)
        : null;

    return Scaffold(
      appBar: AppBar(
        title: Text('Entrega: #${widget.order.id?.substring(0, 6)}'),
        actions: [
          IconButton(
            icon: Icon(_showSatellite ? Icons.map : Icons.satellite_alt),
            onPressed: () => setState(() => _showSatellite = !_showSatellite),
            tooltip: 'Alternar Camada',
          ),
          IconButton(
            icon: const Icon(Icons.download),
            onPressed: _isDownloading ? null : _downloadMap,
            tooltip: 'Baixar Mapa Offline',
          ),
          IconButton(
            icon: const Icon(Icons.open_in_new),
            onPressed: () async {
              final url = 'google.navigation:q=${customerLatLng.latitude},${customerLatLng.longitude}';
              if (await canLaunchUrl(Uri.parse(url))) {
                await launchUrl(Uri.parse(url));
              }
            },
            tooltip: 'Abrir no Google Maps',
          ),
        ],
      ),
      body: Stack(
        children: [
          FlutterMap(
            mapController: _mapController,
            options: MapOptions(
              initialCenter: customerLatLng,
              initialZoom: 15,
            ),
            children: [
              TileLayer(
                urlTemplate: _showSatellite
                    ? 'https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}'
                    : 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                userAgentPackageName: 'com.example.comprafacil',
                tileProvider: CachedTileProvider(),
              ),
              if (delivererLatLng != null)
                PolylineLayer(
                  polylines: [
                    Polyline(
                      points: [delivererLatLng, customerLatLng],
                      color: Colors.blue,
                      strokeWidth: 4,
                    ),
                  ],
                ),
              MarkerLayer(
                markers: [
                  Marker(
                    point: customerLatLng,
                    width: 40,
                    height: 40,
                    child: const Icon(Icons.location_on, color: Colors.red, size: 40),
                  ),
                  if (delivererLatLng != null)
                    Marker(
                      point: delivererLatLng,
                      width: 40,
                      height: 40,
                      child: const Icon(Icons.delivery_dining, color: Colors.blue, size: 40),
                    ),
                ],
              ),
            ],
          ),
          if (_isDownloading)
            Positioned(
              top: 0, left: 0, right: 0,
              child: LinearProgressIndicator(value: _downloadProgress, backgroundColor: Colors.white, color: Colors.orange),
            ),
          Positioned(
            bottom: 20, left: 20, right: 20,
            child: Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(widget.order.customerName ?? 'Cliente', style: const TextStyle(fontWeight: FontWeight.bold)),
                    Text(widget.order.location, style: const TextStyle(fontSize: 12, color: Colors.grey), textAlign: TextAlign.center),
                    const SizedBox(height: 12),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        ElevatedButton.icon(
                          onPressed: () => _mapController.move(customerLatLng, 15),
                          icon: const Icon(Icons.person_pin_circle),
                          label: const Text('Cliente'),
                        ),
                        if (delivererLatLng != null)
                          ElevatedButton.icon(
                            onPressed: () => _mapController.move(delivererLatLng, 15),
                            icon: const Icon(Icons.my_location),
                            label: const Text('Eu'),
                          ),
                      ],
                    )
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
