package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class LocationTracker(
    private val context: Context,
    private val onLocationUpdated: (lat: Double, lng: Double, speedMph: Float, address: String) -> Unit
) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastKnownAddress = "Locating your rideshare..."
    private var lastLat: Double = 37.7749
    private var lastLng: Double = -122.4194

    @SuppressLint("MissingPermission")
    fun startTracking() {
        try {
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (isGpsEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    3f,
                    this
                )
            }
            if (isNetworkEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    this
                )
            }

            // Get last known location for instant display
            val lastLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            lastLoc?.let { onLocationChanged(it) }
        } catch (e: SecurityException) {
            // Permission not granted yet, handled gracefully
        }
    }

    fun stopTracking() {
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onLocationChanged(location: Location) {
        lastLat = location.latitude
        lastLng = location.longitude
        // Convert speed (m/s) to mph: 1 m/s = 2.23694 mph
        val speedMph = location.speed * 2.23694f

        scope.launch {
            resolveAddress(location.latitude, location.longitude) { address ->
                lastKnownAddress = address
                onLocationUpdated(location.latitude, location.longitude, speedMph, address)
            }
        }
    }

    private fun resolveAddress(lat: Double, lng: Double, callback: (String) -> Unit) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    val addr = addresses.firstOrNull()?.getAddressLine(0)
                        ?: "${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                    callback(addr)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val addr = addresses?.firstOrNull()?.getAddressLine(0)
                    ?: "${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                callback(addr)
            }
        } catch (e: Exception) {
            callback("${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}")
        }
    }

    fun simulateSpeed(mph: Float) {
        onLocationUpdated(lastLat, lastLng, mph, lastKnownAddress)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
