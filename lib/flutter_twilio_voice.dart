import 'dart:async' show Completer;
import 'dart:collection' show HashMap;

import 'package:flutter/services.dart'
    show PlatformException, MethodChannel, MethodCall;
import 'package:flutter_twilio_voice/constants/method_channel_methods.dart';
import 'package:flutter_twilio_voice/exceptions/twilio_call_exceptions.dart';
import 'package:flutter_twilio_voice/models/call.dart';
import 'package:meta/meta.dart' show required;

import 'enum/selected_audio.dart';

class FlutterTwilioVoice {
  static const MethodChannel _channel =
      const MethodChannel('flutter_twilio_voice');
  bool isCalling = false,
      isRinging = false,
      isConnected = false,
      onHold = false,
      onSpeaker = true,
      onMute = false;

  final Function onConnected;
  final Function onDisconnected;
  final Function onPermissionDenied;
  final Function onConnectFailure;
  final Function onRinging;
  final Function onBluetoothDeviceChanged;
  final String defaultIcon;

  String bluetoothDevice;
  SelectedAudio selectedAudio;

  FlutterTwilioVoice(
      {@required this.defaultIcon,
      @required this.onConnected,
      @required this.onPermissionDenied,
      @required this.onConnectFailure,
      @required this.onDisconnected,
      @required this.onRinging,
      @required this.onBluetoothDeviceChanged}) {
    _listenToMethodCalls();
  }

  Future<void> connectCall({@required Call call}) async {
    Completer<void> completer = Completer();

    _channel.invokeMethod(MethodChannelMethods.CALL, {
      'to': call.to,
      'accessToken': call.accessToken,
      'name': call.name,
      'locationId': call.locationId,
      'callerId': call.callerId,
      'icon': defaultIcon
    }).then((value) {
      completer.complete();
    }, onError: (Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });
    return completer.future;
  }

  Future<bool> toggleHold() async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.HOLD)
        .then((result) => completer.complete(result))
        .catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });
    return completer.future;
  }

  Future<bool> toggleBluetooth(bool value) async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod("bluetooth", {"bluetooth": value ?? true}).then((result) {
      print("Bluetooth result: $result");
      if (result != null) {
        bluetoothDevice = result;
        if (bluetoothDevice != null) {
          selectedAudio = SelectedAudio.Bluetooth;
        }
        onBluetoothDeviceChanged();
        return true;
      } else
        return false;
    }).catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });
    return completer.future;
  }

  Future<bool> toggleSpeaker(bool speaker) async {
    onSpeaker = speaker;
    Completer<bool> completer = Completer();
    _channel.invokeMethod(MethodChannelMethods.SPEAKER,
        {'speaker': speaker ?? false}).then((result) {
      print("Speaker result: $speaker");

      onSpeaker
          ? selectedAudio = SelectedAudio.Speaker
          : selectedAudio = SelectedAudio.Phone;
      completer.complete(result);
    }).catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });

    return completer.future;
  }

  Future<bool> toggleMute() async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.MUTE)
        .then((result) => completer.complete(result))
        .catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });

    return completer.future;
  }

  Future<void> pressKey({@required String keyValue}) {
    Completer<void> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.KEY_PRESS, {'digit': keyValue})
        .then((result) => completer.complete())
        .catchError((Object e) {
          if (e is PlatformException)
            throw TwilioCallException(error: e.message);
          else
            throw e;
        });

    return completer.future;
  }

  Future<void> disconnectCall() {
    Completer<void> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.DISCONNECT)
        .then((result) => completer.complete())
        .catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });

    return completer.future;
  }

  void _listenToMethodCalls() {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == 'call_listener') {
        String status = call.arguments['status'];
        switch (status) {
          case 'ringing':
            isCalling = isRinging = true;
            onRinging();
            break;
          case 'permission_denied':
            onPermissionDenied();
            break;
          case 'disconnected':
            onDisconnected();
            break;
          case 'connect_failure':
            isConnected = false;
            isCalling = isRinging = false;
            onConnectFailure();
            break;
          case 'connected':
            isConnected = true;
            String sid, from, status;
            if (call.arguments != null) {
              sid = call.arguments['sid'];
              from = call.arguments['from'];
              status = call.arguments['status'];
            }

            onConnected(new HashMap()
              ..addAll({
                if (sid != null) 'sid': sid,
                if (from != null) 'from': from,
                if (status != null) 'status': status
              }));
            break;
          case 'deviceUpdate':
            String selected = call.arguments['selectedDevice'];
            String allDevice = call.arguments['devices'];
            print('Selected: $selected, All: $allDevice');
        }
      }
    });
  }
}
