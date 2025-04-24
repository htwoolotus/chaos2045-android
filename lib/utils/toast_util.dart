import 'package:flutter/material.dart';

class ToastUtil {
  static final GlobalKey<ScaffoldMessengerState> _messengerKey = GlobalKey<ScaffoldMessengerState>();

  static GlobalKey<ScaffoldMessengerState> get messengerKey => _messengerKey;

  static void show(String message, {
    Duration duration = const Duration(seconds: 2),
    bool isError = false,
  }) {
    _messengerKey.currentState?.showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: const TextStyle(color: Colors.white),
        ),
        duration: duration,
        backgroundColor: isError ? Colors.red : Colors.black87,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        margin: const EdgeInsets.all(8),
      ),
    );
  }

  static void showSuccess(String message) {
    show(
      message,
      duration: const Duration(seconds: 2),
    );
  }

  static void showError(String message) {
    show(
      message,
      duration: const Duration(seconds: 3),
      isError: true,
    );
  }
} 