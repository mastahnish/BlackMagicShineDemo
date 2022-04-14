package com.overplay.blackmagicshinedemo.presentation

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Looper
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.overplay.blackmagicshinedemo.R
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.DELAY_IN_SECONDS
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.LOCATION_REQUEST_FASTEST_INTERVAL
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.LOCATION_REQUEST_INTERVAL
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.LOCATION_REQUEST_MAX_WAIT_TIME
import com.overplay.blackmagicshinedemo.databinding.ActivityMainBinding
import com.overplay.blackmagicshinedemo.extensions.countdownListener
import com.overplay.blackmagicshinedemo.extensions.scaleAnimation
import com.overplay.blackmagicshinedemo.presentation.countdown.CountDownAnimation
import com.overplay.blackmagicshinedemo.presentation.gyroscope.Gyroscope
import com.squareup.seismic.ShakeDetector
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class MainActivity : AppCompatActivity(), ShakeDetector.Listener {

    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var gyroscope: Gyroscope

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        if (Util.SDK_INT >= VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private val viewModel: MainViewModel by viewModels()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var geoFencingClient: GeofencingClient? = null
    private lateinit var locationCallback: LocationCallback
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = LOCATION_REQUEST_INTERVAL
        fastestInterval = LOCATION_REQUEST_FASTEST_INTERVAL
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        maxWaitTime = LOCATION_REQUEST_MAX_WAIT_TIME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        checkLocationPermissions()

        initUi()
        initShakeSensitivity()
        initGyroscopeControls()
        initGeofencing()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= VERSION_CODES.N) {
            initPlayerUi()
            initCountDown()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if ((Util.SDK_INT < VERSION_CODES.N || viewModel.getMediaPlayer().getPlayer() == null)) {
            initPlayerUi()
            initCountDown()
        }
        requestLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        gyroscope.unregister()
        if (VERSION.SDK_INT < VERSION_CODES.N) viewModel.getMediaPlayer().release()
        removeLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= VERSION_CODES.N) viewModel.getMediaPlayer().release()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        window?.run{
            WindowCompat.setDecorFitsSystemWindows(this,false)
        }
    }

    private fun initUi() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        hideSystemBars()
        initCountDown()
    }

    private fun initPlayerUi() {
        binding.videoView.player = viewModel.getMediaPlayer().initializePlayer()
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun initCountDown() {
        CountDownAnimation(binding.countdown, DELAY_IN_SECONDS).apply {
            scaleAnimation()
            countdownListener {
                viewModel.getMediaPlayer().play()
                gyroscope.register()
            }
        }.start()
    }

    private fun initShakeSensitivity() {
        ShakeDetector(this).start(getSystemService(SENSOR_SERVICE) as SensorManager)
    }

    override fun hearShake() {
        if (viewModel.getMediaPlayer().isPlaying() == true) viewModel.getMediaPlayer().pause()
    }

    private fun initGyroscopeControls() {
        gyroscope = Gyroscope(this) { xAxis, yAxis, zAxis ->
            viewModel.controlMediaPlayerWithRotationAxis(xAxis, yAxis, zAxis)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initGeofencing() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geoFencingClient = LocationServices.getGeofencingClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                Log.d(MainActivity::javaClass.name, "onLocationResult: $locationResult")
                locationResult ?: return

                var geofenceList = arrayListOf<Geofence>()
                for (location in locationResult.locations) {
                    viewModel.createGeofence(location)?.let { geofenceList.add(it) }
                }

                var currentLocationGeofencingRequest = GeofencingRequest.Builder().apply {
                    setInitialTrigger(0)
                    addGeofences(geofenceList)
                }.build()

                geoFencingClient?.addGeofences(
                    currentLocationGeofencingRequest,
                    geofencePendingIntent
                )
                    ?.addOnSuccessListener {
                        Log.d(MainActivity::javaClass.name, "Geofence for current location created")
                    }
                    ?.addOnFailureListener { e ->
                        handleLocationException(e)
                    }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (checkLocationPermissionsGranted()) {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun removeLocationUpdates() {
        if (checkLocationPermissionsGranted()) {
            fusedLocationClient?.removeLocationUpdates(locationCallback)
        }
    }

    private fun checkLocationPermissions() {
        if (!checkLocationPermissionsGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_COARSE_LOCATION)
            ) {
                showRationaleDialog()
            } else {
                requestLocationPermissions()
            }
        } else {
            checkBackgroundLocationPermission()
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (checkBackgroundLocationPermissionGranted()) requestBackgroundLocationPermission()
    }

    private fun requestBackgroundLocationPermission() {
        if (Util.SDK_INT >= VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    ACCESS_BACKGROUND_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    private fun checkBackgroundLocationPermissionGranted(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            ACCESS_BACKGROUND_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

    private fun checkLocationPermissionsGranted(): Boolean = ActivityCompat.checkSelfPermission(
        this,
        ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        this,
        ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED


    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.location_services_permission_dialog_title))
            .setMessage(getString(R.string.location_services_permission_dialog_description))
            .setPositiveButton(
                getString(R.string.ok)
            ) { _, _ ->
                requestLocationPermissions()
            }
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationUpdates()
                    checkBackgroundLocationPermission()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            ACCESS_FINE_LOCATION
                        ) ||
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            ACCESS_COARSE_LOCATION
                        )
                    ) {
                        startActivity(
                            Intent(
                                ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts(
                                    "com.overplay.blackmagicshinedemo",
                                    this.packageName,
                                    null
                                ),
                            ),
                        )
                    }
                }
                return
            }

            MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkLocationPermissionsGranted()) requestLocationUpdates()
                } else {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.location_permissions_denied),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                return
            }
        }
    }

    private fun handleLocationException(e: Exception) {
        Log.e(
            MainActivity::javaClass.name,
            "${e.localizedMessage} | ${e.stackTraceToString()}"
        )
    }

    inner class GeofenceBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Log.e(MainActivity::javaClass.name, "$errorMessage")
                Snackbar.make(
                    binding.root,
                    "$errorMessage",
                    Snackbar.LENGTH_SHORT
                ).show()
                return
            }

            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Snackbar.make(
                    binding.root,
                    "Geofence exited. Resetting video.",
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.getMediaPlayer().reset()
            }

            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Snackbar.make(binding.root, "Geofence entered.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        private const val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 2
    }
}