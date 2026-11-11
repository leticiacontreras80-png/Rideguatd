package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.RideGuardViewModel
import com.example.ui.dialogs.EmergencySosOverlay
import com.example.ui.dialogs.ErraticDrivingPrompt
import com.example.ui.dialogs.FakeCallOverlay
import com.example.ui.screens.IncidentHistoryScreen
import com.example.ui.screens.RideMonitorScreen
import com.example.ui.screens.SafetyToolkitScreen
import com.example.ui.screens.TrustedContactsScreen
import com.example.ui.theme.CrimsonRedBright
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: RideGuardViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current

                // Request location and audio recording permissions at startup
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (locationGranted) {
                        viewModel.startMonitoring()
                    }
                }

                LaunchedEffect(Unit) {
                    val perms = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                }

                var selectedTab by remember { mutableIntStateOf(0) }

                // State flows
                val isSosActive by viewModel.isSosActive.collectAsStateWithLifecycle()
                val isSosCountdown by viewModel.isSosCountdown.collectAsStateWithLifecycle()
                val sosCountdownSeconds by viewModel.sosCountdownSeconds.collectAsStateWithLifecycle()
                val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
                val rideInfo by viewModel.rideInfo.collectAsStateWithLifecycle()

                val showErraticPrompt by viewModel.showErraticPrompt.collectAsStateWithLifecycle()
                val erraticCountdown by viewModel.erraticPromptCountdown.collectAsStateWithLifecycle()
                val erraticMessage by viewModel.erraticPromptMessage.collectAsStateWithLifecycle()

                val isFakeCallIncoming by viewModel.fakeCallManager.isCallIncoming.collectAsStateWithLifecycle()
                val isFakeCallActive by viewModel.fakeCallManager.isCallActive.collectAsStateWithLifecycle()
                val fakeCallSeconds by viewModel.fakeCallManager.callSeconds.collectAsStateWithLifecycle()
                val currentCaller by viewModel.fakeCallManager.currentCaller.collectAsStateWithLifecycle()

                // Back navigation handling
                BackHandler(enabled = selectedTab != 0 || showErraticPrompt || isSosCountdown || isFakeCallIncoming || isFakeCallActive) {
                    when {
                        isFakeCallIncoming || isFakeCallActive -> viewModel.fakeCallManager.endCall()
                        showErraticPrompt -> viewModel.confirmUserIsSafe()
                        isSosCountdown -> viewModel.cancelSosCountdown()
                        selectedTab != 0 -> selectedTab = 0
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10141D))
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.rideguard_brand_logo_1794425472722),
                                            contentDescription = "RideGuard Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "RideGuard",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (telemetry.isGpsActive) SafeGreen else NeonAmber)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "A better, safer ride",
                                                fontSize = 10.sp,
                                                color = Color(0xFFFDA4AF),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                // 911 Quick Dial Action in Top Bar
                                Surface(
                                    shape = CircleShape,
                                    color = CrimsonRedBright.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRedBright),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .clip(CircleShape)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val intent = viewModel.dialEmergencyServicesIntent()
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(24.dp).testTag("topbar_call_911")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Call 911",
                                                tint = CrimsonRedBright,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "911",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CrimsonRedBright
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkNavyBackground
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            val items = listOf(
                                Triple(0, "Monitor", Icons.Filled.Home to Icons.Outlined.Home),
                                Triple(1, "Toolkit", Icons.Filled.Security to Icons.Outlined.Security),
                                Triple(2, "Contacts", Icons.Filled.Group to Icons.Outlined.Group),
                                Triple(3, "History", Icons.Filled.History to Icons.Outlined.History)
                            )

                            items.forEach { (index, title, icons) ->
                                val selected = selectedTab == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) icons.first else icons.second,
                                            contentDescription = title,
                                            tint = if (selected) NeonCyan else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = title,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) NeonCyan else TextMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = NeonCyan.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_$title")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { tab ->
                            when (tab) {
                                0 -> RideMonitorScreen(
                                    viewModel = viewModel,
                                    onNavigateToContacts = { selectedTab = 2 }
                                )
                                1 -> SafetyToolkitScreen(viewModel = viewModel)
                                2 -> TrustedContactsScreen(viewModel = viewModel)
                                3 -> IncidentHistoryScreen(viewModel = viewModel)
                            }
                        }

                        // HIGH PRIORITY OVERLAYS

                        // 1. Erratic Driving Prompt Dialog
                        ErraticDrivingPrompt(
                            show = showErraticPrompt,
                            message = erraticMessage,
                            countdownSeconds = erraticCountdown,
                            onConfirmSafe = { viewModel.confirmUserIsSafe() },
                            onEscalateToSos = { viewModel.escalateErraticToEmergency() }
                        )

                        // 2. Emergency SOS Countdown and Active Protocol Screen
                        EmergencySosOverlay(
                            isCountdown = isSosCountdown,
                            countdownSeconds = sosCountdownSeconds,
                            isActive = isSosActive,
                            telemetry = telemetry,
                            rideInfo = rideInfo,
                            onCancelCountdown = { viewModel.cancelSosCountdown() },
                            onDismissSos = { viewModel.dismissSos() },
                            onCall911 = {
                                val intent = viewModel.dialEmergencyServicesIntent()
                                context.startActivity(intent)
                            },
                            onShareEmergencyAlert = {
                                val intent = viewModel.shareLiveRideIntent()
                                context.startActivity(Intent.createChooser(intent, "🚨 Emergency SOS Broadcast"))
                            }
                        )

                        // 3. Fake Phone Call Overlay
                        FakeCallOverlay(
                            isIncoming = isFakeCallIncoming,
                            isActive = isFakeCallActive,
                            callSeconds = fakeCallSeconds,
                            caller = currentCaller,
                            onAnswer = { viewModel.fakeCallManager.answerCall() },
                            onEndCall = { viewModel.fakeCallManager.endCall() }
                        )
                    }
                }
            }
        }
    }
}
