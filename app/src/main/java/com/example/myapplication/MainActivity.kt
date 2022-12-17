package com.example.myapplication

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    //first bluetooth
    private var bluetoothSocket1: BluetoothSocket? = null
    private var isBluetoothConnected1 = false

    //second bluetooth
    private var bluetoothSocket2: BluetoothSocket? = null
    private var isBluetoothConnected2 = false

    private val bluetoothConnPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            if (!bluetoothAdapter.isEnabled) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetooth.launch(enableBTIntent)
            }
        } else {
            Snackbar.make(binding.root, "Please provide bluetooth connect permission", Snackbar.LENGTH_SHORT).show()
        }
    }

    private val enableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Snackbar.make(binding.root, "Bluetooth enabled", Snackbar.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to turn on bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setClickListener()
    }

    private fun setClickListener() {
        binding.permissionButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    bluetoothConnPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    return@setOnClickListener
                }

            if (!bluetoothAdapter.isEnabled) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetooth.launch(enableBTIntent)
            }
        }

        binding.firstButton.setOnClickListener {
            connectDevice1()
        }

        binding.secondButton.setOnClickListener {
            connectDevice2()
        }
    }

    // Classic device
    private fun connectDevice1() = lifecycleScope.launch(Dispatchers.Main){
        if (bluetoothSocket1 != null && bluetoothSocket1!!.isConnected) {
            resetBluetoothConnection1()
            return@launch
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val deviceList: MutableList<BluetoothDevice> = ArrayList()
        val deviceNames: MutableList<String> = ArrayList()
        pairedDevices.forEach { device ->
            deviceList.add(device)
            deviceNames.add(device.name)
        }

        val bluetoothListDialog = AlertDialog.Builder(this@MainActivity)
        bluetoothListDialog.setTitle("Choose First bluetooth device")

        bluetoothListDialog.setItems(deviceNames.toTypedArray()) { dialog, which ->

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (bluetoothSocket1 == null || !isBluetoothConnected1) {
                        bluetoothSocket1 = deviceList[which].createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                        bluetoothAdapter.cancelDiscovery()
                        bluetoothSocket1!!.connect()
                        withContext(Dispatchers.Main) {
                            binding.firstButton.text = "Disconnect"
                        }
                        isBluetoothConnected1 = true

                        val buffer = ByteArray(1024)
                        var bytes: Int

                        while (isActive && isBluetoothConnected1) {
                            try {
                                bytes = bluetoothSocket1!!.inputStream!!.read(buffer)
                                val input = String(buffer, 0, bytes)

                                withContext(Dispatchers.Main) {
                                    binding.firstTextView.text = input
                                }
                            } catch (e: IOException) {
                                Log.v("MYTAG", "bluetooth: ${e.message}")
                            }
                        }

                    }
                } catch (e: IOException) {
                    isBluetoothConnected1 = false
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message ?: "Could not connect to device.Please turn on your Hardware", Toast.LENGTH_LONG).show()
                    }
                }

            }

        }

        val dialog = bluetoothListDialog.create()
        dialog.show()
    }

    // BLE device
    private fun connectDevice2() = lifecycleScope.launch(Dispatchers.Main) {
        if (bluetoothSocket2 != null && bluetoothSocket2!!.isConnected) {
            resetBluetoothConnection2()
            return@launch
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val deviceList: MutableList<BluetoothDevice> = ArrayList()
        val deviceNames: MutableList<String> = ArrayList()
        pairedDevices.forEach { device ->
            deviceList.add(device)
            deviceNames.add(device.name)
        }

        val bluetoothListDialog = AlertDialog.Builder(this@MainActivity)
        bluetoothListDialog.setTitle("Choose Second bluetooth device")

        bluetoothListDialog.setItems(deviceNames.toTypedArray()) { dialog, which ->

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (bluetoothSocket2 == null || !isBluetoothConnected2) {
                        bluetoothSocket2 = deviceList[which].createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                        bluetoothAdapter.cancelDiscovery()
                        bluetoothSocket2!!.connect()
                        withContext(Dispatchers.Main) {
                            binding.secondButton.text = "Disconnect"
                        }
                        isBluetoothConnected2 = true

                        val buffer = ByteArray(1024)
                        var bytes: Int

                        while (isActive && isBluetoothConnected2) {
                            try {
                                bytes = bluetoothSocket2!!.inputStream!!.read(buffer)
                                val input = String(buffer, 0, bytes)

                                withContext(Dispatchers.Main) {
                                    binding.secondTextView.text = input
                                }
                            } catch (e: IOException) {
                                Log.v("MYTAG", "bluetooth: ${e.message}")
                            }
                        }

                    }
                } catch (e: IOException) {
                    isBluetoothConnected2 = false
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message ?: "Could not connect to device.Please turn on your Hardware", Toast.LENGTH_LONG).show()
                    }
                }

            }

        }

        val dialog = bluetoothListDialog.create()
        dialog.show()
    }



    private suspend fun resetBluetoothConnection1() = withContext(Dispatchers.IO) {
        if (bluetoothSocket1 != null) {
            isBluetoothConnected1 = false
            try {
                bluetoothSocket1!!.close()
            } catch (e: Exception) {
            }
            bluetoothSocket1 = null
        }
        withContext(Dispatchers.Main) {
            binding.firstButton.text = "First Button"
        }
    }

    private suspend fun resetBluetoothConnection2() = withContext(Dispatchers.IO) {
        if (bluetoothSocket2 != null) {
            isBluetoothConnected2 = false
            try {
                bluetoothSocket2!!.close()
            } catch (e: Exception) {
            }
            bluetoothSocket2 = null
        }
        withContext(Dispatchers.Main) {
            binding.secondButton.text = "First Button"
        }
    }


}