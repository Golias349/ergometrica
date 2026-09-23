package com.golias349.gallantconnect

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity(), BleFtmsManager.Listener {

    private lateinit var ble: BleFtmsManager
    private lateinit var statusText: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var wattsText: TextView
    private lateinit var cadenceText: TextView
    private lateinit var speedText: TextView
    private lateinit var distanceText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var rawText: TextView
    private lateinit var scanButton: Button
    private lateinit var disconnectButton: Button

    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val requestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        deviceList = findViewById(R.id.deviceList)
        wattsText = findViewById(R.id.wattsText)
        cadenceText = findViewById(R.id.cadenceText)
        speedText = findViewById(R.id.speedText)
        distanceText = findViewById(R.id.distanceText)
        heartRateText = findViewById(R.id.heartRateText)
        rawText = findViewById(R.id.rawText)
        scanButton = findViewById(R.id.scanButton)
        disconnectButton = findViewById(R.id.disconnectButton)

        ble = BleFtmsManager(this, this)

        scanButton.setOnClickListener {
            if (hasBluetoothPermissions()) {
                devices.clear()
                renderDevices()
                ble.startScan()
            } else {
                requestBluetoothPermissions()
            }
        }

        disconnectButton.setOnClickListener {
            ble.disconnect()
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            ble.startScan()
        } else {
            onStatus("Permissão Bluetooth não concedida.")
        }
    }

    private fun renderDevices() {
        deviceList.removeAllViews()

        if (devices.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Nenhum dispositivo encontrado ainda."
                setPadding(0, 12, 0, 12)
            }
            deviceList.addView(empty)
            return
        }

        devices.values.forEach { device ->
            val button = Button(this).apply {
                text = try {
                    "${device.name ?: "Dispositivo"}\n${device.address}"
                } catch (_: SecurityException) {
                    "Dispositivo Bluetooth"
                }
                setOnClickListener { ble.connect(device) }
            }
            deviceList.addView(button)
        }
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        runOnUiThread {
            val key = try { device.address } catch (_: SecurityException) { device.toString() }
            if (!devices.containsKey(key)) {
                devices[key] = device
                renderDevices()
            }
        }
    }

    override fun onStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }

    override fun onConnected(device: BluetoothDevice) {
        runOnUiThread {
            disconnectButton.isEnabled = true
            scanButton.isEnabled = false
            statusText.text = "Conectado"
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            disconnectButton.isEnabled = false
            scanButton.isEnabled = true
            statusText.text = "Desconectado"
        }
    }

    override fun onBikeData(data: BikeData, raw: ByteArray) {
        runOnUiThread {
            wattsText.text = "Potência: ${data.watts?.toString() ?: "—"} W"
            cadenceText.text = "Cadência: ${
                data.cadence?.let { String.format("%.1f", it) } ?: "—"
            } RPM"
            speedText.text = "Velocidade: ${
                data.speedKmh?.let { String.format("%.2f", it) } ?: "—"
            } km/h"
            distanceText.text = "Distância: ${data.distanceM?.toString() ?: "—"} m"
            heartRateText.text = "Frequência cardíaca: ${data.heartRate?.toString() ?: "—"} bpm"
            rawText.text = "FTMS bruto: ${FtmsParser.hex(raw)}"
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            statusText.text = "⚠ $message"
        }
    }

    override fun onDestroy() {
        ble.disconnect()
        super.onDestroy()
    }
}
