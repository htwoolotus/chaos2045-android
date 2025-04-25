import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shareit/consts.dart';
import '../services/auth_service.dart';
import '../utils/toast_util.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  Future<void> _testProtectedEndpoint(BuildContext context, AuthService authService) async {
    try {
      final accessToken = await authService.getAccessToken();
      debugPrint('Retrieved access token: ${accessToken?.substring(0, 20)}...');
      
      if (accessToken == null) {
        ToastUtil.show('未找到访问令牌', isError: true);
        return;
      }

      final url = '${Consts.remoteUrl}/protected';
      debugPrint('Making request to: $url');
      
      final response = await http.get(
        Uri.parse(url),
        headers: {
          'Authorization': 'Bearer $accessToken',
        },
      );

      debugPrint('Response status code: ${response.statusCode}');
      debugPrint('Response body: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        ToastUtil.show('受保护端点测试成功: ${data['message']}');
      } else {
        ToastUtil.show('请求失败: ${response.statusCode}', isError: true);
      }
    } catch (e) {
      ToastUtil.show('测试失败: $e', isError: true);
      debugPrint('测试受保护端点错误: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final authService = Provider.of<AuthService>(context);

    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ListTile(
              leading: const Icon(Icons.security),
              title: const Text('测试受保护端点'),
              titleAlignment: ListTileTitleAlignment.center,
              onTap: () => _testProtectedEndpoint(context, authService),
            ),
            const Divider(),
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