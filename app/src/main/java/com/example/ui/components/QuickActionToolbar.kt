package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun QuickActionToolbar(
    isSirenActive: Boolean,
    onToggleSiren: () -> Unit,
    isRecordingAudio: Boolean,
    audioDurationSeconds: Int,
    onToggleAudioRecord: () -> Unit,
    onTriggerFakeCall: () -> Unit,
    onShareLocation: () -> Unit,
    onCall911: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_action_toolbar"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SurfaceDarkBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "EMERGENCY TOOLKIT ACTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Emergency Siren
                ActionButtonItem(
                    icon = Icons.Default.Campaign,
                    label = if (isSirenActive) "Stop Siren" else "Siren Alarm",
                    sublabel = if (isSirenActive) "ACTIVE" else "Loud tone",
                    isActive = isSirenActive,
                    activeColor = CrimsonRedBright,
                    defaultColor = NeonAmber,
                    testTag = "toolbar_siren_button",
                    onClick = onToggleSiren,
                    modifier = Modifier.weight(1f)
                )

                // 2. Fake Incoming Call
                ActionButtonItem(
                    icon = Icons.Default.PhoneInTalk,
                    label = "Fake Call",
                    sublabel = "Escape call",
                    isActive = false,
                    activeColor = NeonCyan,
                    defaultColor = NeonCyan,
                    testTag = "toolbar_fake_call_button",
                    onClick = onTriggerFakeCall,
                    modifier = Modifier.weight(1f)
                )

                // 3. Audio Evidence
                ActionButtonItem(
                    icon = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                    label = if (isRecordingAudio) "Stop Rec" else "Record Audio",
                    sublabel = if (isRecordingAudio) "${audioDurationSeconds}s" else "Discreet",
                    isActive = isRecordingAudio,
                    activeColor = CrimsonRedBright,
                    defaultColor = SafeGreen,
                    testTag = "toolbar_record_audio_button",
                    onClick = onToggleAudioRecord,
                    modifier = Modifier.weight(1f)
                )

                // 4. Share Live Location
                ActionButtonItem(
                    icon = Icons.Default.ShareLocation,
                    label = "Live Share",
                    sublabel = "GPS & Plate",
                    isActive = false,
                    activeColor = NeonCyan,
                    defaultColor = Color(0xFF60A5FA),
                    testTag = "toolbar_share_live_button",
                    onClick = onShareLocation,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonItem(
    icon: ImageVector,
    label: String,
    sublabel: String,
    isActive: Boolean,
    activeColor: Color,
    defaultColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentColor = if (isActive) activeColor else defaultColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(currentColor.copy(alpha = if (isActive) 0.35f else 0.12f))
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = currentColor.copy(alpha = if (isActive) 1f else 0.4f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = currentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) activeColor else TextPrimary,
            maxLines = 1
        )
        Text(
            text = sublabel,
            fontSize = 9.sp,
            color = if (isActive) activeColor else TextMuted,
            maxLines = 1
        )
    }
}
