package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.CaseTestUtils
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.DELETE_PROCESSED_CASE_WITH_NAME
import io.rippledown.constants.api.PROCESS_CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.external.serialize
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
        val rdrCase = RDRCase(CaseId(1, "Case1"))
        val viewableCase = ViewableCase(rdrCase)
        val caseId = 1L
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
            parameter("id", 666)
        }
        result.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun provideCase() = testApplication {
        setup()
        val case = CaseTestUtils.getCase("Case2")
        val caseData = case.serialize()
        val returnCase = createCase("Case2").rdrCase
        every { serverApplication.processCase(case) } returns returnCase
        val result = httpClient.put(PROCESS_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
        }
        result.status shouldBe HttpStatusCode.Accepted
        result.body<RDRCase>() shouldBe returnCase
        verify { serverApplication.processCase(case) }
    }

    @Test
    fun deleteProcessedCaseWithName() = testApplication {
        setup()
        val caseName = "The Case"
        every { serverApplication.deleteProcessedCase(caseName) } returns Unit
        val result = httpClient.delete(DELETE_PROCESSED_CASE_WITH_NAME) {
            contentType(ContentType.Application.Json)
            setBody(CaseName(caseName))
        }
        result.status shouldBe HttpStatusCode.OK
        verify { serverApplication.deleteProcessedCase(caseName) }
    }
}