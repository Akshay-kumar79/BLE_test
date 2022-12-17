package com.example.myapplication.service

import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.nio.charset.Charset
import java.util.*

class BluetoothLeService : Service() {

    companion object {
        const val ACTION_GATT_CONNECTED_1 = "com.example.bluetooth.le.ACTION_GATT_CONNECTED_1"
        const val ACTION_GATT_DISCONNECTED_1 = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_1"
        const val ACTION_GATT_SERVICES_DISCOVERED_1 = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_1"
        const val ACTION_DATA_AVAILABLE_1 = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_1"

        const val ACTION_GATT_CONNECTED_2 = "com.example.bluetooth.le.ACTION_GATT_CONNECTED_2"
        const val ACTION_GATT_DISCONNECTED_2 = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_2"
        const val ACTION_GATT_SERVICES_DISCOVERED_2 = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_2"
        const val ACTION_DATA_AVAILABLE_2 = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_2"

        val EXTRA_DATA = "extra_data"
        val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val DEVICE_NAME_1 = "Utrasonic sensor"
        val SERVICE_UUID_1 = UUID.fromString("9180f838-c19f-4b66-8e90-6ed4264d56f1")
        val CHARACTERISTIC_UUID_1 = UUID.fromString("f1199236-fe1c-4141-a40c-2c646cfdf497")

        val DEVICE_NAME_2 = "Accelerometer"
        val SERVICE_UUID_2 = UUID.fromString("e7ba2b08-cd66-4c00-8dff-97b1046afb7c")
        val CHARACTERISTIC_UUID_2 = UUID.fromString("c9359722-ccb2-4170-a5ee-dc64e96fd823")

    }

    private val TAG = "BluetoothLeService"
    private val binder = LocalBinder()
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var connectionState1 = BluetoothProfile.STATE_DISCONNECTED
    private var connectionState2 = BluetoothProfile.STATE_DISCONNECTED
    private var bluetoothGatt1: BluetoothGatt? = null
    private var bluetoothGatt2: BluetoothGatt? = null

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun initialize(): Boolean {
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        return true
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    private val bluetoothGattCallback1 = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                connectionState1 = BluetoothProfile.STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED_1)
                Log.w(TAG, "Device 1 Connected")

                bluetoothGatt1?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                connectionState1 = BluetoothProfile.STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED_1)
                Log.w(TAG, "Device 1 Disconnected")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_1)
                Log.v(TAG, "on service 1 discover success")
            } else {
                Log.w(TAG, "onServicesDiscovered 1 received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE_1, characteristic)
//            Log.v(TAG, "on char 1 change")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            broadcastUpdate(ACTION_DATA_AVAILABLE_1, value, characteristic)
//            Log.v(TAG, "on char 1 change")
        }
    }

    private val bluetoothGattCallback2 = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                connectionState2 = BluetoothProfile.STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED_2)
                Log.w(TAG, "Device 2 Connected")

                bluetoothGatt2?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                connectionState2 = BluetoothProfile.STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED_2)
                Log.w(TAG, "Device 2 Disconnected")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_2)
                Log.v(TAG, "on service 2 discover success")
            } else {
                Log.w(TAG, "onServicesDiscovered 2 received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE_2, characteristic)
//            Log.v(TAG, "on char change")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            broadcastUpdate(ACTION_DATA_AVAILABLE_2, value, characteristic)
//            Log.v(TAG, "on char change")
        }
    }

    fun connect1(address: String): Boolean {
        bluetoothAdapter.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt1 = device.connectGatt(this, false, bluetoothGattCallback1)
                Log.v(TAG, "device 1 found")
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device 1 not found with provided address.")
                return false
            }
            // connect to the GATT server on the device
        }
    }

    fun connect2(address: String): Boolean {
        bluetoothAdapter.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt2 = device.connectGatt(this, false, bluetoothGattCallback2)
                Log.v(TAG, "device 2 found")
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device 2 not found with provided address.")
                return false
            }
            // connect to the GATT server on the device
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, value: ByteArray, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        when (characteristic.uuid) {
            CHARACTERISTIC_UUID_1, CHARACTERISTIC_UUID_2 -> {
                val str = value.toString(Charset.defaultCharset())
                intent.putExtra(EXTRA_DATA, str)
            }
            else -> {
                // do nothing
            }
        }

        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        when (characteristic.uuid) {
            CHARACTERISTIC_UUID_1, CHARACTERISTIC_UUID_2 -> {
                val str = characteristic.getStringValue(0)
                intent.putExtra(EXTRA_DATA, str)
            }
            else -> {
                // do nothing
            }
        }
        sendBroadcast(intent)
    }

    fun setCharacteristicNotification1(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        bluetoothGatt1?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, enabled)

            if (CHARACTERISTIC_UUID_1 == characteristic.uuid) {
                val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val a = gatt.writeDescriptor(descriptor)
                    Log.v(TAG, "descriptor 1 status: $a")
                } else {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                }
            }
        } ?: run {
            Log.w(TAG, "BluetoothGatt 1 not initialized")
        }
    }

    fun setCharacteristicNotification2(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        bluetoothGatt2?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, enabled)

            if (CHARACTERISTIC_UUID_2 == characteristic.uuid) {
                val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val a = gatt.writeDescriptor(descriptor)
                    Log.v(TAG, "descriptor 1 status: $a")
                } else {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                }
            }
        } ?: run {
            Log.w(TAG, "BluetoothGatt 1 not initialized")
        }
    }

    fun getSupportedGattServices1(): List<BluetoothGattService?>? {
        return bluetoothGatt1?.services
    }

    fun getSupportedGattServices2(): List<BluetoothGattService?>? {
        return bluetoothGatt2?.services
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private fun close() {
        bluetoothGatt1?.let { gatt ->
            gatt.close()
            bluetoothGatt1 = null
        }
        bluetoothGatt2?.let { gatt ->
            gatt.close()
            bluetoothGatt2 = null
        }
    }
}