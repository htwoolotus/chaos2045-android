import 'package:flutter/material.dart';
import 'package:receive_sharing_intent/receive_sharing_intent.dart';
import 'dart:io';

class SharePageDetail extends StatefulWidget {
  const SharePageDetail({super.key});

  @override
  State<SharePageDetail> createState() => _SharePageDetailState();
}

class _SharePageDetailState extends State<SharePageDetail> {
  String? _sharedText;
  List<String>? _sharedImages;

  @override
  void initState() {
    super.initState();
    try {
      // 初始化共享功能
      _initSharing();
    } catch (e) {
        // 处理其他类型的异常
        debugPrint('初始化分享功能时出错: $e');
    }
  }

  void _initSharing() {
    ReceiveSharingIntent.instance.getMediaStream().listen((List<SharedMediaFile> value) {
      for (var file in value) {
        if (file.type == SharedMediaType.text) {
          setState(() {
            _sharedText = file.path;
          });
        } else if (file.type == SharedMediaType.image) {
          setState(() {
            _sharedImages = [...?_sharedImages, file.path];
          });
        }
      }
    });

    ReceiveSharingIntent.instance.getInitialMedia().then((List<SharedMediaFile> value) {
      for (var file in value) {
        if (file.type == SharedMediaType.text) {
          setState(() {
            _sharedText = file.path;
          });
        } else if (file.type == SharedMediaType.image) {
          setState(() {
            _sharedImages = [...?_sharedImages, file.path];
          });
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          if (_sharedText != null)
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Text('分享的文本: $_sharedText'),
            ),
          if (_sharedImages != null && _sharedImages!.isNotEmpty)
            Expanded(
              child: ListView.builder(
                itemCount: _sharedImages!.length,
                itemBuilder: (context, index) {
                  return Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Image.file(
                      File(_sharedImages![index]),
                      height: 200,
                      fit: BoxFit.cover,
                    ),
                  );
                },
              ),
            ),
          if (_sharedText == null && (_sharedImages == null || _sharedImages!.isEmpty))
            const Text('等待接收分享内容...'),
        ],
      ),
    );
  }
} 