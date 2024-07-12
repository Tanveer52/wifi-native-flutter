import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class WiFiScanner {
  static const EventChannel _wifiEventChannel =
      EventChannel('com.example.wifi_app/wifi');

  Stream<List<Map<dynamic, dynamic>>> getWiFiNetworks() {
    return _wifiEventChannel.receiveBroadcastStream().map((event) {
      return (event as List).map((e) => Map<dynamic, dynamic>.from(e)).toList();
    });
  }
}

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final WiFiScanner _wifiScanner = WiFiScanner();
  StreamSubscription<List<Map<dynamic, dynamic>>>? _subscription;
  List<Map<dynamic, dynamic>> _wifiList = [];

  @override
  void initState() {
    super.initState();
    _subscription = _wifiScanner.getWiFiNetworks().listen((wifiNetworks) {
      setState(() {
        _wifiList = wifiNetworks;
      });
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('WiFi Scanner'),
        ),
        body: ListView.builder(
          itemCount: _wifiList.length,
          itemBuilder: (context, index) {
            final wifi = _wifiList[index];
            return ListTile(
              title: Text(wifi['SSID']),
              subtitle:
                  Text('BSSID: ${wifi['BSSID']}, Signal: ${wifi['level']}'),
            );
          },
        ),
      ),
    );
  }
}
