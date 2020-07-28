import 'dart:async';

import 'package:flutter/services.dart';

class BackgroundLocation {
  static const MethodChannel _channel =
      const MethodChannel('background_location');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> LiveTracking(String url) async {
    final String version = await _channel.invokeMethod('LiveTracking', {'URL' : url});
    return version;
  }
}
