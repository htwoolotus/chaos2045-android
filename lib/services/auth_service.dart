import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:flutter_facebook_auth/flutter_facebook_auth.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shareit/consts.dart';
import 'package:shareit/main.dart';

class AuthService {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final GoogleSignIn _googleSignIn = GoogleSignIn();
  final FacebookAuth _facebookAuth = FacebookAuth.instance;
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  
  // 获取当前用户
  User? get currentUser => _auth.currentUser;

  // 监听认证状态变化
  Stream<User?> get authStateChanges => _auth.authStateChanges();

  // Google 登录
  Future<UserCredential?> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      if (googleUser == null) return null;
      // displayName ="Sue Neo (慢慢来比较快)"；email ="neosue@gmail.com"
      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth.accessToken,
        idToken: googleAuth.idToken,
      );


      await _auth.signInWithCredential(credential);

       // 3. 获取ID Token并发送到后端
      final idToken = await FirebaseAuth.instance.currentUser?.getIdToken(true);
      debugPrint('idToken: $idToken');

      final response = await http.post(
        Uri.parse('${Consts.remoteUrl}/api/auth/google'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'idToken': idToken}),
      );
      
      if (response.statusCode == 200) {
        final tokenData = jsonDecode(response.body);
        await _secureStorage.write(
          key: 'access_token', 
          value: tokenData['access_token']
        );
        debugPrint('登录成功');
      } else {
        debugPrint('后端认证失败');
      }
    } catch (e) {
      debugPrint('Google 登录错误: $e');
      return null;
    }
  }

  // Facebook 登录
  Future<UserCredential?> signInWithFacebook() async {
    try {
      final LoginResult result = await _facebookAuth.login();
      if (result.status == LoginStatus.success) {
        final OAuthCredential credential = FacebookAuthProvider.credential(result.accessToken.toString());
        return await _auth.signInWithCredential(credential);
      }
      return null;
    } catch (e) {
      debugPrint('Facebook 登录错误: $e');
      return null;
    }
  }

  // --- 获取 ID Token ---
  Future<String?> getIdToken() async {
    final user = _auth.currentUser;
    if (user != null) {
      // forceRefresh 为 true 会确保获取最新的未过期的 token
      return await user.getIdToken();
    }
    return null;
  }

  // 退出登录
  Future<void> signOut() async {
      try {
        // 1. 获取当前用户的ID Token
        final user = _auth.currentUser;
        if (user != null) {
          // final idToken = await user.getIdToken();
          // 读取 _secureStorage 中的 access_token
          final accessToken = await _secureStorage.read(key: 'access_token');

          // 2. 调用后端登出API
          final response = await http.get(
            Uri.parse('${Consts.remoteUrl}/protected'),
            headers: {
              'Authorization': 'Bearer $accessToken',
            },
          );
          
          if (response.statusCode == 200) {
            debugPrint('后端登出成功');
          }
        }
        
        await _googleSignIn.signOut();
        // FIXME: 退出Facebook登录时，会报错
        // await _facebookAuth.logOut();
        await _auth.signOut();
      } catch (e) {
        debugPrint('登出过程中出错: $e');
      } finally {
        // 导航到登录页面或其他清理操作
      }
  }

  // 获取访问令牌
  Future<String?> getAccessToken() async {
    return await _secureStorage.read(key: 'access_token');
  }
}