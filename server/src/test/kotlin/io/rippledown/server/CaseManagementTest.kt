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
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.createCase
import io.rippledown.model.external.serialize
import io.rippledown.server.routes.ID_SHOULD_BE_A_LONG
import io.rippledown.server.routes.MISSING_CASE_ID
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
    fun `should return a viewable case`() = testApplication {
        setup()
        val rdrCase = RDRCase(CaseId(1, "Case1"))
        val viewableCase = ViewableCase(rdrCase)
        val caseId = 1L
        every { kbEndpoint.viewableCase(caseId) } returns viewableCase

        val result = httpClient.get(CASE) {
            parameter(CASE_ID, caseId)
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<ViewableCase>() shouldBe viewableCase
    }

    @Test
    fun `should throw exception when trying to get a case which is not founc`() = testApplication {
        //Given
        setup()
        val message = "Case not found"
        every { kbEndpoint.viewableCase(any()) } throws Exception(message)

        //When
        val response = httpClient.get(CASE) {
            parameter(CASE_ID, 42L)
                parameter(KB_ID, kbId)
            }

        //Then
        response.status shouldBe HttpStatusCode.BadRequest
        response.body<String>() shouldBe message
    }

    @Test
    fun `Should return BadRequest if the case id is not a Long`() = testApplication {
        //Given
        setup()

        //When
        val response = httpClient.get(CASE) {
            parameter(CASE_ID, "badId")
            parameter(KB_ID, kbId)
        }

        //Then
        response.status shouldBe HttpStatusCode.BadRequest
        response.body<String>() shouldBe ID_SHOULD_BE_A_LONG
    }

    @Test
    fun `Should return BadRequest if the case id parameter is missing`() = testApplication {
        //Given
        setup()

        //When
        val response = httpClient.get(CASE) {
            parameter(KB_ID, kbId)
        }

        //Then
        response.status shouldBe HttpStatusCode.BadRequest
        response.body<String>() shouldBe MISSING_CASE_ID
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