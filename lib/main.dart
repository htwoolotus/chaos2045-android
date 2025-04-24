import 'package:flutter/material.dart';
import 'package:receive_sharing_intent/receive_sharing_intent.dart';
import 'dart:io';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'services/auth_service.dart';
import 'pages/home_page.dart';
import 'pages/share_page.dart';
import 'pages/login_page.dart';
import 'pages/settings_page.dart';
import 'utils/toast_util.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<AuthService>(
          create: (_) => AuthService(),
        ),
      ],
      child: MaterialApp(
        scaffoldMessengerKey: ToastUtil.messengerKey,
        title: 'ShareIt',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        ),
        initialRoute: '/',
        routes: {
          '/': (context) => const AuthWrapper(),
          '/home': (context) => const MainPage(),
        },
      ),
    );
  }
}

class AuthWrapper extends StatelessWidget {
  const AuthWrapper({super.key});

  @override
  Widget build(BuildContext context) {
    final authService = Provider.of<AuthService>(context);

    return StreamBuilder<User?>(
      stream: authService.authStateChanges,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.active) {
          final user = snapshot.data;
          if (user == null) {
            return const LoginPage();
          }
          return const MainPage();
        }
        return const Scaffold(
          body: Center(
            child: CircularProgressIndicator(),
          ),
        );
      },
    );
  }
}

class MainPage extends StatefulWidget {
  const MainPage({super.key});

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  int _selectedIndex = 0;
  String? _sharedText;
  List<String>? _sharedImages;

  @override
  void initState() {
    super.initState();
    _initSharing();
  }

  final List<Widget> _pages = [
    const HomePage(),
    const SharePage(),
    const SettingsPage(),
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  void _initSharing() {
    ReceiveSharingIntent.instance.getMediaStream().listen((List<SharedMediaFile> value) {
      if (value.isNotEmpty) {
        _handleSharedContent(value);
      }
    });

    ReceiveSharingIntent.instance.getInitialMedia().then((List<SharedMediaFile> value) {
      if (value.isNotEmpty) {
        _handleSharedContent(value);
      }
    });
  }

  void _handleSharedContent(List<SharedMediaFile> value) {
    for (var file in value) {
      if (file.type == SharedMediaType.text) {
        setState(() {
          _sharedText = file.path;
        });
        _showShareDialog('收到文本分享: ${file.path}');
        debugPrint('Shared text: ${file.path}');
      } else if (file.type == SharedMediaType.image) {
        List<String>? sharedImages;
        sharedImages = [...?sharedImages, file.path];
        _showShareDialog('收到图片分享');
        debugPrint('Shared images: $sharedImages');
      }
    }
  }

  void _showShareDialog(String message) {
    showDialog(
      context: context,
      barrierDismissible: false, // 点击外部不关闭
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('分享内容已接收'),
          content: Text(message),
          actions: <Widget>[
            TextButton(
              child: const Text('返回分享App'),
              onPressed: () {
                Navigator.of(context).pop();
                // 使用系统服务返回上一个应用
                SystemNavigator.pop();
              },
            ),
            TextButton(
              child: const Text('留在当前App'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('ShareIt'),
      ),
      body: _pages[_selectedIndex],
      bottomNavigationBar: BottomNavigationBar(
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(Icons.home),
            label: '首页',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.share),
            label: '分享',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: '设置',
          ),
        ],
        currentIndex: _selectedIndex,
        selectedItemColor: Colors.deepPurple,
        onTap: _onItemTapped,
      ),
    );
  }
}
