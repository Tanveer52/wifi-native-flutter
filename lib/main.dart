import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: WiFiListScreen(),
    );
  }
}

class WiFiListScreen extends StatefulWidget {
  @override
  _WiFiListScreenState createState() => _WiFiListScreenState();
}

class _WiFiListScreenState extends State<WiFiListScreen> {
  static const platform = MethodChannel('com.example.wifi_app/wifi');
  List<Map<dynamic, dynamic>> wifiList = [];

  @override
  void initState() {
    super.initState();
    _getAvailableWiFiNetworks();
    platform.setMethodCallHandler((call) async {
      if (call.method == 'getAvailableWiFiNetworksResult') {
        setState(() {
          wifiList = List<Map<dynamic, dynamic>>.from(
              call.arguments.map((e) => Map<dynamic, dynamic>.from(e)));
          log('wifiList setState: $wifiList');
        });
      }
    });
  }

  Future<void> _getAvailableWiFiNetworks() async {
    try {
      final List<dynamic> result =
          await platform.invokeMethod('getAvailableWiFiNetworks');
      setState(() {
        wifiList = List<Map<dynamic, dynamic>>.from(
            result.map((e) => Map<dynamic, dynamic>.from(e)));
        log('wifiList: $wifiList');
      });
    } on PlatformException catch (e) {
      print("Failed to initiate WiFi scan: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Available WiFi Networks'),
      ),
      body: ListView.builder(
        itemCount: wifiList.length,
        itemBuilder: (context, index) {
          final wifi = wifiList[index];
          return ListTile(
            title: Text(wifi['SSID'] ?? 'Unknown SSID'),
            subtitle: Text(
                'BSSID: ${wifi['BSSID']}\nSignal Strength: ${wifi['level']}'),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _getAvailableWiFiNetworks,
        child: Icon(Icons.refresh),
      ),
    );
  }
}
