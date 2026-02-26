import 'package:flutter/material.dart';
import '../services/ota_stub.dart' if (dart.library.io) 'package:ota_update/ota_update.dart';
import '../services/update_service.dart';
import '../theme/app_theme.dart';

class UpdateDialog extends StatefulWidget {
  final String latestVersion;
  final String downloadUrl;

  const UpdateDialog({
    super.key,
    required this.latestVersion,
    required this.downloadUrl,
  });

  @override
  State<UpdateDialog> createState() => _UpdateDialogState();
}

class _UpdateDialogState extends State<UpdateDialog> {
  bool _isDownloading = false;
  double _progress = 0;
  String _statusMessage = 'Preparando download...';
  String? _errorMessage;

  void _startDownload() {
    setState(() {
      _isDownloading = true;
      _errorMessage = null;
    });

    UpdateService.downloadAndInstall(widget.downloadUrl).listen(
      (OtaEvent event) {
        setState(() {
          switch (event.status) {
            case OtaStatus.DOWNLOADING:
              _statusMessage = 'Baixando atualização...';
              _progress = double.tryParse(event.value ?? '0') ?? 0;
              break;
            case OtaStatus.INSTALLING:
              _statusMessage = 'Iniciando instalação...';
              _progress = 100;
              break;
            case OtaStatus.INSTALLATION_DONE:
              _statusMessage = 'Instalação concluída.';
              _isDownloading = false;
              break;
            case OtaStatus.ALREADY_RUNNING_ERROR:
              _errorMessage = 'Já existe um download em execução.';
              _isDownloading = false;
              break;
            case OtaStatus.PERMISSION_NOT_GRANTED_ERROR:
              _errorMessage = 'Permissão negada para instalar o aplicativo.';
              _isDownloading = false;
              break;
            case OtaStatus.INTERNAL_ERROR:
              _errorMessage = 'Erro interno ao processar atualização.';
              _isDownloading = false;
              break;
            case OtaStatus.DOWNLOAD_ERROR:
              _errorMessage = 'Erro ao baixar o arquivo. Verifique sua conexão.';
              _isDownloading = false;
              break;
            case OtaStatus.CHECKSUM_ERROR:
              _errorMessage = 'Erro de integridade no arquivo baixado.';
              _isDownloading = false;
              break;
            case OtaStatus.INSTALLATION_ERROR:
              _errorMessage = 'Erro ao instalar a atualização.';
              _isDownloading = false;
              break;
            case OtaStatus.CANCELED:
              _errorMessage = 'Download cancelado.';
              _isDownloading = false;
              break;
            default:
              _statusMessage = 'Processando...';
          }
        });

        if (event.status == OtaStatus.INSTALLING) {
           // On most Androids, the app will be closed here or the intent will take over.
        }
      },
      onError: (e) {
        setState(() {
          _isDownloading = false;
          _errorMessage = 'Ocorreu um erro inesperado: $e';
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async => !_isDownloading,
      child: AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            const Icon(Icons.system_update, color: AppTheme.primaryColor),
            const SizedBox(width: 10),
            const Text('Nova Atualização'),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (!_isDownloading) ...[
              Text('Uma nova versão (${widget.latestVersion}) está disponível.'),
              const SizedBox(height: 10),
              const Text('Deseja atualizar agora para aproveitar as melhorias?'),
            ] else ...[
              Text(_statusMessage),
              const SizedBox(height: 20),
              LinearProgressIndicator(
                value: _progress / 100,
                backgroundColor: Colors.grey[200],
                valueColor: const AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
              ),
              const SizedBox(height: 10),
              Center(
                child: Text(
                  '${_progress.toStringAsFixed(0)}%',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
            ],
            if (_errorMessage != null) ...[
              const SizedBox(height: 10),
              Text(
                _errorMessage!,
                style: const TextStyle(color: Colors.red, fontSize: 12),
              ),
            ],
          ],
        ),
        actions: _isDownloading
            ? []
            : [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('DEPOIS', style: TextStyle(color: Colors.grey)),
                ),
                ElevatedButton(
                  onPressed: _startDownload,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryColor,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  ),
                  child: const Text('ATUALIZAR AGORA'),
                ),
              ],
      ),
    );
  }
}
