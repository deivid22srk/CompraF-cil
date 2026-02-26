import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:dio/dio.dart';
import 'package:comprafacil_flutter/utils/platform_stubs.dart' if (dart.library.io) 'dart:io' as io;
import 'package:comprafacil_flutter/utils/platform_stubs.dart' if (dart.library.io) 'package:path_provider/path_provider.dart' as path;
import 'dart:ui' as ui;
import 'dart:typed_data';

class CachedTileProvider extends TileProvider {
  @override
  ImageProvider getImage(TileCoordinates coordinates, TileLayer options) {
    final url = getTileUrl(coordinates, options);
    return CachedNetworkImageProvider(url, coordinates);
  }

  @override
  String getTileUrl(TileCoordinates coordinates, TileLayer options) {
    return options.urlTemplate!
        .replaceAll('{x}', coordinates.x.toString())
        .replaceAll('{y}', coordinates.y.toString())
        .replaceAll('{z}', coordinates.z.toString());
  }
}

class CachedNetworkImageProvider extends ImageProvider<CachedNetworkImageProvider> {
  final String url;
  final TileCoordinates coordinates;

  CachedNetworkImageProvider(this.url, this.coordinates);

  @override
  Future<CachedNetworkImageProvider> obtainKey(ImageConfiguration configuration) {
    return Future.value(this);
  }

  @override
  ImageStreamCompleter loadImage(CachedNetworkImageProvider key, ImageDecoderCallback decode) {
    return MultiFrameImageStreamCompleter(
      codec: _loadAsync(key, decode),
      scale: 1.0,
      debugLabel: key.url,
    );
  }

  Future<ui.Codec> _loadAsync(CachedNetworkImageProvider key, ImageDecoderCallback decode) async {
    if (kIsWeb) {
      // No caching on web to avoid dart:io issues
      final response = await Dio().get<List<int>>(url, options: Options(responseType: ResponseType.bytes));
      return decode(await ui.ImmutableBuffer.fromUint8List(Uint8List.fromList(response.data!)));
    }

    final cacheDir = await path.getTemporaryDirectory();
    final file = io.File('${cacheDir.path}/tiles/${coordinates.z}/${coordinates.x}/${coordinates.y}.png');

    if (await file.exists()) {
      final bytes = await file.readAsBytes();
      return decode(await ui.ImmutableBuffer.fromUint8List(bytes));
    }

    try {
      final response = await Dio().get<List<int>>(url, options: Options(responseType: ResponseType.bytes));
      final bytes = response.data!;
      await file.create(recursive: true);
      await file.writeAsBytes(bytes);
      return decode(await ui.ImmutableBuffer.fromUint8List(Uint8List.fromList(bytes)));
    } catch (e) {
      rethrow;
    }
  }

  @override
  bool operator ==(Object other) => other is CachedNetworkImageProvider && url == other.url;

  @override
  int get hashCode => url.hashCode;
}
