package io.rippledown.server.routes

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.KB_NAME
import io.rippledown.server.OpenRDRServerTestBase
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class RoutingUtilitiesTest : OpenRDRServerTestBase() {
    lateinit var parameters: Parameters
    lateinit var call: RoutingCall
    lateinit var context: RoutingContext

    @BeforeEach
    fun setup() {
        parameters = mockk()
        call = mockk<RoutingCall>()
        every { call.parameters } returns parameters
        context = mockk<RoutingContext>()
        every { context.call } returns call
    }

    @Test
    fun `throw exception if no kb name supplied`() = testApplication {
        setupServer()
        shouldThrow<Exception> {
            context.kbEndpointByName(serverApplication)
        }.message shouldBe MISSING_KB_NAME
    }

    @Test
    fun `throw exception if blank kb name supplied`() = testApplication {
        setupServer()
        every { parameters.get(KB_NAME) } returns ""
        shouldThrow<Exception> {
            context.kbEndpointByName(serverApplication)
        }.message shouldBe MISSING_KB_NAME
    }

    @Test
    fun `use kb name`() = testApplication {
        setupServer()
        every { parameters.get(KB_NAME) } returns "Otford"
        every { serverApplication.kbForName("Otford") } returns mockk()
        context.kbEndpointByName(serverApplication)
        verify { serverApplication.kbForName("Otford") }
    }

    @Test
    fun `throw exception if no kb id supplied`() = testApplication {
        setupServer()
        shouldThrow<Exception> {
            context.kbEndpoint(serverApplication)
        }.message shouldBe MISSING_KB_ID
    }

    @Test
    fun `throw exception if blank kb id supplied`() = testApplication {
        setupServer()
        every { parameters.get(KB_ID) } returns ""
        shouldThrow<Exception> {
            context.kbEndpoint(serverApplication)
        }.message shouldBe MISSING_KB_ID
    }

    @Test
    fun `use kb id`() = testApplication {
        setupServer()
        every { parameters.get(KB_ID) } returns "KB123"
        context.kbEndpoint(serverApplication)
        verify { serverApplication.kbForId("KB123") }
    }

    @Test
    fun kbIdTest() = testApplication {
        setupServer()

        every { parameters.get(KB_ID) } returns null
        shouldThrow<Exception> {
            context.kbId()
        }.message shouldBe MISSING_KB_ID

        every { parameters.get(KB_ID) } returns ""
        shouldThrow<Exception> {
            context.kbId()
        }.message shouldBe MISSING_KB_ID

        every { parameters.get(KB_ID) } returns "Hurstville"
        context.kbId() shouldBe "Hurstville"
    }

    @Test
    fun caseIdTest() = testApplication {
        setupServer()

        every { parameters.get(CASE_ID) } returns null
        shouldThrow<Exception> {
            context.caseId()
        }.message shouldBe MISSING_CASE_ID

        every { parameters.get(CASE_ID) } returns ""
        shouldThrow<Exception> {
            context.caseId()
        }.message shouldBe MISSING_CASE_ID

        every { parameters.get(CASE_ID) } returns "Five"
        shouldThrow<Exception> {
            context.caseId()
        }.message shouldBe ID_SHOULD_BE_A_LONG

        every { parameters.get(CASE_ID) } returns "1000"
        context.caseId() shouldBe 1000L
    }

    @Test
    fun kbIdOrNullNullTest() = testApplication {
        setupServer()
        every { parameters[KB_ID] } returns null
        context.kbIdOrNull() shouldBe null
    }

    @Test
    fun kbIdOrNullTest() = testApplication {
        setupServer()
        every { parameters[KB_ID] } returns "Otford"
        context.kbIdOrNull() shouldBe "Otford"
    }
}