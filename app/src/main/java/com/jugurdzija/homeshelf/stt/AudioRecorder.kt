package com.jugurdzija.homeshelf.stt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val SAMPLE_RATE = 16000

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val chunks = mutableListOf<FloatArray>()

    fun start() {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        ) { "RECORD_AUDIO permission not granted" }
        check(recordingJob == null) { "AudioRecorder is already recording" }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord failed to initialize" }

        synchronized(chunks) { chunks.clear() }
        audioRecord = record
        record.startRecording()

        recordingJob = scope.launch {
            val buffer = ShortArray(minBufferSize)
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val floatChunk = FloatArray(read) { buffer[it] / 32768.0f }
                    synchronized(chunks) { chunks.add(floatChunk) }
                }
            }
        }
    }

    suspend fun stop(): FloatArray {
        audioRecord?.stop()
        recordingJob?.cancelAndJoin()
        recordingJob = null
        audioRecord?.release()
        audioRecord = null

        val recordedChunks = synchronized(chunks) { chunks.toList().also { chunks.clear() } }
        val result = FloatArray(recordedChunks.sumOf { it.size })
        var offset = 0
        for (chunk in recordedChunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }
}
