import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../utils/toast_util.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    final authService = Provider.of<AuthService>(context);

    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ListTile(
              leading: const Icon(Icons.logout),
              title: const Text('退出登录'),
              titleAlignment: ListTileTitleAlignment.center,
              onTap: () async {
                try {
                  await authService.signOut();
                  if (!context.mounted) return;
                  Navigator.of(context).pushNamedAndRemoveUntil('/', (Route<dynamic> route) => false);
                } catch (e) {
                  if (!context.mounted) return;
                  ToastUtil.show('退出登录失败: $e', isError: true);
                  debugPrint('退出登录错误: $e');
                }
              },
            ),
          ],
        ),
      ),
    );
  }
} 