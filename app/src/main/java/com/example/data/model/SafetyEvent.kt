package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SafetyEventType {
    SOS_TRIGGERED,
    ERRATIC_DRIVING,
    HARSH_BRAKING,
    SHARP_SWERVE,
    RAPID_ACCEL,
    CHECK_IN_OK,
    CHECK_IN_MISSED,
    AUDIO_RECORDING,
    FAKE_CALL_USED,
    RIDE_STARTED,
    RIDE_ENDED
}

@Entity(tableName = "safety_events")
data class SafetyEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: SafetyEventType,
    val title: String,
    val description: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedMph: Float? = null,
    val severityLevel: Int = 1 // 1: Info, 2: Warning, 3: Danger / Critical
)
