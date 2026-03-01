import 'dart:typed_data';

enum OtaStatus {
  DOWNLOADING,
  INSTALLING,
  INSTALLATION_DONE,
  ALREADY_RUNNING_ERROR,
  PERMISSION_NOT_GRANTED_ERROR,
  INTERNAL_ERROR,
  DOWNLOAD_ERROR,
  CHECKSUM_ERROR,
  INSTALLATION_ERROR,
  CANCELED
}

class OtaEvent {
  final OtaStatus status;
  final String? value;
  OtaEvent(this.status, this.value);
}

class OtaUpdate {
  Stream<OtaEvent> execute(String url, {String? destinationFilename, Map<String, String>? headers}) {
    return Stream.value(OtaEvent(OtaStatus.INTERNAL_ERROR, 'Not supported on this platform'));
  }
}

// dart:io stubs
class File {
  final String path;
  File(this.path);
  Future<bool> exists() async => false;
  Future<void> create({bool recursive = false}) async {}
  Future<void> writeAsBytes(List<int> bytes) async {}
  Future<Uint8List> readAsBytes() async => Uint8List(0);
}

class Directory {
  final String path;
  Directory(this.path);
}

Future<Directory> getTemporaryDirectory() async => Directory('');
