package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityBleBinding
import com.example.myapplication.service.BluetoothLeService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BLEActivity : AppCompatActivity() {

    private val REQUEST_CHECK_SETTINGS: Int = 312
    private val TAG = "BluetoothLeService"
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val PERMISSIONS_12_PLUS = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val SCAN_PERIOD: Long = 10000

    private lateinit var binding: ActivityBleBinding

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private var scanning = false
    private var bluetoothService: BluetoothLeService? = null
    private var deviceAddress1: String? = null
    private var deviceAddress2: String? = null
    private var connected = false

    private lateinit var bluetoothPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val enableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Snackbar.make(binding.root, "Bluetooth enabled", Snackbar.LENGTH_SHORT).show()
            enableLocation()
        } else {
            Toast.makeText(this, "Failed to turn on bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
//            leDeviceListAdapter.addDevice(result.device)

            if (result.device.name == BluetoothLeService.DEVICE_NAME_1) {
                Log.v("BluetoothLeService", "device 1 name: ${result.device.name}")
                deviceAddress1 = result.device.address
                bluetoothService?.connect1(result.device.address)
            } else if (result.device.name == BluetoothLeService.DEVICE_NAME_2) {
                Log.v("BluetoothLeService", "device 2 name: ${result.device.name}")
                deviceAddress2 = result.device.address
                bluetoothService?.connect2(result.device.address)
            }

        }
    }

    //Bluetooth LE service
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                // call functions on service to check connection and connect to devices
                bluetooth.initialize()
                deviceAddress1?.let {
                    bluetooth.connect1(it)
                }
                deviceAddress2?.let {
                    bluetooth.connect2(it)
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (isGranted.containsValue(false)) {
                Snackbar.make(binding.root, "Please provide all permissions", Snackbar.LENGTH_SHORT).show()
                bluetoothPermissionLauncher.launch(PERMISSIONS)
            } else if (!bluetoothAdapter.isEnabled) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetooth.launch(enableBTIntent)
            } else {
                enableLocation()
            }
        }

        bluetoothManager = getSystemService(
            BluetoothManager::class.java
        )
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        setClickListener()

    }

    private fun enableLocation() {

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000).apply {
            setWaitForAccurateLocation(false)
            setMinUpdateIntervalMillis(5000)
            setMaxUpdateDelayMillis(10000)
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@BLEActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun setClickListener() {
        binding.permissionButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermissions(PERMISSIONS_12_PLUS)) {
                    bluetoothPermissionLauncher.launch(PERMISSIONS_12_PLUS)
                    return@setOnClickListener
                }
            } else {
                if (!hasPermissions(PERMISSIONS)) {
                    bluetoothPermissionLauncher.launch(PERMISSIONS)
                    return@setOnClickListener
                }
            }

            if (!bluetoothAdapter.isEnabled) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetooth.launch(enableBTIntent)
                return@setOnClickListener
            }

            enableLocation()
        }

        binding.scanButton.setOnClickListener {
            scanLeDevice()
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun scanLeDevice() = lifecycleScope.launch(Dispatchers.IO) {
        if (!scanning) {
            lifecycleScope.launch(Dispatchers.IO) {
                delay(SCAN_PERIOD)
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BLEActivity, "scan time over", Toast.LENGTH_SHORT).show()
                }
            }
            scanning = true
            val scanFilters = listOf<ScanFilter>(
                ScanFilter.Builder()
                    .setDeviceName(BluetoothLeService.DEVICE_NAME_1)
                    .setServiceUuid(ParcelUuid(BluetoothLeService.SERVICE_UUID_1))
                    .build(),
                ScanFilter.Builder()
                    .setDeviceName(BluetoothLeService.DEVICE_NAME_2)
                    .setServiceUuid(ParcelUuid(BluetoothLeService.SERVICE_UUID_2))
                    .build()
            )
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()

            bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BLEActivity, "scanning started", Toast.LENGTH_SHORT).show()
            }
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BLEActivity, "scanning stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val gattUpdateReceiver1: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED_1 -> {
                    connected = true
                    binding.connectionTextView1.text = "ultrasonic: connected"
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED_1 -> {
                    connected = false
                    binding.connectionTextView1.text = "ultrasonic: disconnected"
                    Log.w(TAG, "1 disconnected")
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_1 -> {
                    Log.w(TAG, "1 new service discovered")
                    displayGattServices1(bluetoothService?.getSupportedGattServices1())
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE_1 -> {
                    val data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                    data?.let {
//                        Log.w(TAG, it)
                        binding.outputTextView1.text = it
                    }
                }
            }
        }
    }

    private val gattUpdateReceiver2: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED_2 -> {
                    connected = true
                    binding.connectionTextView2.text = "accelerometer: connected"
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED_2 -> {
                    connected = false
                    binding.connectionTextView2.text = "accelerometer: disconnected"
                    Log.w(TAG, "2 disconnected")
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_2 -> {
                    Log.w(TAG, "2 new service discovered")
                    displayGattServices2(bluetoothService?.getSupportedGattServices2())
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE_2 -> {
                    val data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                    data?.let {
//                        Log.w(TAG, it)
                        binding.outputTextView2.text = it
                    }
                }
            }
        }
    }

    private fun displayGattServices1(gattServices: List<BluetoothGattService?>?) {
        if (gattServices == null)
            return

        for (gattService in gattServices) {
            if (gattService == null)
                continue
            if (gattService.uuid == BluetoothLeService.SERVICE_UUID_1) {
                for (gattCharacteristic in gattService.characteristics) {
                    if (gattCharacteristic.uuid == BluetoothLeService.CHARACTERISTIC_UUID_1) {
                        Log.v(TAG, "characteristic 2 found")
                        //bluetoothService?.readCharacteristic(gattCharacteristic)
                        bluetoothService?.setCharacteristicNotification1(gattCharacteristic, true)
                        return
                    }
                }
            }
        }
        Log.v(TAG, "characteristic 2 not found")

    }

    private fun displayGattServices2(gattServices: List<BluetoothGattService?>?) {
        if (gattServices == null)
            return

        for (gattService in gattServices) {
            if (gattService == null)
                continue
            if (gattService.uuid == BluetoothLeService.SERVICE_UUID_2) {
                for (gattCharacteristic in gattService.characteristics) {
                    if (gattCharacteristic.uuid == BluetoothLeService.CHARACTERISTIC_UUID_2) {
                        Log.v(TAG, "characteristic 2 found")
                        //bluetoothService?.readCharacteristic(gattCharacteristic)
                        bluetoothService?.setCharacteristicNotification2(gattCharacteristic, true)
                        return
                    }
                }
            }
        }
        Log.v(TAG, "characteristic 2 not found")

    }

    override fun onResume() {
        super.onResume()

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        registerReceiver(gattUpdateReceiver1, makeGattUpdateIntentFilter1())
        registerReceiver(gattUpdateReceiver2, makeGattUpdateIntentFilter2())
    }

    override fun onPause() {
        super.onPause()

        unbindService(serviceConnection)

        unregisterReceiver(gattUpdateReceiver1)
        unregisterReceiver(gattUpdateReceiver2)
    }

    private fun makeGattUpdateIntentFilter1(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED_1)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED_1)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_1)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_1)
        }
    }

    private fun makeGattUpdateIntentFilter2(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED_2)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED_2)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_2)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_2)
        }
    }
}
