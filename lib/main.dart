import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: WiFiListScreen(),
    );
  }
}

class WiFiListScreen extends StatefulWidget {
  const WiFiListScreen({super.key});

  @override
  WiFiListScreenState createState() => WiFiListScreenState();
}

class WiFiListScreenState extends State<WiFiListScreen> {
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
      log("Failed to initiate WiFi scan: '${e.message}'.");
    }
  }

  Future<void> _connectToWiFi(String ssid, String password) async {
    try {
      final String result = await platform
          .invokeMethod('connectToWiFi', {'ssid': ssid, 'password': password});
      log("Connected to WiFi: $result");
    } on PlatformException catch (e) {
      log("Failed to connect to WiFi: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Available WiFi Networks'),
      ),
      body: ListView.builder(
        itemCount: wifiList.length,
        itemBuilder: (context, index) {
          final wifi = wifiList[index];
          return ListTile(
            title: Text(wifi['SSID'] ?? 'Unknown SSID'),
            subtitle: Text(
                'BSSID: ${wifi['BSSID']}\nSignal Strength: ${wifi['level']}'),
            onTap: () {
              _showConnectDialog(wifi['SSID']);
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _getAvailableWiFiNetworks,
        child: const Icon(Icons.refresh),
      ),
    );
  }

  void _showConnectDialog(String ssid) {
    final TextEditingController passwordController = TextEditingController();
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('Connect to $ssid'),
          content: TextField(
            controller: passwordController,
            decoration: const InputDecoration(labelText: 'Password'),
            obscureText: true,
          ),
          actions: <Widget>[
            TextButton(
              child: const Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: const Text('Connect'),
              onPressed: () {
                _connectToWiFi(ssid, passwordController.text);
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }
}
