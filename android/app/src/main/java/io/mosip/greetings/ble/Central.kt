package io.mosip.greetings.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import io.mosip.greetings.chat.ChatManager
import uniffi.identity.decrypt
import uniffi.identity.encrypt
import java.nio.charset.Charset

// Sequence of actions
// Scanning -> Connecting -> Discover Services -> Subscribes to Read Characteristic
class Central : ChatManager {
    private lateinit var updateLoadingText: (String) -> Unit
    private var scanning: Boolean = false
    private var connected: Boolean = false
    private lateinit var peripheralDevice: BluetoothDevice
    private lateinit var onDeviceConnected: () -> Unit
    private lateinit var onMessageReceived: (String) -> Unit
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var onDeviceFound: () -> Unit
    private lateinit var onConnectionFailure: (String) -> Unit
    private lateinit var bluetoothGatt: BluetoothGatt
    private var servicesDiscoveryRetryCounter = 3

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.i("BLE Central", "Status of write is $status for ${characteristic?.uuid}")

            if(status != BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLE", "\"Failed to send message to peripheral")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            val decryptedMsg = decrypt(characteristic.value.toUByteArray().asList())
            Log.i("BLE Central", "Characteristic changed to ${decryptedMsg}")
            onMessageReceived(decryptedMsg)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.i("BLE Central", "$status + ${descriptor?.uuid}")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLE Central", "Subscribed to read messages from peripheral")
                updateLoadingText("Subscribed to peripheral")
            } else {
                onConnectionFailure("Failed to Subscribe to read messages from peripheral. Please retry connecting.")
                Log.i("BLE Central", "Failed to Subscribe to read messages from peripheral")

                updateLoadingText("Failed to subscribe to peripheral")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            onDeviceConnected()
            Log.i("BLE Central", "Successfully changed mtu size: " +  mtu)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLE Central", "Failed to discover services")
                return
            }

            Log.i("BLE Central", "discovered services: ${gatt?.services?.map { it.uuid }}")
            updateLoadingText("Discovered Services.")

            if (gatt != null) {
                bluetoothGatt = gatt
            }

            val hasPeripheralService = gatt?.services?.map { it.uuid }?.contains(Peripheral.serviceUUID)

            if(hasPeripheralService == true) {
                Log.i("BLE Central", "Device is connected")
                val success = gatt.requestMtu(517)
                Log.i("BLE Central", "Word size: $success")
                servicesDiscoveryRetryCounter = 3
            } else if(servicesDiscoveryRetryCounter > 0) {
                Log.i("BLE", "Retrying discover services times: $servicesDiscoveryRetryCounter")
                updateLoadingText("Retrying discover Services: $servicesDiscoveryRetryCounter")
                servicesDiscoveryRetryCounter--

                Handler(Looper.getMainLooper()).postDelayed({
                    gatt?.discoverServices()
                }, 300)

            } else {
                onConnectionFailure("Unable to discover the peripheral service. Please retry connecting.")
                servicesDiscoveryRetryCounter = 3
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE Central", "Connected to the peripheral")
                updateLoadingText("Connected to the peripheral")
                connected = true
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("BLE Central", "Disconnected from the peripheral")

                connected = false
                gatt?.disconnect()
                gatt?.close()
            }
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.i("BLE Central", "Found the device: $result")
            stopScan()

            super.onScanResult(callbackType, result)
            peripheralDevice = result.device

            updateLoadingText("Found a peripheral with name: ${peripheralDevice.name}")
            onDeviceFound()
        }
    }

    companion object {
        @Volatile
        private lateinit var instance: Central

        fun getInstance(): Central {
            synchronized(this) {
                if (!::instance.isInitialized) {
                    instance = Central()
                }
                return instance
            }
        }
    }

    override fun addMessageReceiver(onMessageReceived: (String) -> Unit) {
        if (!connected) {
            Log.e("BLE Central", "Peripheral is not connected")
            return
        }

        this.onMessageReceived = onMessageReceived
        subscribeToMessages()

    }

    private fun subscribeToMessages() {
        Log.i("BLE Central", "Subscribing to read message char")
        val service = bluetoothGatt.getService(Peripheral.serviceUUID)
        val readChar = service.getCharacteristic(Peripheral.READ_MESSAGE_CHAR_UUID)
        bluetoothGatt.setCharacteristicNotification(readChar, true)

        val descriptor: BluetoothGattDescriptor =
            readChar.getDescriptor(UUIDHelper.uuidFromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        val status = bluetoothGatt.writeDescriptor(descriptor)
        Log.i("BLE Central", "Raised subscription to peripheral: $status")
    }

    override fun sendMessage(message: String): String? {
        if (!connected) {
            Log.e("BLE Central", "Peripheral is not connected")
            return "Peripheral is not connected"
        }

        val service = bluetoothGatt.getService(Peripheral.serviceUUID)
        val writeChar = service.getCharacteristic(Peripheral.WRITE_MESSAGE_CHAR_UUID)
//        val value = message.toByteArray(Charset.defaultCharset())
        val encryptedMsg = encrypt(message)
        writeChar.value = encryptedMsg.toUByteArray().toByteArray()
        writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        val status = bluetoothGatt.writeCharacteristic(writeChar)
        Log.i("BLE Central", "Sent message to peripheral: $status")

        return null
    }

    override fun name(): String = "Central"

    fun startScanning(
        context: Context,
        onDeviceFound: () -> Unit,
        updateLoadingText: (String) -> Unit
    ) {
        init(onDeviceFound, context)
        this.updateLoadingText = updateLoadingText

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (scanning)
                stopScan()
        }, 100000)

        scanning = true
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(Peripheral.serviceUUID))
            .build()

        bluetoothLeScanner.startScan(
            mutableListOf(filter),
            ScanSettings.Builder().build(),
            leScanCallback
        )
    }

    private fun init(onDeviceFound: () -> Unit, context: Context) {
        this.onDeviceFound = onDeviceFound
        val bluetoothManager: BluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    fun stopScan() {
        bluetoothLeScanner.stopScan(leScanCallback)
    }

    fun connect(
        context: Context,
        onDeviceConnected: () -> Unit,
        onConnectionFailure: (String) -> Unit
    ) {
        Log.i("BLE Central", "Connecting to Peripheral")
        this.onDeviceConnected = onDeviceConnected
        this.onConnectionFailure = onConnectionFailure

        val gatt = peripheralDevice.connectGatt(
            context,
            false,
            bluetoothGattCallback,
            BluetoothDevice.TRANSPORT_LE
        )

        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
    }

}
