import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:crypto/crypto.dart';
import 'dart:convert';
import '../theme/app_theme.dart';

class CachedAvatar extends StatefulWidget {
  final String? url;
  final double radius;
  final IconData placeholderIcon;

  const CachedAvatar({
    super.key,
    required this.url,
    this.radius = 40,
    this.placeholderIcon = Icons.person,
  });

  @override
  State<CachedAvatar> createState() => _CachedAvatarState();
}

class _CachedAvatarState extends State<CachedAvatar> {
  File? _localFile;
  double _downloadProgress = 0;
  bool _isDownloading = false;

  @override
  void initState() {
    super.initState();
    _checkCache();
  }

  @override
  void didUpdateWidget(CachedAvatar oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.url != widget.url) {
      _checkCache();
    }
  }

  Future<void> _checkCache() async {
    if (widget.url == null || widget.url!.isEmpty) {
      setState(() => _localFile = null);
      return;
    }

    final directory = await getApplicationDocumentsDirectory();
    final hash = md5.convert(utf8.encode(widget.url!)).toString();
    final file = File('${directory.path}/avatar_$hash.png');

    if (await file.exists()) {
      setState(() => _localFile = file);
    } else {
      _downloadImage(file);
    }
  }

  Future<void> _downloadImage(File file) async {
    if (widget.url == null || widget.url!.isEmpty) return;

    setState(() {
      _isDownloading = true;
      _downloadProgress = 0;
    });

    try {
      final dio = Dio();
      await dio.download(
        widget.url!,
        file.path,
        onReceiveProgress: (count, total) {
          if (total != -1) {
            setState(() {
              _downloadProgress = count / total;
            });
          }
        },
      );
      if (mounted) {
        setState(() {
          _localFile = file;
          _isDownloading = false;
        });
      }
    } catch (e) {
      debugPrint('Error downloading avatar: $e');
      if (mounted) {
        setState(() => _isDownloading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: Alignment.center,
      children: [
        if (_isDownloading)
          SizedBox(
            width: (widget.radius * 2) + 8,
            height: (widget.radius * 2) + 8,
            child: CircularProgressIndicator(
              value: _downloadProgress,
              strokeWidth: 3,
              backgroundColor: Colors.white24,
              valueColor: const AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
            ),
          ),
        CircleAvatar(
          radius: widget.radius,
          backgroundColor: Colors.white24,
          backgroundImage: _localFile != null ? FileImage(_localFile!) : null,
          child: _localFile == null && !_isDownloading
              ? Icon(widget.placeholderIcon, size: widget.radius, color: Colors.white)
              : null,
        ),
      ],
    );
  }
}
