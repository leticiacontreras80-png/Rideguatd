package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Contact
import com.example.ui.RideGuardViewModel
import com.example.ui.components.PanicSosButton
import com.example.ui.components.QuickActionToolbar
import com.example.ui.components.RideInfoCard
import com.example.ui.components.SafetyTimerCard
import com.example.ui.components.TelemetryCard
import com.example.ui.dialogs.EditRideDialog
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonRedBright
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceDarkBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RideMonitorScreen(
    viewModel: RideGuardViewModel,
    onNavigateToContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rideInfo by viewModel.rideInfo.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val isStealthSos by viewModel.isStealthSos.collectAsStateWithLifecycle()
    val isSirenActive by viewModel.isSirenActive.collectAsStateWithLifecycle()
    val isRecordingAudio by viewModel.audioRecorder.isRecording.collectAsStateWithLifecycle()
    val audioDurationSeconds by viewModel.audioRecorder.recordingDurationSeconds.collectAsStateWithLifecycle()
    val isSafetyTimerActive by viewModel.isSafetyTimerActive.collectAsStateWithLifecycle()
    val safetyTimerRemainingSeconds by viewModel.safetyTimerRemainingSeconds.collectAsStateWithLifecycle()
    val safetyTimerTotalMinutes by viewModel.safetyTimerTotalMinutes.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val isBackgroundTrackingRunning by viewModel.isBackgroundTrackingRunning.collectAsStateWithLifecycle()

    var showEditRideDialog by remember { mutableStateOf(false) }

    if (showEditRideDialog) {
        EditRideDialog(
            currentRide = rideInfo,
            onDismiss = { showEditRideDialog = false },
            onSave = { service, driver, vehicle, plate, pickup, dest ->
                viewModel.updateRideInfo(service, driver, vehicle, plate, pickup, dest)
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ride_monitor_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                // Background Hero Image with new brand logo & tagline
                Image(
                    painter = painterResource(id = com.example.R.drawable.rideguard_brand_banner_1794425485505),
                    contentDescription = "RideGuard: A Better, Safer Ride",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color(0xFF0C101A).copy(alpha = 0.90f)
                                )
                            )
                        )
                )
                // Overlay text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFDA4AF).copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDA4AF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SafeGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RIDEGUARD ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A Better, Safer Ride",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Continuous GPS telemetry, panic SOS & erratic driver protection.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Active Ride Info Card
        item {
            RideInfoCard(
                rideInfo = rideInfo,
                isBackgroundTrackingRunning = isBackgroundTrackingRunning,
                onToggleBackgroundTracking = { viewModel.toggleBackgroundRideTracking() },
                onEditRide = { showEditRideDialog = true },
                onShareRide = {
                    val intent = viewModel.shareLiveRideIntent()
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Live Ride"))
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Central Panic SOS Button
        item {
            PanicSosButton(
                onTriggerSos = { viewModel.requestSos() },
                isStealthMode = isStealthSos,
                onToggleStealth = { viewModel.toggleStealthSos() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        // Quick Action Emergency Toolbar
        item {
            QuickActionToolbar(
                isSirenActive = isSirenActive,
                onToggleSiren = { viewModel.toggleSiren() },
                isRecordingAudio = isRecordingAudio,
                audioDurationSeconds = audioDurationSeconds,
                onToggleAudioRecord = { viewModel.toggleAudioRecording() },
                onTriggerFakeCall = { viewModel.triggerFakeCall(delaySeconds = 0) },
                onShareLocation = {
                    val intent = viewModel.shareLiveRideIntent()
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Live Ride Alert"))
                },
                onCall911 = {
                    val intent = viewModel.dialEmergencyServicesIntent()
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Live Telemetry & Motion Card
        item {
            TelemetryCard(
                telemetry = telemetry,
                onSimulateEvent = { status -> viewModel.simulateErraticEvent(status) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Safety Check-In Timer Card
        item {
            SafetyTimerCard(
                isTimerActive = isSafetyTimerActive,
                remainingSeconds = safetyTimerRemainingSeconds,
                totalMinutes = safetyTimerTotalMinutes,
                onStartTimer = { mins -> viewModel.startSafetyTimer(mins) },
                onCancelTimer = { viewModel.cancelSafetyTimer() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Trusted Contacts Summary Strip
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SurfaceDarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Contacts",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRUSTED SAFETY CIRCLE (${contacts.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "Manage",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("manage_contacts_link")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (contacts.isEmpty()) {
                        Text(
                            text = "No trusted contacts added yet. Tap Manage to add contacts.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            contacts.take(3).forEach { contact ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = contact.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = contact.relationship,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
