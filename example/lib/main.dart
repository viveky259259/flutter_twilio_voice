import 'package:flutter/material.dart';
import 'package:flutter_twilio_voice/flutter_twilio_voice.dart';
import 'package:flutter_twilio_voice/models/call.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterTwilioVoice twilioVoice;
  bool isCalling = false,
      isRinging = false,
      isConnected = false,
      onHold = false,
      onSpeaker = true,
      onMute = false;

  @override
  void initState() {
    super.initState();
    twilioVoice = FlutterTwilioVoice(
        defaultIcon: "periscope",
        onRinging: () {
          isCalling = isRinging = true;
          setState(() {});
        },
        onConnectFailure: () {
          closeScreen('Connection Failure');
        },
        onConnected: (data) {
          isConnected = true;
          setState(() {});
        },
        onPermissionDenied: () {
          closeScreen('Permission Denied');
        },
        onDisconnected: () {
          closeScreen('Call Disconnected');
        });
    _fetchButtonStates();
    _makeCall();
  }

  void _makeCall() async {
    twilioVoice
        .connectCall(
            call: Call(
                accessToken: 'accessToken',
                name: 'widget.contact.fullName',
                dataToSend: {'var': 'value'}))
        .catchError((e) {
      print(e);
    });
  }

  @override
  Widget build(BuildContext context) {}

  void closeScreen(String message) {
    isConnected = false;
    isCalling = isRinging = false;
    // code to act on closing of call/error/permission denied
  }

  void _toggleHold() async {
    var result = await twilioVoice.hold();
    print("hold $result");
    setState(() {
      onHold = result;
    });
  }

  void _toggleMute() async {
    var result = await twilioVoice.mute();
    print("mute $result");
    setState(() {
      onMute = result;
    });
  }

  void _toggleSpeaker(bool speaker) async {
    var result = await twilioVoice.speaker(speaker);
    print("speaker $result");
    setState(() {
      onSpeaker = result;
    });
  }

  void _disconnect() {
    twilioVoice.disconnectCall();
  }

  @override
  void dispose() {
    _disconnect();
    super.dispose();
  }

  void _fetchButtonStates() {
    _toggleMute();
    _toggleHold();
    _toggleSpeaker(false);
  }
}
