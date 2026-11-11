package com.example.data.model

enum class DrivingStatus {
    SMOOTH,
    MODERATE_SWAY,
    HARSH_BRAKING,
    SHARP_SWERVE,
    ERRATIC_HAZARD
}

data class TelemetryState(
    val speedMph: Float = 0f,
    val speedKmh: Float = 0f,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val address: String = "Fetching GPS location...",
    val lateralGForce: Float = 0f,
    val longitudinalGForce: Float = 0f,
    val totalGForce: Float = 1.0f,
    val erraticDrivingScore: Int = 0, // 0 - 100
    val drivingStatus: DrivingStatus = DrivingStatus.SMOOTH,
    val harshEventsCount: Int = 0,
    val lastEventAlert: String? = null,
    val isGpsActive: Boolean = false,
    val isSensorActive: Boolean = false
)
