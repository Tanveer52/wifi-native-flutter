package com.example.wifi_app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val WIFI_CHANNEL = "com.example.wifi_app/wifi"
    private val TAG = "MainActivity"
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private var eventSink: EventChannel.EventSink? = null
    private val scanInterval = 10000L // 10 seconds
    private val handler = Handler(Looper.getMainLooper())

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, WIFI_CHANNEL).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
                checkAndRequestPermissions()
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
                handler.removeCallbacks(scanRunnable)
            }
        })

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    if (success) {
                        handleScanResults()
                    } else {
                        Log.d(TAG, "Scan failed")
                        eventSink?.error("SCAN_FAILED", "WiFi scan failed", null)
                    }
                }
            }
        }
        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            startWifiScan()
            handler.postDelayed(this, scanInterval)
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startPeriodicScan()
        }
    }

    private fun startPeriodicScan() {
        handler.post(scanRunnable)
    }

    private fun startWifiScan() {
        Log.d(TAG, "Starting WiFi scan")
        val scanSuccess = wifiManager.startScan()
        if (!scanSuccess) {
            Log.d(TAG, "Scan initiation failed")
            eventSink?.error("SCAN_FAILED", "WiFi scan initiation failed", null)
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
        eventSink?.success(wifiList)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startPeriodicScan()
            } else {
                eventSink?.error("PERMISSION_DENIED", "Location permission denied", null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)
        handler.removeCallbacks(scanRunnable)
    }
}
