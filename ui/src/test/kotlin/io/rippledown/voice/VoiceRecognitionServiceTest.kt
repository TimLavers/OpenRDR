package io.rippledown.voice

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.awaitility.Awaitility.await
import org.awaitility.core.ThrowingRunnable
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.TargetDataLine

class VoiceRecognitionServiceTest {

    private val captured = mutableListOf<String>()
    private val onFinalResult: (String) -> Unit = { captured.add(it) }
    private val testScope = CoroutineScope(Dispatchers.IO)

    private fun fakeMicrophone(payload: ByteArray = ByteArray(64) { it.toByte() }): TargetDataLine {
        val line = mockk<TargetDataLine>(relaxed = true)
        val delivered = AtomicBoolean(false)
        every { line.read(any(), any(), any()) } answers {
            val buf = arg<ByteArray>(0)
            val off = arg<Int>(1)
            val len = arg<Int>(2)
            if (delivered.compareAndSet(false, true)) {
                val n = minOf(len, payload.size)
                System.arraycopy(payload, 0, buf, off, n)
                n
            } else {
                Thread.sleep(5)
                0
            }
        }
        return line
    }

    private fun waitFor(timeoutSeconds: Long = 2, assertion: () -> Unit) {
        await().atMost(Duration.ofSeconds(timeoutSeconds)).untilAsserted(ThrowingRunnable { assertion() })
    }

    @Test
    fun `initial state is idle with empty partial result`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone() },
            transcribe = { "" }
        )
        service.isListening.value.shouldBeFalse()
        service.partialResult.value shouldBe ""
    }

    @Test
    fun `startListening flips isListening to true and opens the line`() {
        val line = fakeMicrophone()
        val service = VoiceRecognitionService(
            microphoneFactory = { line },
            transcribe = { "ignored" }
        )

        service.startListening(testScope) {}

        waitFor { service.isListening.value.shouldBeTrue() }
        verify { line.open(any<AudioFormat>()) }
        verify { line.start() }
        service.close()
    }

    @Test
    fun `stopListening transcribes captured audio and delivers the result`() {
        val payload = ByteArray(128) { (it and 0xFF).toByte() }
        val transcribed = AtomicBoolean(false)
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone(payload) },
            transcribe = { wav ->
                transcribed.set(true)
                wav.size shouldBe 44 + payload.size
                String(wav.copyOfRange(0, 4)) shouldBe "RIFF"
                "hello world"
            }
        )

        service.startListening(testScope, onFinalResult)
        waitFor { service.isListening.value.shouldBeTrue() }

        service.stopListening()

        waitFor {
            transcribed.get().shouldBeTrue()
            captured shouldContainExactly listOf("hello world")
            service.isListening.value.shouldBeFalse()
        }
    }

    @Test
    fun `result is trimmed before delivery`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone() },
            transcribe = { "  padded  " }
        )

        service.startListening(testScope, onFinalResult)
        waitFor { service.isListening.value.shouldBeTrue() }
        service.stopListening()
        waitFor { captured shouldContainExactly listOf("padded") }
    }

    @Test
    fun `blank transcription is not delivered`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone() },
            transcribe = { "   " }
        )

        service.startListening(testScope, onFinalResult)
        waitFor { service.isListening.value.shouldBeTrue() }
        service.stopListening()
        waitFor { service.isListening.value.shouldBeFalse() }
        captured.shouldHaveSize(0)
    }

    @Test
    fun `transcriber failure is swallowed and listening state still resets`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone() },
            transcribe = { throw RuntimeException("boom") }
        )

        service.startListening(testScope, onFinalResult)
        waitFor { service.isListening.value.shouldBeTrue() }
        service.stopListening()
        waitFor { service.isListening.value.shouldBeFalse() }
        captured.shouldHaveSize(0)
    }

    @Test
    fun `nothing captured means transcriber is not invoked`() {
        val silentLine = mockk<TargetDataLine>(relaxed = true)
        every { silentLine.read(any(), any(), any()) } answers {
            Thread.sleep(5); 0
        }
        val transcriberCalls = AtomicInteger(0)
        val service = VoiceRecognitionService(
            microphoneFactory = { silentLine },
            transcribe = { transcriberCalls.incrementAndGet(); "should not happen" }
        )

        service.startListening(testScope, onFinalResult)
        waitFor { service.isListening.value.shouldBeTrue() }
        service.stopListening()
        waitFor { service.isListening.value.shouldBeFalse() }
        transcriberCalls.get() shouldBe 0
        captured.shouldHaveSize(0)
    }

    @Test
    fun `microphone open failure leaves the service idle`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { throw RuntimeException("no mic") },
            transcribe = { "irrelevant" }
        )

        service.startListening(testScope, onFinalResult)

        service.isListening.value.shouldBeFalse()
        captured.shouldHaveSize(0)
    }

    @Test
    fun `startListening is a no-op while already listening`() {
        val factoryCalls = AtomicInteger(0)
        val service = VoiceRecognitionService(
            microphoneFactory = {
                factoryCalls.incrementAndGet()
                fakeMicrophone()
            },
            transcribe = { "" }
        )

        service.startListening(testScope, onFinalResult)
        waitFor { service.isListening.value.shouldBeTrue() }
        service.startListening(testScope, onFinalResult)

        factoryCalls.get() shouldBe 1
        service.close()
    }

    @Test
    fun `stopListening when idle is a no-op`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone() },
            transcribe = { "" }
        )
        service.stopListening()
        service.isListening.value.shouldBeFalse()
    }

    @Test
    fun `resetAccumulatedText clears the partial result`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone() },
            transcribe = { "" }
        )
        service.resetAccumulatedText()
        service.partialResult.value shouldBe ""
    }

    @Test
    fun `close cancels capture and prevents further startListening`() {
        val service = VoiceRecognitionService(
            microphoneFactory = { fakeMicrophone() },
            transcribe = { "" }
        )
        service.startListening(testScope, onFinalResult)
        waitFor { service.isListening.value.shouldBeTrue() }

        service.close()

        service.isListening.value.shouldBeFalse()
        try {
            service.startListening(testScope, onFinalResult)
            error("expected IllegalStateException after close()")
        } catch (_: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `pcmToWav writes a valid 16-bit mono WAV header`() {
        val pcm = ByteArray(200) { (it and 0xFF).toByte() }
        val wav = VoiceRecognitionService.pcmToWav(pcm, 16_000f)

        wav.size shouldBe 44 + pcm.size
        String(wav.copyOfRange(0, 4)) shouldBe "RIFF"
        String(wav.copyOfRange(8, 12)) shouldBe "WAVE"
        String(wav.copyOfRange(12, 16)) shouldBe "fmt "
        String(wav.copyOfRange(36, 40)) shouldBe "data"

        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        header.getInt(4) shouldBe 36 + pcm.size
        header.getInt(16) shouldBe 16
        header.getShort(20) shouldBe 1.toShort()
        header.getShort(22) shouldBe 1.toShort()
        header.getInt(24) shouldBe 16_000
        header.getInt(28) shouldBe 16_000 * 2
        header.getShort(32) shouldBe 2.toShort()
        header.getShort(34) shouldBe 16.toShort()
        header.getInt(40) shouldBe pcm.size

        wav.copyOfRange(44, wav.size) shouldBe pcm
    }
}
