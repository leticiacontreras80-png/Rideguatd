package com.example.safety

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioRecordingItem(
    val file: File,
    val durationSeconds: Int,
    val timestamp: Long,
    val title: String
)

class AudioEvidenceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _audioWaveLevels = MutableStateFlow(listOf(0.2f, 0.4f, 0.3f, 0.5f, 0.2f))
    val audioWaveLevels: StateFlow<List<Float>> = _audioWaveLevels.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        return try {
            val audioDir = File(context.cacheDir, "ride_evidence").apply { mkdirs() }
            val file = File(audioDir, "evidence_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            recordingStartTime = System.currentTimeMillis()

            tickerJob = scope.launch {
                while (isActive && _isRecording.value) {
                    val elapsed = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                    _recordingDurationSeconds.value = elapsed

                    val amp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) { 0 }
                    val normalized = (amp / 32767f).coerceIn(0.1f, 1.0f)
                    val currentList = _audioWaveLevels.value.toMutableList()
                    currentList.add(normalized)
                    if (currentList.size > 12) currentList.removeAt(0)
                    _audioWaveLevels.value = currentList

                    delay(200)
                }
            }
            true
        } catch (e: Exception) {
            _isRecording.value = false
            false
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        tickerJob?.cancel()
        tickerJob = null
        _isRecording.value = false

        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            currentOutputFile
        } catch (e: Exception) {
            mediaRecorder = null
            null
        }
    }

    fun getRecordings(): List<AudioRecordingItem> {
        val audioDir = File(context.cacheDir, "ride_evidence")
        if (!audioDir.exists()) return emptyList()
        return audioDir.listFiles()
            ?.filter { it.extension == "m4a" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                AudioRecordingItem(
                    file = file,
                    durationSeconds = (file.length() / 16000).toInt().coerceAtLeast(1),
                    timestamp = file.lastModified(),
                    title = "Ride Evidence (${file.nameWithoutExtension.removePrefix("evidence_")})"
                )
            } ?: emptyList()
    }

    fun playRecording(file: File, onComplete: () -> Unit) {
        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlayingAudio.value = false
                    onComplete()
                }
                start()
            }
            _isPlayingAudio.value = true
        } catch (e: Exception) {
            _isPlayingAudio.value = false
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        _isPlayingAudio.value = false
    }
}
