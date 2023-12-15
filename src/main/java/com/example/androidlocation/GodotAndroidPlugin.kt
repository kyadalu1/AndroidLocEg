package com.example.androidlocation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class GodotAndroidPlugin(godot: Godot) : GodotPlugin(godot),LocationCallback {

    private val locationRequestCode = 123

    private var locationService: LocationService? = null
    private var isServiceBound = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            locationService?.setLocationCallback(this@GodotAndroidPlugin)
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isServiceBound = false
        }
    }

    override fun getPluginName() = "AndroidLocationPlugin"


    override fun getPluginSignals(): MutableSet<SignalInfo> {
        val signals: MutableSet<SignalInfo> = mutableSetOf();
        signals.add(SignalInfo("locationSignal", String::class.java))
        return signals
    }

    @UsedByGodot
    private fun getLocation() {
        runOnUiThread {
            Toast.makeText(activity?.applicationContext,"1",Toast.LENGTH_SHORT).show()
            if (activity?.hasLocationPermission() == true) {
                Toast.makeText(activity?.applicationContext,"2",Toast.LENGTH_SHORT).show()
                startLocationService()
            } else {
                Toast.makeText(activity?.applicationContext,"3",Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        }
    }

    @UsedByGodot
    private fun stopLocation() {
        Intent(activity?.applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            activity?.startService(this)
        }

    }

    private fun requestPermissions() {
        Toast.makeText(activity?.applicationContext,"4",Toast.LENGTH_SHORT).show()
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        activity?.let { ActivityCompat.requestPermissions(it, permissions, locationRequestCode) }
    }

    override fun onMainRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ) {
        super.onMainRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode) {
            Toast.makeText(activity?.applicationContext,"5",Toast.LENGTH_SHORT).show()
            val allLocationPermissionsGranted =
                grantResults?.all { it == PackageManager.PERMISSION_GRANTED }
            if (allLocationPermissionsGranted == true) {
                Toast.makeText(activity?.applicationContext,"6",Toast.LENGTH_SHORT).show()
                startLocationService()
            } else {
                Toast.makeText(activity, "Location permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startLocationService() {
        Toast.makeText(activity?.applicationContext,"7",Toast.LENGTH_SHORT).show()
        val locationManager =
            activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        if (isEnabled) {
            val serviceIntent = Intent(activity?.applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            activity?.startService(serviceIntent)
            activity?.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        } else {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity?.startActivity(intent)
            Toast.makeText(activity, "Enable location", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onLocationUpdated(latitude: Double, longitude: Double) {
        runOnUiThread {
            emitSignal("locationSignal", "$latitude $longitude")
        }

    }


}
