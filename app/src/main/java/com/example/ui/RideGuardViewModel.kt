package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RideGuardDatabase
import com.example.data.model.Contact
import com.example.data.model.DrivingStatus
import com.example.data.model.RideInfo
import com.example.data.model.SafetyEvent
import com.example.data.model.SafetyEventType
import com.example.data.model.TelemetryState
import com.example.safety.AudioEvidenceRecorder
import com.example.safety.AudioRecordingItem
import com.example.safety.FakeCallManager
import com.example.safety.FakeCaller
import com.example.safety.SirenManager
import com.example.sensor.ErraticDrivingDetector
import com.example.sensor.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class RideGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RideGuardDatabase.getDatabase(application)
    private val contactDao = database.contactDao()
    private val safetyEventDao = database.safetyEventDao()

    val contacts: StateFlow<List<Contact>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val safetyEvents: StateFlow<List<SafetyEvent>> = safetyEventDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Ride Information
    private val _rideInfo = MutableStateFlow(RideInfo())
    val rideInfo: StateFlow<RideInfo> = _rideInfo.asStateFlow()

    // Live Telemetry & Motion
    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    // SOS State
    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    private val _isSosCountdown = MutableStateFlow(false)
    val isSosCountdown: StateFlow<Boolean> = _isSosCountdown.asStateFlow()

    private val _sosCountdownSeconds = MutableStateFlow(5)
    val sosCountdownSeconds: StateFlow<Int> = _sosCountdownSeconds.asStateFlow()

    private val _isStealthSos = MutableStateFlow(false)
    val isStealthSos: StateFlow<Boolean> = _isStealthSos.asStateFlow()

    // Erratic Driving Warning Dialog
    private val _showErraticPrompt = MutableStateFlow(false)
    val showErraticPrompt: StateFlow<Boolean> = _showErraticPrompt.asStateFlow()

    private val _erraticPromptCountdown = MutableStateFlow(20)
    val erraticPromptCountdown: StateFlow<Int> = _erraticPromptCountdown.asStateFlow()

    private val _erraticPromptMessage = MutableStateFlow("")
    val erraticPromptMessage: StateFlow<String> = _erraticPromptMessage.asStateFlow()

    // Siren & Flashlight
    val sirenManager = SirenManager(application)
    private val _isSirenActive = MutableStateFlow(false)
    val isSirenActive: StateFlow<Boolean> = _isSirenActive.asStateFlow()

    // Audio Evidence Recorder
    val audioRecorder = AudioEvidenceRecorder(application)
    private val _recordedClips = MutableStateFlow<List<AudioRecordingItem>>(emptyList())
    val recordedClips: StateFlow<List<AudioRecordingItem>> = _recordedClips.asStateFlow()

    // Fake Inbound Call
    val fakeCallManager = FakeCallManager(application)

    // Safety Check-In Timer
    private val _isSafetyTimerActive = MutableStateFlow(false)
    val isSafetyTimerActive: StateFlow<Boolean> = _isSafetyTimerActive.asStateFlow()

    private val _safetyTimerTotalMinutes = MutableStateFlow(15)
    val safetyTimerTotalMinutes: StateFlow<Int> = _safetyTimerTotalMinutes.asStateFlow()

    private val _safetyTimerRemainingSeconds = MutableStateFlow(15 * 60)
    val safetyTimerRemainingSeconds: StateFlow<Int> = _safetyTimerRemainingSeconds.asStateFlow()

    // Sensor & Hardware
    private val erraticDetector = ErraticDrivingDetector(application) { latG, longG, totalG, score, status, alert ->
        val current = _telemetry.value
        val newHarshCount = if (status == DrivingStatus.HARSH_BRAKING || status == DrivingStatus.SHARP_SWERVE) {
            current.harshEventsCount + 1
        } else {
            current.harshEventsCount
        }

        _telemetry.value = current.copy(
            lateralGForce = latG,
            longitudinalGForce = longG,
            totalGForce = totalG,
            erraticDrivingScore = score,
            drivingStatus = status,
            harshEventsCount = newHarshCount,
            lastEventAlert = alert ?: current.lastEventAlert,
            isSensorActive = true
        )

        if (alert != null) {
            logSafetyEvent(
                eventType = when (status) {
                    DrivingStatus.HARSH_BRAKING -> SafetyEventType.HARSH_BRAKING
                    DrivingStatus.SHARP_SWERVE -> SafetyEventType.SHARP_SWERVE
                    DrivingStatus.ERRATIC_HAZARD -> SafetyEventType.ERRATIC_DRIVING
                    else -> SafetyEventType.ERRATIC_DRIVING
                },
                title = "Erratic Driving Event: ${status.name}",
                description = alert,
                severityLevel = if (status == DrivingStatus.ERRATIC_HAZARD) 3 else 2
            )

            // Trigger Safety Check-in Prompt if hazard is elevated
            if ((status == DrivingStatus.ERRATIC_HAZARD || score >= 75) && !_showErraticPrompt.value && !_isSosActive.value) {
                triggerErraticHazardPrompt(alert)
            }
        }
    }

    private val locationTracker = LocationTracker(application) { lat, lng, speedMph, addr ->
        _telemetry.value = _telemetry.value.copy(
            latitude = lat,
            longitude = lng,
            speedMph = speedMph,
            speedKmh = speedMph * 1.60934f,
            address = addr,
            isGpsActive = true
        )
    }

    private var countdownJob: Job? = null
    private var safetyTimerJob: Job? = null
    private var erraticPromptJob: Job? = null

    val isBackgroundTrackingRunning: StateFlow<Boolean> = com.example.service.RideLocationServiceHelper.isServiceRunning

    init {
        startMonitoring()
        refreshRecordings()

        // Observe continuous coordinates from Background Location Foreground Service
        viewModelScope.launch {
            com.example.service.RideLocationServiceHelper.currentLocation.collect { loc ->
                if (loc != null) {
                    val addr = com.example.service.RideLocationServiceHelper.currentAddress.value
                    val speed = com.example.service.RideLocationServiceHelper.currentSpeedMph.value
                    _telemetry.value = _telemetry.value.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedMph = speed,
                        speedKmh = speed * 1.60934f,
                        address = addr,
                        isGpsActive = true
                    )
                }
            }
        }
    }

    fun startMonitoring() {
        erraticDetector.start()
        locationTracker.startTracking()
    }

    fun stopMonitoring() {
        erraticDetector.stop()
        locationTracker.stopTracking()
    }

    fun startBackgroundRideTracking() {
        val r = _rideInfo.value
        com.example.service.RideLocationServiceHelper.startTracking(
            getApplication(),
            driverName = r.driverName,
            vehicleInfo = "${r.vehicleModel} (${r.licensePlate})"
        )
    }

    fun stopBackgroundRideTracking() {
        com.example.service.RideLocationServiceHelper.stopTracking(getApplication())
    }

    fun toggleBackgroundRideTracking() {
        if (com.example.service.RideLocationServiceHelper.isServiceRunning.value) {
            stopBackgroundRideTracking()
        } else {
            startBackgroundRideTracking()
        }
    }

    // --- SOS CONTROLS ---

    fun requestSos() {
        sirenManager.triggerHapticFeedback()
        if (_isStealthSos.value) {
            // Stealth mode executes instantly with silent alerts and background recording
            executeSosProtocol()
        } else {
            // Standard panic button: 5 second cancel countdown to avoid accidental presses
            _isSosCountdown.value = true
            _sosCountdownSeconds.value = 5
            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                while (isActive && _sosCountdownSeconds.value > 0) {
                    sirenManager.triggerHapticFeedback()
                    delay(1000)
                    _sosCountdownSeconds.value -= 1
                }
                if (_isSosCountdown.value) {
                    _isSosCountdown.value = false
                    executeSosProtocol()
                }
            }
        }
    }

    fun cancelSosCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _isSosCountdown.value = false
    }

    fun executeSosProtocol() {
        _isSosActive.value = true
        _isSosCountdown.value = false

        logSafetyEvent(
            eventType = SafetyEventType.SOS_TRIGGERED,
            title = "PANIC SOS ACTIVATED",
            description = "Emergency SOS protocol triggered. Coordinates: ${_telemetry.value.latitude}, ${_telemetry.value.longitude}. Address: ${_telemetry.value.address}",
            severityLevel = 3
        )

        if (!_isStealthSos.value) {
            // Sound loud siren and flash strobe
            startSiren()
        } else {
            // Discreet audio recording start
            startAudioRecording()
        }
    }

    fun dismissSos() {
        _isSosActive.value = false
        stopSiren()
        audioRecorder.stopRecording()
        refreshRecordings()
    }

    fun toggleStealthSos() {
        _isStealthSos.value = !_isStealthSos.value
    }

    // --- ERRATIC HAZARD CHECK-IN ---

    private fun triggerErraticHazardPrompt(message: String) {
        _erraticPromptMessage.value = message
        _showErraticPrompt.value = true
        _erraticPromptCountdown.value = 20

        sirenManager.triggerHapticFeedback()

        erraticPromptJob?.cancel()
        erraticPromptJob = viewModelScope.launch {
            while (isActive && _erraticPromptCountdown.value > 0) {
                delay(1000)
                _erraticPromptCountdown.value -= 1
            }
            if (_showErraticPrompt.value) {
                // User did not respond in 20 seconds, escalate to SOS!
                _showErraticPrompt.value = false
                executeSosProtocol()
            }
        }
    }

    fun confirmUserIsSafe() {
        erraticPromptJob?.cancel()
        erraticPromptJob = null
        _showErraticPrompt.value = false
        logSafetyEvent(
            eventType = SafetyEventType.CHECK_IN_OK,
            title = "Safety Check-in Confirmed",
            description = "Rider confirmed they are safe following erratic driving warning.",
            severityLevel = 1
        )
    }

    fun escalateErraticToEmergency() {
        erraticPromptJob?.cancel()
        erraticPromptJob = null
        _showErraticPrompt.value = false
        executeSosProtocol()
    }

    // --- SIREN & FLASH ---

    fun toggleSiren() {
        if (_isSirenActive.value) {
            stopSiren()
        } else {
            startSiren()
        }
    }

    fun startSiren() {
        _isSirenActive.value = true
        sirenManager.startSiren(enableTorchStrobe = true)
    }

    fun stopSiren() {
        _isSirenActive.value = false
        sirenManager.stopSiren()
    }

    // --- AUDIO EVIDENCE ---

    fun toggleAudioRecording() {
        if (audioRecorder.isRecording.value) {
            val file = audioRecorder.stopRecording()
            refreshRecordings()
            if (file != null) {
                logSafetyEvent(
                    eventType = SafetyEventType.AUDIO_RECORDING,
                    title = "Audio Evidence Captured",
                    description = "Ambient ride audio recording saved (${file.name}).",
                    severityLevel = 2
                )
            }
        } else {
            startAudioRecording()
        }
    }

    private fun startAudioRecording() {
        val started = audioRecorder.startRecording()
        if (started) {
            logSafetyEvent(
                eventType = SafetyEventType.AUDIO_RECORDING,
                title = "Audio Recording Started",
                description = "Discreet evidence recording initiated.",
                severityLevel = 1
            )
        }
    }

    fun refreshRecordings() {
        _recordedClips.value = audioRecorder.getRecordings()
    }

    // --- FAKE INBOUND CALL ---

    fun triggerFakeCall(delaySeconds: Int = 0, callerName: String = "Mom", callerSubtitle: String = "Mobile") {
        val caller = FakeCaller(
            name = callerName,
            number = "+1 (555) 019-2834",
            subtitle = callerSubtitle
        )
        fakeCallManager.triggerFakeCall(caller, delaySeconds)
        logSafetyEvent(
            eventType = SafetyEventType.FAKE_CALL_USED,
            title = "Fake Call Initiated",
            description = "Simulated incoming call from $callerName triggered with ${delaySeconds}s delay.",
            severityLevel = 1
        )
    }

    // --- SAFETY TIMER (DEADMAN'S SWITCH) ---

    fun startSafetyTimer(minutes: Int) {
        _safetyTimerTotalMinutes.value = minutes
        _safetyTimerRemainingSeconds.value = minutes * 60
        _isSafetyTimerActive.value = true

        safetyTimerJob?.cancel()
        safetyTimerJob = viewModelScope.launch {
            while (isActive && _safetyTimerRemainingSeconds.value > 0) {
                delay(1000)
                _safetyTimerRemainingSeconds.value -= 1
            }
            if (_isSafetyTimerActive.value) {
                _isSafetyTimerActive.value = false
                logSafetyEvent(
                    eventType = SafetyEventType.CHECK_IN_MISSED,
                    title = "Safety Timer Expired",
                    description = "Ride safety timer reached 0 without rider check-in.",
                    severityLevel = 3
                )
                triggerErraticHazardPrompt("Safety Timer Expired: Expected ride completion exceeded. Are you OK?")
            }
        }
    }

    fun cancelSafetyTimer() {
        safetyTimerJob?.cancel()
        safetyTimerJob = null
        _isSafetyTimerActive.value = false
    }

    // --- LIVE RIDE SHARING & CONTACTS ---

    fun getShareableEmergencyText(): String {
        val t = _telemetry.value
        val r = _rideInfo.value
        val mapsUrl = "https://maps.google.com/?q=${t.latitude},${t.longitude}"
        return "🚨 RIDEGUARD EMERGENCY ALERT!\n" +
                "I am currently in an unsafe rideshare.\n" +
                "🚗 Vehicle: ${r.vehicleModel} (${r.serviceName})\n" +
                "🔢 License Plate: ${r.licensePlate}\n" +
                "👤 Driver: ${r.driverName}\n" +
                "📍 Destination: ${r.destinationAddress}\n" +
                "📍 Current Location: ${t.address}\n" +
                "⚡ Current Speed: ${String.format("%.0f", t.speedMph)} mph\n" +
                "🗺️ Live GPS Map: $mapsUrl"
    }

    fun shareLiveRideIntent(): Intent {
        val text = getShareableEmergencyText()
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "🚨 RideGuard Emergency Location Alert")
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    fun dialEmergencyServicesIntent(): Intent {
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
    }

    fun dialContactIntent(phone: String): Intent {
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    }

    fun smsContactIntent(phone: String): Intent {
        val text = getShareableEmergencyText()
        return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", text)
        }
    }

    // --- CONTACT MANAGEMENT ---

    fun addContact(name: String, phone: String, relationship: String, isEmergency: Boolean = true) {
        viewModelScope.launch {
            contactDao.insertContact(
                Contact(
                    name = name,
                    phone = phone,
                    relationship = relationship,
                    isEmergencyAlertEnabled = isEmergency
                )
            )
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.updateContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.deleteContact(contact)
        }
    }

    // --- RIDE INFO EDITING ---

    fun updateRideInfo(
        service: String,
        driver: String,
        vehicle: String,
        plate: String,
        pickup: String,
        destination: String
    ) {
        _rideInfo.value = _rideInfo.value.copy(
            serviceName = service,
            driverName = driver,
            vehicleModel = vehicle,
            licensePlate = plate,
            pickupAddress = pickup,
            destinationAddress = destination
        )
    }

    // --- SIMULATION TESTING HELPERS ---

    fun simulateErraticEvent(status: DrivingStatus) {
        erraticDetector.simulateErraticEvent(status)
    }

    fun simulateSpeed(mph: Float) {
        locationTracker.simulateSpeed(mph)
    }

    fun setSensitivity(multiplier: Float) {
        erraticDetector.sensitivityMultiplier = multiplier
    }

    private fun logSafetyEvent(
        eventType: SafetyEventType,
        title: String,
        description: String,
        severityLevel: Int
    ) {
        viewModelScope.launch {
            val t = _telemetry.value
            safetyEventDao.insertEvent(
                SafetyEvent(
                    eventType = eventType,
                    title = title,
                    description = description,
                    latitude = t.latitude,
                    longitude = t.longitude,
                    speedMph = t.speedMph,
                    severityLevel = severityLevel
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
        sirenManager.stopSiren()
        audioRecorder.stopRecording()
        audioRecorder.stopPlayback()
        fakeCallManager.endCall()
    }
}
