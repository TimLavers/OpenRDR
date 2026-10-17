package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.model.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestRequestGateTest {
    @Test
    fun `reset waits for admitted work and rejects new work until clearing finishes`() = runTest {
        // Given
        val gate = TestRequestGate()
        gate.enterRequest() shouldBe true
        val data = mutableListOf("old")

        // When
        val reset = async { gate.reset({ emptySet() }) { data.clear() } }
        runCurrent()

        // Then
        reset.isCompleted shouldBe false
        gate.enterRequest() shouldBe false
        gate.registerConnection { error("Must not register") } shouldBe false
        data shouldBe listOf("old")

        // When - the old request finishes a late mutation before leaving
        data.add("late")
        gate.leaveRequest()
        advanceUntilIdle()
        reset.await()

        // Then
        data shouldBe emptyList()
        gate.enterRequest() shouldBe true
        gate.leaveRequest()
        gate.reset({ emptySet() }) { data.clear() }
    }

    @Test
    fun `reset allows a disconnect to finish before clearing`() = runTest {
        // Given
        val gate = TestRequestGate()
        var connected = setOf(UserId("alice"))
        var cleared = false

        // When
        val reset = async { gate.reset({ connected }) { cleared = true } }
        runCurrent()

        // Then
        cleared shouldBe false

        // When
        connected = emptySet()
        advanceUntilIdle()
        reset.await()

        // Then
        cleared shouldBe true
    }

    @Test
    fun `a drain timeout preserves data and permanently closes admission`() = runTest {
        // Given
        val gate = TestRequestGate(drainTimeoutMillis = 100)
        gate.enterRequest()
        var cleared = false

        // When
        val failure = shouldThrow<ResetRejectedException> {
            gate.reset({ setOf(UserId("alice")) }) { cleared = true }
        }

        // Then
        failure.message.orEmpty() shouldContain "1 active request"
        failure.message.orEmpty() shouldContain "alice"
        cleared shouldBe false
        gate.leaveRequest()
        gate.enterRequest() shouldBe false
        shouldThrow<ResetRejectedException> { gate.reset({ emptySet() }) { cleared = true } }
        cleared shouldBe false
    }

    @Test
    fun `reset cancellation leaves admission closed`() = runTest {
        // Given
        val gate = TestRequestGate()
        gate.enterRequest()
        var cleared = false
        val reset = launch { gate.reset({ emptySet() }) { cleared = true } }
        runCurrent()

        // When
        reset.cancelAndJoin()
        gate.leaveRequest()

        // Then
        cleared shouldBe false
        gate.enterRequest() shouldBe false
        shouldThrow<ResetRejectedException> { gate.reset({ emptySet() }) {} }
    }

    @Test
    fun `a failing clear leaves admission closed`() = runTest {
        // Given
        val gate = TestRequestGate()

        // When
        shouldThrow<IllegalArgumentException> { gate.reset({ emptySet() }) { errorArgument() } }

        // Then
        gate.enterRequest() shouldBe false
        shouldThrow<ResetRejectedException> { gate.reset({ emptySet() }) {} }
    }

    @Test
    fun `concurrent reset is refused without disturbing the first reset`() = runTest {
        // Given
        val gate = TestRequestGate()
        gate.enterRequest()
        var clears = 0
        val first = async { gate.reset({ emptySet() }) { clears++ } }
        runCurrent()

        // When
        shouldThrow<ResetRejectedException> { gate.reset({ emptySet() }) { clears++ } }
        gate.leaveRequest()
        advanceUntilIdle()
        first.await()

        // Then
        clears shouldBe 1
        gate.registerConnection { clears++ } shouldBe true
        clears shouldBe 2
    }

    private fun errorArgument(): Nothing = throw IllegalArgumentException("Cannot delete KB")

    @Test
    fun `a delayed WebSocket upgrade from before reset cannot register afterwards`() = runTest {
        // Given
        val gate = TestRequestGate()
        val beforeReset = gate.admitRequest()
        gate.leaveRequest()
        var registered = false

        // When
        gate.reset({ emptySet() }) {}

        // Then
        gate.registerConnection(beforeReset) { registered = true } shouldBe false
        registered shouldBe false
        val afterReset = gate.admitRequest()
        gate.registerConnection(afterReset) { registered = true } shouldBe true
        registered shouldBe true
        gate.leaveRequest()
    }
}
