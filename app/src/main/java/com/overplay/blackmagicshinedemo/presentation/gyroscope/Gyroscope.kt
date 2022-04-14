package com.overplay.blackmagicshinedemo.presentation.gyroscope

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager


class Gyroscope internal constructor(
    context: Context,
    listener: (xAxis: Float, yAxis: Float, zAxis: Float) -> Unit
) {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent) {
            if (listener != null) {
                listener(
                    sensorEvent.values[0], sensorEvent.values[1],
                    sensorEvent.values[2]
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, i: Int) {}
    }

    fun register() {
        sensorManager.registerListener(
            sensorEventListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun unregister() {
        sensorManager.unregisterListener(sensorEventListener)
    }
}