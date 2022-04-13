package com.overplay.blackmagicshinedemo.presentation

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.GEOFENCE_EXPIRATION_IN_MILLISECONDS
import com.overplay.blackmagicshinedemo.constants.Constants.Companion.GEOFENCE_RADIUS_IN_METERS
import com.overplay.blackmagicshinedemo.presentation.videoplayer.MediaPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val videoPlayer: MediaPlayer) : ViewModel() {

    fun getMediaPlayer() = videoPlayer

    fun createGeofence(geolocation: Location?): Geofence? = geolocation?.run {
        Geofence.Builder()
            .setRequestId(UUID.randomUUID().toString())
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    fun controlMediaPlayerWithRotationAxis(tx: Float, ty: Float, tz: Float) {
        Log.d("GYRO", "tx: $tx | ty: $ty | tz: $tz")
        //TODO find the proper way to seek through the video based on gyro data
        when {
            tz > 1.5f -> videoPlayer.seekBack()
            tz < -1.5f -> videoPlayer.seekForward()
            tx > 1.5f -> videoPlayer.volumeDown()
            tx < -1.5f -> videoPlayer.volumeUp()
        }
    }

}