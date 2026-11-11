package com.example.service

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RideLocationServiceHelper {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _currentAddress = MutableStateFlow("Tracking location...")
    val currentAddress: StateFlow<String> = _currentAddress.asStateFlow()

    private val _currentSpeedMph = MutableStateFlow(0f)
    val currentSpeedMph: StateFlow<Float> = _currentSpeedMph.asStateFlow()

    fun startTracking(
        context: Context,
        driverName: String = "Alex Rivera",
        vehicleInfo: String = "Toyota Camry"
    ) {
        val intent = Intent(context, RideLocationForegroundService::class.java).apply {
            action = RideLocationForegroundService.ACTION_START_TRACKING
            putExtra(RideLocationForegroundService.EXTRA_DRIVER_NAME, driverName)
            putExtra(RideLocationForegroundService.EXTRA_VEHICLE_INFO, vehicleInfo)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, RideLocationForegroundService::class.java).apply {
            action = RideLocationForegroundService.ACTION_STOP_TRACKING
        }
        context.startService(intent)
    }

    internal fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    internal fun updateLocation(location: Location, address: String) {
        _currentLocation.value = location
        _currentAddress.value = address
        // Convert m/s to mph: 1 m/s = 2.23694 mph
        _currentSpeedMph.value = location.speed * 2.23694f
    }
}
