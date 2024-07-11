package com.example.wifi_app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.wifi_app/wifi"
    private val TAG = "MainActivity"
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private var methodResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "getAvailableWiFiNetworks") {
                methodResult = result
                checkAndRequestPermissions()
            } else {
                result.notImplemented()
            }
        }

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    if (success) {
                        handleScanResults()
                    } else {
                        Log.d(TAG, "Scan failed")
                        methodResult?.error("SCAN_FAILED", "WiFi scan failed", null)
                    }
                }
            }
        }
        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startWifiScan()
        }
    }

    private fun startWifiScan() {
        Log.d(TAG, "Starting WiFi scan")
        val scanSuccess = wifiManager.startScan()
        if (!scanSuccess) {
            Log.d(TAG, "Scan initiation failed")
            methodResult?.error("SCAN_FAILED", "WiFi scan initiation failed", null)
        }
    }

    private fun handleScanResults() {
        val scanResults: List<ScanResult> = wifiManager.scanResults
        if (scanResults.isEmpty()) {
            Log.d(TAG, "Scan results are empty")
        } else {
            Log.d(TAG, "Scan results found: ${scanResults.size}")
        }
        val wifiList = mutableListOf<Map<String, String>>()
        for (scanResult in scanResults) {
            val wifiInfo = mapOf(
                "SSID" to scanResult.SSID,
                "BSSID" to scanResult.BSSID,
                "capabilities" to scanResult.capabilities,
                "frequency" to scanResult.frequency.toString(),
                "level" to scanResult.level.toString()
            )
            wifiList.add(wifiInfo)
        }
        Log.d(TAG, "Returning WiFi list with size: ${wifiList.size}")
        methodResult?.success(wifiList)
        methodResult = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startWifiScan()
            } else {
                methodResult?.error("PERMISSION_DENIED", "Location permission denied", null)
                methodResult = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)
    }
}
