package io.mosip.greetings.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.reflect.KFunction0


class Common {
    companion object {
        // For Central and Android 9 (API LEVEL 28) or lower
        // Requires : Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION
        //
        // For Peripheral and Android 9 (API LEVEL 28) or lower
        // Requires : Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN
        //
        // For Central and Android 11 (API LEVEL 30) or lower
        // Requires : Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION
        //
        // For Peripheral and Android 11 (API LEVEL 30) or lower
        // Requires : Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN
        //
        // For Central and Android 12 (API LEVEL 31 & 32) or higher
        // Requires : Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN
        //
        // For Peripheral and Android 12 (API LEVEL 31 & 32) or higher
        // Requires : Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE
        //
        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 2
        private var REQUIRED_PERMISSIONS: Array<String>
        private val REQUIRED_PERMISSIONS_FOR_API_LEVEL_31_AND_ABOVE = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
        private val REQUIRED_PERMISSIONS_FOR_API_LEVEL_BELOW_31 = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        init {
            REQUIRED_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= 31) {
                REQUIRED_PERMISSIONS_FOR_API_LEVEL_31_AND_ABOVE
            } else {
                REQUIRED_PERMISSIONS_FOR_API_LEVEL_BELOW_31
            }
        }
        private const val REQUEST_ENABLE_BT = 3

        private fun checkPermissions(context: Context?): Boolean {
            for (permission in REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(
                        context!!,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        fun requestForRequiredPermissions(
            activity: Activity,
            context: Context,
            showActionsView: KFunction0<Unit>
        ) {
            if (!checkPermissions(context)) {
                ActivityCompat.requestPermissions(
                    activity,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_REQUIRED_PERMISSIONS
                )
                Toast.makeText(context, "Requested permissions", Toast.LENGTH_SHORT).show()
                return
            }

            showActionsView()
            startBluetooth(activity)
        }

        private fun startBluetooth(activity: Activity) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }
}