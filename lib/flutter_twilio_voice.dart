import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_twilio_voice/models/call.dart';
import 'package:meta/meta.dart';

class FlutterTwilioVoice {
  static const MethodChannel _channel =
      const MethodChannel('flutter_twilio_voice');
  bool isCalling = false,
      isRinging = false,
      isConnected = false,
      onHold = false,
      onSpeaker = true,
      onMute = false;
  String accessToken, keyPressed = '';

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  FlutterTwilioVoice() {
    _listenToMethodCalls();
  }

  Future call({@required Call call}) async {
    var result = await _channel.invokeMethod("call", {
      "to": call.to,
      "accessToken": call.accessToken,
      "name": call.name,
      "locationId": call.locationId,
      "callerId": call.callerId,
    });
    return result;
  }

  Future<bool> hold() async {}

  Future<bool> speaker() async {}

  Future<bool> mute() async {}

  void keyPress() {}

  void disconnect() {}

  void _listenToMethodCalls() {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == "call_listener") {
        String status = call.arguments['status'];
        print('status: $status');
        switch (status) {
          case "ringing":
            isCalling = isRinging = true;
            break;
          case "permission_denied":
          case "disconnected":
          case "connect_failure":
            isConnected = false;
            isCalling = isRinging = false;
            // Navigator.of(context).pop(status == "permission_denied"
            //     ? "Permisssions denied"
            //     : status == "disconnected"
            //         ? "Call Disconnected"
            //         : "Connection failure");
            break;
          case "connected":
            isConnected = true;
            // _sendOutBoundMessage(call.arguments['sid'], call.arguments['from']);
            break;
        }
      }
    });
  }
}