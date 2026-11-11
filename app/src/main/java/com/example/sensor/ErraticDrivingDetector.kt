package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.data.model.DrivingStatus
import kotlin.math.abs
import kotlin.math.sqrt

class ErraticDrivingDetector(
    private val context: Context,
    private val onTelemetryUpdate: (lateralG: Float, longitudinalG: Float, totalG: Float, score: Int, status: DrivingStatus, alert: String?) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Sensitivity multipliers
    var sensitivityMultiplier: Float = 1.0f // 0.8f = Low, 1.0f = Medium, 1.3f = High

    // Gravity filter
    private val gravity = FloatArray(3) { 0f }
    private val linearAcceleration = FloatArray(3) { 0f }
    private val alpha = 0.8f

    // Rolling erratic events
    private var erraticScore = 0
    private var lastDecayTime = System.currentTimeMillis()
    private var lastEventTime = 0L

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Low-pass filter to isolate gravity
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        // High-pass filter to isolate vehicle movement & vibration
        linearAcceleration[0] = event.values[0] - gravity[0] // Lateral (Left-Right)
        linearAcceleration[1] = event.values[1] - gravity[1] // Longitudinal (Braking-Accel)
        linearAcceleration[2] = event.values[2] - gravity[2] // Vertical (Bumps)

        val ax = linearAcceleration[0]
        val ay = linearAcceleration[1]
        val az = linearAcceleration[2]

        // Total linear acceleration magnitude in m/s^2
        val netMagnitude = sqrt(ax * ax + ay * ay + az * az)

        // Convert to G-force (1G ≈ 9.80665 m/s^2)
        val latG = (ax / 9.81f) * sensitivityMultiplier
        val longG = (ay / 9.81f) * sensitivityMultiplier
        val totalG = 1.0f + (netMagnitude / 9.81f)

        // Gradually decay erratic score over time (calms down every 2 seconds of smooth driving)
        val now = System.currentTimeMillis()
        if (now - lastDecayTime > 1500) {
            if (erraticScore > 0) erraticScore = (erraticScore - 5).coerceAtLeast(0)
            lastDecayTime = now
        }

        var detectedStatus = DrivingStatus.SMOOTH
        var alertMessage: String? = null

        // Threshold detection (scaled with sensitivity)
        val swerveThreshold = 0.38f / sensitivityMultiplier
        val brakeThreshold = 0.42f / sensitivityMultiplier

        if (abs(longG) > brakeThreshold && now - lastEventTime > 1800) {
            detectedStatus = DrivingStatus.HARSH_BRAKING
            erraticScore = (erraticScore + 25).coerceAtMost(100)
            alertMessage = "Sudden Harsh Braking Detected! (${String.format("%.2f", abs(longG))} G)"
            lastEventTime = now
        } else if (abs(latG) > swerveThreshold && now - lastEventTime > 1800) {
            detectedStatus = DrivingStatus.SHARP_SWERVE
            erraticScore = (erraticScore + 25).coerceAtMost(100)
            alertMessage = "Sharp Swerve / Sudden Turn Detected! (${String.format("%.2f", abs(latG))} G)"
            lastEventTime = now
        } else if (erraticScore >= 60) {
            detectedStatus = DrivingStatus.ERRATIC_HAZARD
            alertMessage = "High Hazard: Repeated Erratic Driver Maneuvers!"
        } else if (erraticScore >= 30 || abs(latG) > 0.22f) {
            detectedStatus = DrivingStatus.MODERATE_SWAY
        }

        onTelemetryUpdate(latG, longG, totalG, erraticScore, detectedStatus, alertMessage)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Allows simulated motion injection for testing in emulator or demonstration
     */
    fun simulateErraticEvent(type: DrivingStatus) {
        val now = System.currentTimeMillis()
        when (type) {
            DrivingStatus.HARSH_BRAKING -> {
                erraticScore = (erraticScore + 30).coerceAtMost(100)
                onTelemetryUpdate(
                    0.08f,
                    -0.65f,
                    1.65f,
                    erraticScore,
                    DrivingStatus.HARSH_BRAKING,
                    "Simulated: Harsh Braking (-0.65 G)"
                )
            }
            DrivingStatus.SHARP_SWERVE -> {
                erraticScore = (erraticScore + 30).coerceAtMost(100)
                onTelemetryUpdate(
                    0.58f,
                    0.15f,
                    1.58f,
                    erraticScore,
                    DrivingStatus.SHARP_SWERVE,
                    "Simulated: Aggressive Swerve (0.58 G)"
                )
            }
            DrivingStatus.ERRATIC_HAZARD -> {
                erraticScore = 88
                onTelemetryUpdate(
                    0.72f,
                    -0.85f,
                    2.1f,
                    erraticScore,
                    DrivingStatus.ERRATIC_HAZARD,
                    "Simulated: Dangerous Erratic Driving! Hazard level critical."
                )
            }
            DrivingStatus.MODERATE_SWAY -> {
                erraticScore = (erraticScore + 15).coerceAtMost(100)
                onTelemetryUpdate(
                    0.30f,
                    0.10f,
                    1.2f,
                    erraticScore,
                    DrivingStatus.MODERATE_SWAY,
                    "Simulated: Moderate Sway"
                )
            }
            DrivingStatus.SMOOTH -> {
                erraticScore = 0
                onTelemetryUpdate(0.02f, 0.01f, 1.0f, 0, DrivingStatus.SMOOTH, null)
            }
        }
    }
}
