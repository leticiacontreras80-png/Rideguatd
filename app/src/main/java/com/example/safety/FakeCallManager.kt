package com.example.safety

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

data class FakeCaller(
    val name: String,
    val number: String,
    val subtitle: String
)

class FakeCallManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var ringtoneJob: Job? = null
    private var inCallSpeechJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isCallIncoming = MutableStateFlow(false)
    val isCallIncoming: StateFlow<Boolean> = _isCallIncoming.asStateFlow()

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _callSeconds = MutableStateFlow(0)
    val callSeconds: StateFlow<Int> = _callSeconds.asStateFlow()

    private val _currentCaller = MutableStateFlow(
        FakeCaller("Mom", "+1 (555) 234-5678", "Incoming Call...")
    )
    val currentCaller: StateFlow<FakeCaller> = _currentCaller.asStateFlow()

    fun triggerFakeCall(caller: FakeCaller = _currentCaller.value, delaySeconds: Int = 0) {
        _currentCaller.value = caller
        scope.launch {
            if (delaySeconds > 0) {
                delay(delaySeconds * 1000L)
            }
            startRinging()
        }
    }

    private fun startRinging() {
        _isCallIncoming.value = true
        _isCallActive.value = false
        _callSeconds.value = 0

        // Ringtone vibration
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 1000, 1000, 1000, 1000)
                val amplitudes = intArrayOf(0, 200, 0, 200, 0)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 1000, 1000), 0)
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Synthesize telephone ring tone
        ringtoneJob = scope.launch {
            try {
                val sampleRate = 44100
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(2048)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                val buffer = ShortArray(1024)
                var phase1 = 0.0
                var phase2 = 0.0

                while (isActive && _isCallIncoming.value) {
                    for (sec in 0 until 2) {
                        for (i in 0 until (sampleRate / 1024)) {
                            if (!isActive || !_isCallIncoming.value) break
                            for (j in buffer.indices) {
                                // Standard US ringtone: 440 Hz + 480 Hz
                                val sample = (sin(phase1) * 12000 + sin(phase2) * 12000).toInt().toShort()
                                buffer[j] = sample
                                phase1 += 2.0 * PI * 440.0 / sampleRate
                                phase2 += 2.0 * PI * 480.0 / sampleRate
                            }
                            track.write(buffer, 0, buffer.size)
                        }
                    }
                    delay(3000) // 3-second pause between rings
                }
                track.stop()
                track.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun answerCall() {
        vibrator?.cancel()
        ringtoneJob?.cancel()
        ringtoneJob = null
        _isCallIncoming.value = false
        _isCallActive.value = true
        _callSeconds.value = 0

        scope.launch {
            while (isActive && _isCallActive.value) {
                delay(1000)
                _callSeconds.value += 1
            }
        }
    }

    fun endCall() {
        vibrator?.cancel()
        ringtoneJob?.cancel()
        ringtoneJob = null
        inCallSpeechJob?.cancel()
        inCallSpeechJob = null
        _isCallIncoming.value = false
        _isCallActive.value = false
        _callSeconds.value = 0
    }
}
