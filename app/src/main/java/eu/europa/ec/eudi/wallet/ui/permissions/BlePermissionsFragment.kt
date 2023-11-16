/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.eudi.wallet.ui.permissions

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import eu.europa.ec.eudi.wallet.ui.databinding.FragmentBlePermissionsBinding
import eu.europa.ec.eudi.wallet.ui.util.log

/**
 * BlePermissionsFragment enables the required permissions for BLE functionality
 */
class BlePermissionsFragment private constructor() : BottomSheetDialogFragment() {

    private var isCentralClientModeEnabled: Boolean = false
    private var onSuccess: () -> Unit = {}
    private var onCancelled: () -> Unit = {}

    fun setCentralClientModeEnabled(isCentralClientModeEnabled: Boolean) {
        this.isCentralClientModeEnabled = isCentralClientModeEnabled
    }

    fun withSuccessCallback(onSuccess: () -> Unit) = apply {
        this.onSuccess = onSuccess
    }

    fun withCancelledCallback(onCancelled: () -> Unit) = apply {
        this.onCancelled = onCancelled
    }

    private var _binding: FragmentBlePermissionsBinding? = null
    private val binding get() = _binding!!

    private val blePermissions: List<String>
        get() {
            val permissions: MutableList<String> = mutableListOf()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 && isCentralClientModeEnabled) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            return permissions
        }

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                log("Ble permissions ${it.key} = ${it.value}")

                // Open settings if user denied any required permission
                if (!it.value && !shouldShowRequestPermissionRationale(it.key)) {
                    openSettings()
                    return@registerForActivityResult
                }
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateUI()
            }
        }

    private val enableLocationProviderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateUI()
            }
        }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (!TextUtils.isEmpty(action) && action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_ON -> updateUI()
                }
            }
        }
    }

    private val locationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (!TextUtils.isEmpty(action) && action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                updateUI()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlePermissionsBinding.inflate(layoutInflater)
        binding.enableBluetoothPermissions.setOnClickListener {
            if (!isAllPermissionsGranted()) shouldRequestPermission()
        }
        binding.enableBluetoothBtn.apply {
            visibility =
                if (isAllPermissionsGranted() && !isBluetoothAdapterEnabled()) View.VISIBLE else View.GONE
            setOnClickListener {
                enableBluetoothAdapter()
            }
        }
        binding.enableLocationProvider.setOnClickListener { enableLocationProvider() }
        binding.nextBtn.setOnClickListener { this.dismiss() }
        binding.skipBtn.setOnClickListener { this.dismiss() }
        return binding.root
    }

    private fun updateUI() {
        binding.enableBluetoothBtn.visibility =
            if (isAllPermissionsGranted() && !isBluetoothAdapterEnabled()) View.VISIBLE
            else View.GONE
        binding.enableBluetoothPermissions.visibility =
            if (isAllPermissionsGranted()) View.GONE
            else View.VISIBLE
        binding.enableLocationProvider.visibility =
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
                isCentralClientModeEnabled &&
                !isLocationProviderEnabled()
            )
                View.VISIBLE
            else View.GONE

        if (checkAllRequirements()) dismiss()
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(
            bluetoothReceiver, IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED
            )
        )
        context?.registerReceiver(
            locationReceiver, IntentFilter(
                LocationManager.PROVIDERS_CHANGED_ACTION
            )
        )
        updateUI()
    }

    override fun onPause() {
        context?.unregisterReceiver(
            bluetoothReceiver
        )
        context?.unregisterReceiver(
            locationReceiver
        )
        super.onPause()
    }

    private fun checkAllRequirements(): Boolean {
        return if (isBluetoothAdapterEnabled() && isAllPermissionsGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) true
            else if (isCentralClientModeEnabled) isLocationProviderEnabled()
            else true
        } else false
    }

    private fun isAllPermissionsGranted(): Boolean {
        return blePermissions.none { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldRequestPermission() {
        val permissionsNeeded = blePermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            permissionsLauncher.launch(
                permissionsNeeded.toTypedArray()
            )
        }
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", requireContext().packageName, null)
        startActivity(intent)
    }

    private fun enableBluetoothAdapter() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    private fun isBluetoothAdapterEnabled(): Boolean {
        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter.isEnabled
    }

    private fun enableLocationProvider() {
        val enableLcPrIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        enableLocationProviderLauncher.launch(enableLcPrIntent)
    }

    private fun isLocationProviderEnabled(): Boolean {
        val locationManager =
            context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (checkAllRequirements()) onSuccess.invoke()
        else onCancelled.invoke()
        _binding = null
    }

    companion object {

        private const val TAG = "BlePermissionsFragment"

        fun showDialog(
            manager: FragmentManager,
            centralModeEnabled: Boolean = false,
            onSuccess: () -> Unit,
            onCancelled: () -> Unit
        ) {
            if (manager.findFragmentByTag(TAG) == null) {
                BlePermissionsFragment().apply {
                    isCancelable = false
                    show(manager, TAG)
                    setCentralClientModeEnabled(centralModeEnabled)
                    withSuccessCallback(onSuccess)
                    withCancelledCallback(onCancelled)
                }
            }
        }
    }
}