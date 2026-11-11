package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RideInfo
import com.example.data.model.TelemetryState
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonRedBright
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SafeGreen

@Composable
fun EmergencySosOverlay(
    isCountdown: Boolean,
    countdownSeconds: Int,
    isActive: Boolean,
    telemetry: TelemetryState,
    rideInfo: RideInfo,
    onCancelCountdown: () -> Unit,
    onDismissSos: () -> Unit,
    onCall911: () -> Unit,
    onShareEmergencyAlert: () -> Unit
) {
    if (!isCountdown && !isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "strobe")
    val strobeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(250),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobeAlpha"
    )

    Dialog(
        onDismissRequest = { /* Prevent accidental dismiss */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isCountdown) Color(0xFF1A0A0A)
                    else Color(0xFF260505)
                )
                .testTag("emergency_sos_dialog")
        ) {
            // Flashing screen strobe edge
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 12.dp,
                            color = CrimsonRedBright.copy(alpha = strobeAlpha)
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CrimsonRedBright.copy(alpha = 0.25f))
                            .border(2.dp, CrimsonRedBright, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = CrimsonRedBright,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isCountdown) "SOS ARMED: CANCEL IN" else "EMERGENCY SOS ACTIVE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isCountdown) "Release or tap Cancel if pressed by accident" else "Live location & vehicle information broadcasting",
                        fontSize = 13.sp,
                        color = Color(0xFFFDA4AF),
                        textAlign = TextAlign.Center
                    )
                }

                // Center Content: Countdown number OR Live Dispatch telemetry
                if (isCountdown) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 20.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(CrimsonRedBright)
                        ) {
                            Text(
                                text = "$countdownSeconds",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap CANCEL below to stop emergency alert",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF180505)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRedBright.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "GPS",
                                    tint = CrimsonRedBright,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE RIDESHARE TELEMETRY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = telemetry.address,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "GPS: ${String.format("%.5f", telemetry.latitude)}, ${String.format("%.5f", telemetry.longitude)} • Speed: ${String.format("%.0f", telemetry.speedMph)} mph",
                                fontSize = 12.sp,
                                color = Color(0xFFD1D5DB)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Vehicle: ${rideInfo.vehicleModel} (${rideInfo.licensePlate}) • Driver: ${rideInfo.driverName}",
                                fontSize = 12.sp,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Action Buttons at Bottom
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isCountdown) {
                        Button(
                            onClick = onCancelCountdown,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("cancel_sos_countdown_button")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = CrimsonRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CANCEL EMERGENCY SOS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = CrimsonRed
                            )
                        }
                    } else {
                        // Call 911 Direct Action
                        Button(
                            onClick = onCall911,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRedBright),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("call_911_sos_button")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "911", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CALL 911 POLICE DISPATCH",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        // Broadcast SMS to Trusted Contacts
                        Button(
                            onClick = onShareEmergencyAlert,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("share_emergency_sms_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SEND EMERGENCY SMS TO CONTACTS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Dismiss / Safe
                        OutlinedButton(
                            onClick = onDismissSos,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dismiss_sos_safe_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Safe", tint = SafeGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I AM SAFE / END EMERGENCY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
