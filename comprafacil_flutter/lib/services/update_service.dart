import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:ota_update/ota_update.dart';

class UpdateService {
  static Future<Map<String, dynamic>?> checkForUpdate() async {
    try {
      final supabase = Supabase.instance.client;
      final packageInfo = await PackageInfo.fromPlatform();
      final currentVersion = packageInfo.version;

      final response = await supabase
          .from('app_config')
          .select()
          .or('key.eq.latest_version,key.eq.download_url');

      if (response == null || response.isEmpty) return null;

      String? latestVersion;
      String? downloadUrl;

      for (var item in response) {
        if (item['key'] == 'latest_version') {
          latestVersion = item['value'].toString().replaceAll('"', '');
        } else if (item['key'] == 'download_url') {
          downloadUrl = item['value'].toString().replaceAll('"', '');
        }
      }

      // If latest_version is not set, check min_version as fallback
      if (latestVersion == null) {
         final minVerResponse = await supabase
          .from('app_config')
          .select()
          .eq('key', 'min_version')
          .maybeSingle();
         if (minVerResponse != null) {
           latestVersion = minVerResponse['value'].toString().replaceAll('"', '');
         }
      }

      if (latestVersion != null && downloadUrl != null) {
        if (_isNewerVersion(currentVersion, latestVersion)) {
          return {
            'latestVersion': latestVersion,
            'downloadUrl': downloadUrl,
            'currentVersion': currentVersion,
          };
        }
      }
    } catch (e) {
      print('Error checking for update: $e');
    }
    return null;
  }

  static bool _isNewerVersion(String current, String latest) {
    try {
      List<int> currentParts = current.split('.').map((e) => int.tryParse(e) ?? 0).toList();
      List<int> latestParts = latest.split('.').map((e) => int.tryParse(e) ?? 0).toList();

      for (int i = 0; i < latestParts.length; i++) {
        int c = i < currentParts.length ? currentParts[i] : 0;
        int l = latestParts[i];
        if (l > c) return true;
        if (l < c) return false;
      }
    } catch (e) {
      return latest != current;
    }
    return false;
  }

  static Stream<OtaEvent> downloadAndInstall(String url) {
    return OtaUpdate().execute(
      url,
      destinationFilename: 'comprafacil_update.apk',
    );
  }
}
