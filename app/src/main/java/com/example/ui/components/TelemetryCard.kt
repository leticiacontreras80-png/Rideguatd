package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrivingStatus
import com.example.data.model.TelemetryState
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonRedBright
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceDarkBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun TelemetryCard(
    telemetry: TelemetryState,
    onSimulateEvent: (DrivingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (telemetry.drivingStatus) {
            DrivingStatus.SMOOTH -> SafeGreen
            DrivingStatus.MODERATE_SWAY -> NeonAmber
            DrivingStatus.HARSH_BRAKING, DrivingStatus.SHARP_SWERVE, DrivingStatus.ERRATIC_HAZARD -> CrimsonRedBright
        },
        label = "statusColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("telemetry_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SurfaceDarkBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Telemetry",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE MOTION TELEMETRY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                }

                // Sensor Status Dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isSensorActive || telemetry.isGpsActive) SafeGreen else NeonAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (telemetry.isSensorActive) "Active" else "Standby",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Metrics Row: Speedometer + G-Force Gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Speed Indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Canvas(modifier = Modifier.size(84.dp)) {
                            // Background Arc
                            drawArc(
                                color = Color.White.copy(alpha = 0.08f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Speed Progress Arc (up to 90 mph)
                            val speedRatio = (telemetry.speedMph / 90f).coerceIn(0f, 1f)
                            drawArc(
                                color = if (telemetry.speedMph > 75) CrimsonRedBright else NeonCyan,
                                startAngle = 135f,
                                sweepAngle = 270f * speedRatio,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.0f", telemetry.speedMph),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "MPH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // G-Force & Inertial Forces
                Column(modifier = Modifier.weight(1f)) {
                    // Lateral G-Force (Swerve / Turn)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lateral Sway (G)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format("%.2f", abs(telemetry.lateralGForce))} G",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(telemetry.lateralGForce) > 0.4f) CrimsonRedBright else TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (abs(telemetry.lateralGForce) / 0.8f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (abs(telemetry.lateralGForce) > 0.4f) CrimsonRedBright else NeonCyan,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Longitudinal G-Force (Braking / Accel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Braking Force (G)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${String.format("%.2f", abs(telemetry.longitudinalGForce))} G",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(telemetry.longitudinalGForce) > 0.45f) CrimsonRedBright else TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (abs(telemetry.longitudinalGForce) / 0.8f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (abs(telemetry.longitudinalGForce) > 0.45f) CrimsonRedBright else NeonAmber,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hazard Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (telemetry.drivingStatus == DrivingStatus.SMOOTH) Icons.Default.Info else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (telemetry.drivingStatus) {
                                DrivingStatus.SMOOTH -> "Normal Safe Driving"
                                DrivingStatus.MODERATE_SWAY -> "Moderate Swaying Detected"
                                DrivingStatus.HARSH_BRAKING -> "Harsh Braking Alert!"
                                DrivingStatus.SHARP_SWERVE -> "Sharp Swerve / Sudden Turn!"
                                DrivingStatus.ERRATIC_HAZARD -> "Hazard: Severe Erratic Driving!"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = statusColor
                        )
                        Text(
                            text = telemetry.lastEventAlert ?: "Accelerometer monitoring vehicle balance & speed continuously.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    // Erratic Score Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "Score: ${telemetry.erraticDrivingScore}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulation / Testing Controls
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SENSOR SIMULATION / TEST (FOR DESK & EMULATOR):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SimulationChip(
                        label = "Smooth",
                        color = SafeGreen,
                        onClick = { onSimulateEvent(DrivingStatus.SMOOTH) },
                        modifier = Modifier.weight(1f)
                    )
                    SimulationChip(
                        label = "Harsh Brake",
                        color = NeonAmber,
                        onClick = { onSimulateEvent(DrivingStatus.HARSH_BRAKING) },
                        modifier = Modifier.weight(1.1f)
                    )
                    SimulationChip(
                        label = "Sharp Swerve",
                        color = CrimsonRedBright,
                        onClick = { onSimulateEvent(DrivingStatus.SHARP_SWERVE) },
                        modifier = Modifier.weight(1.2f)
                    )
                    SimulationChip(
                        label = "Hazard SOS",
                        color = CrimsonRed,
                        onClick = { onSimulateEvent(DrivingStatus.ERRATIC_HAZARD) },
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulationChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .clickable { onClick() }
            .testTag("test_chip_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
