package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.VERIFIED_INTERPRETATION_SAVED
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import kotlin.test.Test

class CaseManagementTest: OpenRDRServerTestBase() {

    @Test
    fun waitingCases() = testApplication {
        setup()
        val casesInfo = CasesInfo(listOf(CaseId("Tea"), CaseId("Coffee")))
        every { serverApplication.waitingCasesInfo() } returns casesInfo
        val result = httpClient.get(WAITING_CASES)
        result.status shouldBe HttpStatusCode.OK
        result.body<CasesInfo>() shouldBe casesInfo
        verify { serverApplication.waitingCasesInfo() }
    }

    @Test
    fun viewableCase() = testApplication {
        setup()
        val rdrCase = RDRCase("Case1")
        val viewableCase = ViewableCase(rdrCase)
        val caseId = "Case1"
        every { serverApplication.viewableCase(caseId) } returns viewableCase

        val result = httpClient.get(CASE) {
            parameter("id", caseId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<ViewableCase>() shouldBe viewableCase
    }

    @Test
    fun viewableCaseNoId() = testApplication {
        setup()
        shouldThrow<IllegalStateException> {
            httpClient.get(CASE)
        }
    }

    @Test
    fun viewableCaseBadId() = testApplication {
        setup()
        val result = httpClient.get(CASE) {
            parameter("id", "Case123")
        }
        result.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun saveInterpretation() = testApplication {
        setup()
        val rdrCase = RDRCase("Case1")
        val viewableCase = ViewableCase(rdrCase)
        val interpretation = viewableCase.interpretation.apply {
            verifiedText = "Verified Text"
        }
        every { serverApplication.saveInterpretation(interpretation) } returns Unit

        val result = httpClient.post(VERIFIED_INTERPRETATION_SAVED) {
            contentType(ContentType.Application.Json)
            setBody(interpretation)
        }
        result.status shouldBe HttpStatusCode.OK
        verify { serverApplication.saveInterpretation(interpretation) }
    }
}