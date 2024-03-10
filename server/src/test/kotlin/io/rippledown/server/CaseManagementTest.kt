package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.CaseTestUtils
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.DELETE_CASE_WITH_NAME
import io.rippledown.constants.api.PROCESS_CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.createCase
import io.rippledown.model.external.serialize
import io.rippledown.server.routes.KB_ID
import kotlin.test.Test

class CaseManagementTest : OpenRDRServerTestBase() {

    @Test
    fun waitingCases() = testApplication {
        setup()
        val casesInfo = CasesInfo(listOf(CaseId("Tea"), CaseId("Coffee")))
        every { kbEndpoint.waitingCasesInfo() } returns casesInfo
        val result = httpClient.get(WAITING_CASES) { parameter(KB_ID, kbId) }
        result.status shouldBe HttpStatusCode.OK
        result.body<CasesInfo>() shouldBe casesInfo
        verify { kbEndpoint.waitingCasesInfo() }
    }

    @Test
    fun viewableCase() = testApplication {
        setup()
        val rdrCase = RDRCase(CaseId(1, "Case1"))
        val viewableCase = ViewableCase(rdrCase)
        val caseId = 1L
        every { kbEndpoint.viewableCase(caseId) } returns viewableCase

        val result = httpClient.get(CASE) {
            parameter("id", caseId)
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<ViewableCase>() shouldBe viewableCase
    }

    @Test
    fun `should return error status the case with that id has been deleted`() = testApplication {
        setup()
        val result = httpClient.get(CASE) {
            parameter("id", 42L)
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun viewableCaseBadId() = testApplication {
        setup()
        var ok = false
        try {
            httpClient.get(CASE) {
                parameter("id", "badId")
                parameter(KB_ID, kbId)
            }
        } catch (e: Exception) {
            ok = true

        }
        ok shouldBe true
    }

    @Test
    fun provideCase() = testApplication {
        setup()
        val case = CaseTestUtils.getCase("Case2")
        val caseData = case.serialize()
        val returnCase = createCase("Case2").case
        every { kbEndpoint.processCase(case) } returns returnCase
        val result = httpClient.put(PROCESS_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.Accepted
        result.body<RDRCase>() shouldBe returnCase
        verify { kbEndpoint.processCase(case) }
    }

    @Test
    fun deleteProcessedCaseWithName() = testApplication {
        setup()
        val caseName = "The Case"
        every { kbEndpoint.deleteCase(caseName) } returns Unit
        val result = httpClient.delete(DELETE_CASE_WITH_NAME) {
            contentType(ContentType.Application.Json)
            parameter(KB_ID, kbId)
            parameter("name", caseName)
        }
        result.status shouldBe HttpStatusCode.OK
        verify { kbEndpoint.deleteCase(caseName) }
    }
}