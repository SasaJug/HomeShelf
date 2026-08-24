package com.jugurdzija.homeshelf.stt

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val WAV_HEADER_BYTES = 44

@RunWith(AndroidJUnit4::class)
class WhisperSpeechToTextEngineTest {

    @Test
    fun transcribe_recognizesKnownPhrase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = WhisperSpeechToTextEngine(context)
        val samples = readWavAsFloatArray(context.assets.open("test-audio/p001.wav").readBytes())

        val result = engine.transcribe(samples)

        assertTrue("Expected a successful transcription, got: $result", result.isSuccess)
        val text = result.getOrThrow().lowercase()
        assertTrue("Expected transcript to mention 'seventeen', got: '$text'", text.contains("seventeen"))
    }

    private fun readWavAsFloatArray(bytes: ByteArray): FloatArray {
        val pcm = bytes.copyOfRange(WAV_HEADER_BYTES, bytes.size)
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        val sampleCount = pcm.size / 2
        val samples = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            samples[i] = buffer.short / 32768.0f
        }
        return samples
    }
}
