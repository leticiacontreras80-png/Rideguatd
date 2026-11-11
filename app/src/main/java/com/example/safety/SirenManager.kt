package com.example.safety

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SirenManager(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var isSirenPlaying = false
    private var sirenJob: Job? = null
    private var strobeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val cameraId: String? by lazy {
        try {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun startSiren(enableTorchStrobe: Boolean = true) {
        if (isSirenPlaying) return
        isSirenPlaying = true

        // Start synthesized police siren tone
        sirenJob = scope.launch {
            try {
                val sampleRate = 44100
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
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
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val buffer = ShortArray(1024)
                var phase = 0.0
                var time = 0.0

                while (isActive && isSirenPlaying) {
                    for (i in buffer.indices) {
                        // Modulate between 700 Hz and 1400 Hz over a 0.8s cycle
                        val cycleTime = (time % 0.8) / 0.8
                        val currentFreq = 700.0 + 700.0 * (0.5 * (1.0 + sin(2.0 * PI * cycleTime)))

                        buffer[i] = (sin(phase) * 30000).toInt().toShort()
                        val phaseIncrement = 2.0 * PI * currentFreq / sampleRate
                        phase += phaseIncrement
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        time += 1.0 / sampleRate
                    }
                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                // Graceful fallback
            }
        }

        // Emergency vibration pattern
        triggerEmergencyVibration()

        // Strobe flashlight if available
        if (enableTorchStrobe) {
            startTorchStrobe()
        }
    }

    fun stopSiren() {
        isSirenPlaying = false
        sirenJob?.cancel()
        sirenJob = null
        strobeJob?.cancel()
        strobeJob = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null

        setTorch(false)
        vibrator?.cancel()
    }

    private fun startTorchStrobe() {
        strobeJob = scope.launch {
            var torchState = false
            while (isActive && isSirenPlaying) {
                torchState = !torchState
                setTorch(torchState)
                delay(120) // Fast pulsing strobe
            }
            setTorch(false)
        }
    }

    private fun setTorch(enabled: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager?.setTorchMode(id, enabled)
        } catch (e: Exception) {
            // Torch may not be available or permission denied
        }
    }

    fun triggerEmergencyVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 300, 100, 300, 100, 600)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), 0)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    val isPlaying: Boolean
        get() = isSirenPlaying
}
