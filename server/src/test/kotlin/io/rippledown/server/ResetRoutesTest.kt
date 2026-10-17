package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.server.IN_MEMORY
import io.rippledown.constants.server.PING
import io.rippledown.constants.server.RESET
import io.rippledown.constants.server.TEST_RESET_FLAG
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ResetRoutesTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `the reset route requires explicit opt-in independently of persistence`(inMemory: Boolean) = testApplication {
        // Given - the non-memory provider uses a double so no database instance is needed
        val persistence = if (inMemory) InMemoryPersistenceProvider() else mockk<PersistenceProvider>().also {
            every { it.idStore().data() } returns emptyMap()
        }
        application { module(persistence = persistence) }

        // When / Then
        client.post(RESET).status shouldBe HttpStatusCode.NotFound
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `explicit opt-in enables reset independently of persistence`(inMemory: Boolean) = testApplication {
        // Given
        val persistence = if (inMemory) InMemoryPersistenceProvider() else mockk<PersistenceProvider>().also {
            every { it.idStore().data() } returns emptyMap()
        }
        application { module(persistence = persistence, enableTestReset = true) }

        // When / Then
        client.post(RESET).status shouldBe HttpStatusCode.OK
        client.get(PING).status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `only the explicit test flag enables reset`() {
        // Given / When / Then
        testResetEnabled(emptyArray()) shouldBe false
        testResetEnabled(arrayOf(IN_MEMORY)) shouldBe false
        testResetEnabled(arrayOf(IN_MEMORY, "Demo")) shouldBe false
        testResetEnabled(arrayOf(IN_MEMORY, TEST_RESET_FLAG)) shouldBe true
        testResetEnabled(arrayOf(TEST_RESET_FLAG)) shouldBe true
    }

    @Test
    fun `HTTP work remains admitted until the handler finishes and reset rejects new work`() = testApplication {
        // Given
        val gate = TestRequestGate()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        application {
            module(persistence = InMemoryPersistenceProvider(), enableTestReset = true, resetGate = gate)
            routing {
                get("/slow") {
                    entered.complete(Unit)
                    release.await()
                    call.respondText("done")
                }
            }
        }
        coroutineScope {
            val slow = async { client.get("/slow") }
            entered.await()

            // When
            val reset = async { client.post(RESET) }
            // The direct admission probe avoids relying on timing or real sleeps.
            withTimeout(5_000) {
                while (gate.enterRequest()) {
                    gate.leaveRequest()
                    yield()
                }
            }

            // Then
            reset.isCompleted shouldBe false
            client.get("/slow").status shouldBe HttpStatusCode.ServiceUnavailable
            client.get(PING).status shouldBe HttpStatusCode.OK

            // When
            release.complete(Unit)

            // Then
            slow.await().status shouldBe HttpStatusCode.OK
            reset.await().status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `a handler exception releases admission and permits a later reset`() = testApplication {
        // Given
        application {
            module(persistence = InMemoryPersistenceProvider(), enableTestReset = true)
            routing { get("/fails") { error("Handler failed") } }
        }

        // When
        client.get("/fails").status shouldBe HttpStatusCode.InternalServerError

        // Then
        client.post(RESET).status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `drain failure returns conflict and leaves application routes unavailable`() = testApplication {
        // Given
        val gate = TestRequestGate(drainTimeoutMillis = 20)
        gate.enterRequest()
        application { module(persistence = InMemoryPersistenceProvider(), enableTestReset = true, resetGate = gate) }

        // When
        client.post(RESET).status shouldBe HttpStatusCode.Conflict
        gate.leaveRequest()

        // Then
        client.get("/").status shouldBe HttpStatusCode.ServiceUnavailable
        client.post(RESET).status shouldBe HttpStatusCode.Conflict
        client.get(PING).status shouldBe HttpStatusCode.OK
    }
}
