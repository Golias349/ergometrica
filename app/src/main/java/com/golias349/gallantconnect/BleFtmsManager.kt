package com.golias349.gallantconnect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import java.util.UUID

class BleFtmsManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onStatus(message: String)
        fun onDeviceFound(device: BluetoothDevice)
        fun onConnected(device: BluetoothDevice)
        fun onDisconnected()
        fun onBikeData(data: BikeData, raw: ByteArray)
        fun onError(message: String)
    }

    companion object {
        val FTMS_SERVICE_UUID: UUID =
            UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
        val INDOOR_BIKE_DATA_UUID: UUID =
            UUID.fromString("00002ad2-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var scanning = false

    @SuppressLint("MissingPermission")
    fun startScan() {
        val bt = adapter
        if (bt == null) {
            listener.onError("Este celular não possui Bluetooth.")
            return
        }
        if (!bt.isEnabled) {
            listener.onError("Ative o Bluetooth do celular.")
            return
        }

        stopScan()
        scanner = bt.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid(FTMS_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        listener.onStatus("Procurando dispositivos FTMS...")
        scanning = true
        scanner?.startScan(listOf(filter), settings, scanCallback)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopScan()
            if (scanning) {
                listener.onStatus("Busca encerrada.")
            }
        }, 15000)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (scanning) {
            scanner?.stopScan(scanCallback)
        }
        scanning = false
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        gatt?.close()
        listener.onStatus("Conectando em ${safeName(device)}...")
        gatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        listener.onDisconnected()
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            listener.onError("Não foi possível habilitar as notificações FTMS.")
            return
        }

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            listener.onError("CCCD da característica FTMS não encontrada.")
            return
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun safeName(device: BluetoothDevice): String =
        try {
            device.name?.takeIf { it.isNotBlank() } ?: "Dispositivo sem nome"
        } catch (_: SecurityException) {
            "Dispositivo Bluetooth"
        }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            listener.onDeviceFound(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            listener.onError("Falha na busca Bluetooth. Código: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                listener.onError("Erro GATT: $status")
                g.close()
                gatt = null
                return
            }

            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                listener.onConnected(g.device)
                listener.onStatus("Conectado. Procurando serviço FTMS...")
                g.discoverServices()
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                g.close()
                if (gatt === g) gatt = null
                listener.onDisconnected()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                listener.onError("Falha ao descobrir serviços: $status")
                return
            }

            val service = g.getService(FTMS_SERVICE_UUID)
            if (service == null) {
                listener.onError("Serviço FTMS (0x1826) não encontrado.")
                return
            }

            val characteristic = service.getCharacteristic(INDOOR_BIKE_DATA_UUID)
            if (characteristic == null) {
                listener.onError("Indoor Bike Data (0x2AD2) não encontrada.")
                return
            }

            listener.onStatus("FTMS encontrado. Ativando dados da bicicleta...")
            enableNotifications(g, characteristic)
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == CCCD_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    listener.onStatus("Recebendo dados da bicicleta.")
                } else {
                    listener.onError("Falha ao ativar notificações: $status")
                }
            }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == INDOOR_BIKE_DATA_UUID) {
                listener.onBikeData(FtmsParser.parse(value), value)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == INDOOR_BIKE_DATA_UUID) {
                val value = characteristic.value ?: return
                listener.onBikeData(FtmsParser.parse(value), value)
            }
        }
    }
}
