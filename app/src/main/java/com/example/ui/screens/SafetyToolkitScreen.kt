package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.RideGuardViewModel
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonRedBright
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceDarkBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SafetyToolkitScreen(
    viewModel: RideGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSirenActive by viewModel.isSirenActive.collectAsStateWithLifecycle()
    val isRecordingAudio by viewModel.audioRecorder.isRecording.collectAsStateWithLifecycle()
    val audioDurationSeconds by viewModel.audioRecorder.recordingDurationSeconds.collectAsStateWithLifecycle()
    val audioWaveLevels by viewModel.audioRecorder.audioWaveLevels.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.audioRecorder.isPlayingAudio.collectAsStateWithLifecycle()
    val recordedClips by viewModel.recordedClips.collectAsStateWithLifecycle()

    var selectedCallerName by remember { mutableStateOf("Mom") }
    var selectedCallerSubtitle by remember { mutableStateOf("Mobile") }
    var selectedDelaySeconds by remember { mutableIntStateOf(0) }
    var sensitivitySlider by remember { mutableFloatStateOf(1.0f) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("safety_toolkit_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Safety Defense Toolkit",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Tactical tools for de-escalation, deterrence, and evidence gathering.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // TOOL 1: FAKE INCOMING CALL STUDIO
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fake_call_studio_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SurfaceDarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = "Fake Call", tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Fake Inbound Call", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Simulate a phone call to exit or defuse tense situations", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("SELECT CALLER PRESET:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Mom", "Family", "Mom"),
                            Triple("Dad (Officer)", "Dad (Officer Mike)", "Dad"),
                            Triple("Dispatcher", "Rideshare Safety Dispatch", "Safety"),
                            Triple("Roommate", "Roommate Jordan", "Jordan")
                        ).forEach { (label, fullTitle, titleShort) ->
                            FilterChip(
                                selected = selectedCallerName == label,
                                onClick = {
                                    selectedCallerName = label
                                    selectedCallerSubtitle = fullTitle
                                },
                                label = { Text(titleShort, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("TRIGGER DELAY:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Now", 10 to "In 10s", 30 to "In 30s").forEach { (delaySec, label) ->
                            FilterChip(
                                selected = selectedDelaySeconds == delaySec,
                                onClick = { selectedDelaySeconds = delaySec },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.triggerFakeCall(
                                delaySeconds = selectedDelaySeconds,
                                callerName = selectedCallerName,
                                callerSubtitle = selectedCallerSubtitle
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trigger_fake_call_button")
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = "Trigger Call", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedDelaySeconds > 0) "Trigger Fake Call in ${selectedDelaySeconds}s" else "Ring Fake Call Immediately",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // TOOL 2: HIGH DECIBEL SIREN & TORCH STROBE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("siren_strobe_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SurfaceDarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CrimsonRedBright.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = "Siren", tint = CrimsonRedBright, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Emergency Siren & Flash Strobe", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Oscillating 1400Hz deterrent siren + camera torch strobe", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.toggleSiren() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSirenActive) CrimsonRedBright else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_siren_button")
                    ) {
                        Icon(
                            imageVector = if (isSirenActive) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = "Siren",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSirenActive) "STOP LOUD SIREN" else "SOUND EMERGENCY SIREN",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // TOOL 3: AMBIENT AUDIO EVIDENCE RECORDER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_evidence_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SurfaceDarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SafeGreen.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Mic", tint = SafeGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Discreet Audio Evidence", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Records cabin audio securely to device storage", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Waveform Equalizer Display if recording
                    if (isRecordingAudio) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F0808),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRedBright.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonRedBright)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "RECORDING: ${audioDurationSeconds}s",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CrimsonRedBright
                                    )
                                }

                                // Waveform bars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    audioWaveLevels.forEach { lvl ->
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height((24.dp * lvl).coerceIn(4.dp, 28.dp))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(CrimsonRedBright)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = { viewModel.toggleAudioRecording() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecordingAudio) CrimsonRed else SafeGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_audio_record_button")
                    ) {
                        Icon(
                            imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = if (isRecordingAudio) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRecordingAudio) "Stop & Save Audio Recording" else "Start Discreet Audio Recording",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isRecordingAudio) Color.White else Color.Black
                        )
                    }

                    // Saved Clips List
                    if (recordedClips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("SAVED AUDIO RECORDINGS (${recordedClips.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            recordedClips.take(3).forEach { clip ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(clip.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(clip.timestamp))
                                            Text("$dateStr • ${clip.durationSeconds}s", fontSize = 10.sp, color = TextMuted)
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    viewModel.audioRecorder.playRecording(clip.file) {}
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = SafeGreen)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TOOL 4: ERRATIC DRIVING SENSOR CALIBRATION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sensor_calibration_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SurfaceDarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Sensitivity", tint = NeonAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Erratic Driving Sensitivity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Calibrate sensor threshold for city vs highway riding", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                sensitivitySlider < 0.9f -> "Highway (Low Sensitivity: 0.8x)"
                                sensitivitySlider > 1.15f -> "Strict / High (1.3x Sensitivity)"
                                else -> "Standard City (1.0x Balanced)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                    }

                    Slider(
                        value = sensitivitySlider,
                        onValueChange = {
                            sensitivitySlider = it
                            viewModel.setSensitivity(it)
                        },
                        valueRange = 0.8f..1.3f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonAmber,
                            activeTrackColor = NeonAmber
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Standard mode triggers alerts if braking exceeds 0.42G or swerving exceeds 0.38G.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
