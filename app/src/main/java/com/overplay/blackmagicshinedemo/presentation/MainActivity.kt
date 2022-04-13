package com.overplay.blackmagicshinedemo.presentation

import android.Manifest.permission.*
import android.R
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.location.Location
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.DELAY_IN_SECONDS
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
        PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geoFencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        checkPermissions()
        initUi()
        initShakeSensitivity()
        initGyroscopeControls()
    }

    private fun initGyroscopeControls() {
        gyroscope = Gyroscope(this) { xAxis, yAxis, zAxis ->
            viewModel.controlMediaPlayerWithRotationAxis(xAxis, yAxis, zAxis)
        }
    }

    private fun checkPermissions() {
        Dexter.withContext(this)
            .withPermissions(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
                ACCESS_BACKGROUND_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        initGeofencing()
                    } else {
                        DialogOnAnyDeniedMultiplePermissionsListener.Builder
                            .withContext(applicationContext)
                            .withTitle(getString(com.overplay.blackmagicshinedemo.R.string.location_services_permission_dialog_title))
                            .withMessage(getString(com.overplay.blackmagicshinedemo.R.string.location_services_permission_dialog_description))
                            .withButtonText(R.string.ok)
                            .withIcon(R.mipmap.sym_def_app_icon)
                            .build()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                ) {
                }
            }).check()
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
    }

    override fun onPause() {
        super.onPause()
        gyroscope.unregister()
        if (VERSION.SDK_INT < VERSION_CODES.N) viewModel.getMediaPlayer().release()
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= VERSION_CODES.N) viewModel.getMediaPlayer().release()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        window.setDecorFitsSystemWindows(false)
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

    @SuppressLint("MissingPermission")
    private fun initGeofencing() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geoFencingClient = LocationServices.getGeofencingClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            var lastLocationGeofencingRequest = GeofencingRequest.Builder().apply {
                setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                addGeofence(viewModel.createGeofence(location))
            }.build()

            geoFencingClient.addGeofences(lastLocationGeofencingRequest, geofencePendingIntent)
                .addOnSuccessListener { viewModel.getMediaPlayer().reset() }
                .addOnFailureListener { e ->
                    Log.e(
                        MainActivity::javaClass.name,
                        "${e.localizedMessage} | ${e.stackTraceToString()}"
                    )
                    Snackbar.make(
                        binding.root,
                        "${e.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
        }

        var locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                var geofenceList = arrayListOf<Geofence>()
                for (location in locationResult.locations) {
                    viewModel.createGeofence(location)?.let { geofenceList.add(it) }
                }

                var currentLocationGeofencingRequest = GeofencingRequest.Builder().apply {
                    setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    addGeofences(geofenceList)
                }.build()

                geoFencingClient.addGeofences(
                    currentLocationGeofencingRequest,
                    geofencePendingIntent
                )
                    .addOnSuccessListener { viewModel.getMediaPlayer().reset() }
                    .addOnFailureListener { e ->
                        Log.e(
                            MainActivity::javaClass.name,
                            "${e.localizedMessage} | ${e.stackTraceToString()}"
                        )
                        Snackbar.make(
                            binding.root,
                            "${e.message}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            LocationRequest.create(),
            locationCallback,
            Looper.getMainLooper()
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
                viewModel.getMediaPlayer().reset()
            }
        }
    }
}