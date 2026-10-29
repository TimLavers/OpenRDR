package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.rippledown.CaseTestUtils
import io.rippledown.constants.api.*
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.kb.ai.InterpretationIncompleteException
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.caseview.IncompleteInterpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.external.serialize
import io.rippledown.server.routes.ID_SHOULD_BE_A_LONG
import io.rippledown.server.routes.INCOMPLETE_INTERPRETATION_STATUS
import io.rippledown.server.routes.MISSING_CASE_ID
import io.rippledown.utils.createViewableCase
import kotlin.test.Test

class CaseManagementTest : OpenRDRServerTestBase() {

    @Test
    fun waitingCases() = testApplication {
        setupServer()
        val casesInfo = CasesInfo(listOf(CaseId("Tea"), CaseId("Coffee")))
        every { kbEndpoint.waitingCasesInfo() } returns casesInfo
        val result = httpClient.get(WAITING_CASES) { parameter(KB_ID, kbId) }
        result.status shouldBe HttpStatusCode.OK
        result.body<CasesInfo>() shouldBe casesInfo
        verify { kbEndpoint.waitingCasesInfo() }
    }

    @Test
    fun `should return a viewable case`() = testApplication {
        setupServer()
        val rdrCase = RDRCase(CaseId(1, "Case1"))
        val viewableCase = ViewableCase(rdrCase)
        val caseId = 1L
        coEvery { kbEndpoint.viewableCase(caseId) } returns viewableCase

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
        setupServer()
        val message = "Case not found"
        coEvery { kbEndpoint.viewableCase(any()) } throws Exception(message)

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
    fun `a case whose interpretation is incomplete is answered with the diagnosis`() = testApplication {
        //Given
        setupServer()
        val incomplete = IncompleteInterpretation(42L, IncompleteCategory.EVALUATION_FAILED, "hasDiabetes", "Gemini unavailable")
        coEvery { kbEndpoint.viewableCase(42L) } throws InterpretationIncompleteException(incomplete)

        //When
        val response = httpClient.get(CASE) {
            parameter(CASE_ID, 42L)
            parameter(KB_ID, kbId)
        }

        //Then
        response.status shouldBe INCOMPLETE_INTERPRETATION_STATUS
        response.body<IncompleteInterpretation>() shouldBe incomplete
    }

    @Test
    fun `Should return BadRequest if the case id is not a Long`() = testApplication {
        //Given
        setupServer()

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
        setupServer()

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
        setupServer()
        val case = CaseTestUtils.getCase("Case2")
        val caseData = case.serialize()
        val returnCase = createViewableCase("Case2").case
        coEvery { kbEndpoint.processCase(case) } returns returnCase
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()
        val result = httpClient.put(PROCESS_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.Accepted
        result.body<RDRCase>() shouldBe returnCase
        coVerify { kbEndpoint.processCase(case) }
    }

    @Test
    fun `a processed case whose interpretation is incomplete is answered with the retained id and still announced`() =
        testApplication {
            //Given
            setupServer()
            val case = CaseTestUtils.getCase("Case2")
            val incomplete = IncompleteInterpretation(7L, IncompleteCategory.EVALUATION_FAILED, "hasDiabetes", "Gemini unavailable")
            val updatedCasesInfo = CasesInfo(listOf(CaseId(id = 7, name = "Case2")), kbName = "TestKB")
            coEvery { kbEndpoint.processCase(case) } throws InterpretationIncompleteException(incomplete)
            every { kbEndpoint.waitingCasesInfo() } returns updatedCasesInfo

            //When
            val result = httpClient.put(PROCESS_CASE) {
                contentType(ContentType.Application.Json)
                setBody(case.serialize())
                parameter(KB_ID, kbId)
            }

            //Then
            result.status shouldBe INCOMPLETE_INTERPRETATION_STATUS
            result.body<IncompleteInterpretation>() shouldBe incomplete
            coVerify { webSocketManager.sendCasesInfo(updatedCasesInfo) }
        }

    @Test
    fun `should push CasesInfo via WebSocket after processing a case`() = testApplication {
        //Given
        setupServer()
        val case = CaseTestUtils.getCase("Case1")
        val caseData = case.serialize()
        val returnCase = createViewableCase("Case1").case
        val updatedCasesInfo = CasesInfo(listOf(CaseId(id = 1, name = "Case1")), kbName = "TestKB")
        coEvery { kbEndpoint.processCase(case) } returns returnCase
        every { kbEndpoint.waitingCasesInfo() } returns updatedCasesInfo

        //When
        httpClient.put(PROCESS_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
            parameter(KB_ID, kbId)
        }

        //Then
        coVerify { webSocketManager.sendCasesInfo(updatedCasesInfo) }
    }

    @Test
    fun `should push CasesInfo via WebSocket after adding a cornerstone case`() = testApplication {
        //Given
        setupServer()
        val case = CaseTestUtils.getCase("Case3")
        val caseData = case.serialize()
        val returnCase = createViewableCase("Case3").case
        val updatedCasesInfo = CasesInfo(
            cornerstoneCaseIds = listOf(CaseId(id = 1, name = "Case3")),
            kbName = "TestKB"
        )
        every { kbEndpoint.addCornerstoneCase(case) } returns returnCase
        every { kbEndpoint.waitingCasesInfo() } returns updatedCasesInfo

        //When
        val result = httpClient.put(ADD_CORNERSTONE_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
            parameter(KB_ID, kbId)
        }

        //Then
        result.status shouldBe HttpStatusCode.Accepted
        coVerify { webSocketManager.sendCasesInfo(updatedCasesInfo) }
    }

    @Test
    fun deleteProcessedCaseWithName() = testApplication {
        setupServer()
        val caseName = "The Case"
        every { kbEndpoint.deleteCase(caseName, testUser) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()
        val result = httpClient.delete(DELETE_CASE_WITH_NAME) {
            contentType(ContentType.Application.Json)
            parameter(KB_ID, kbId)
            parameter("name", caseName)
        }
        result.status shouldBe HttpStatusCode.OK
        verify { kbEndpoint.deleteCase(caseName, testUser) }
    }

    @Test
    fun `should push CasesInfo via WebSocket after deleting a case`() = testApplication {
        //Given
        setupServer()
        val caseName = "DeleteMe"
        val updatedCasesInfo = CasesInfo(listOf(CaseId(id = 2, name = "Remaining")), kbName = "TestKB")
        every { kbEndpoint.deleteCase(caseName, testUser) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns updatedCasesInfo

        //When
        httpClient.delete(DELETE_CASE_WITH_NAME) {
            contentType(ContentType.Application.Json)
            parameter(KB_ID, kbId)
            parameter("name", caseName)
        }

        //Then
        coVerify { webSocketManager.sendCasesInfo(updatedCasesInfo) }
    }
}