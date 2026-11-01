package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.rippledown.CaseTestUtils
import io.rippledown.constants.api.INTERPRET_CASE
import io.rippledown.constants.server.KB_NAME
import io.rippledown.kb.ai.InterpretationIncompleteException
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.caseview.IncompleteInterpretation
import io.rippledown.model.external.serialize
import io.rippledown.server.routes.INCOMPLETE_INTERPRETATION_STATUS
import io.rippledown.utils.createViewableCase
import kotlin.test.Test

class InterpreterTest : OpenRDRServerTestBase() {

    @Test
    fun interpretCase() = testApplication {
        setupServer()
        val case = CaseTestUtils.getCase("Case2")
        val caseData = case.serialize()
        val returnCase = createViewableCase("Case2").case
        coEvery { kbEndpoint.processCase(case) } returns returnCase
        val result = httpClient.put(INTERPRET_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
            parameter(KB_NAME, kbName)
        }
        result.status shouldBe HttpStatusCode.Accepted
        result.body<RDRCase>() shouldBe returnCase
        coVerify { kbEndpoint.processCase(case) }
    }

    @Test
    fun interpretCaseSendsWebSocketUpdate() = testApplication {
        setupServer()
        val case = CaseTestUtils.getCase("Case2")
        val caseData = case.serialize()
        val returnCase = createViewableCase("Case2").case
        val casesInfo = CasesInfo(emptyList(), emptyList(), emptyList(), kbName)
        coEvery { kbEndpoint.processCase(case) } returns returnCase
        every { kbEndpoint.waitingCasesInfo() } returns casesInfo
        httpClient.put(INTERPRET_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
            parameter(KB_NAME, kbName)
        }
        coVerify { webSocketManager.sendCasesInfo(casesInfo) }
    }

    @Test
    fun `an incomplete interpretation is answered with the retained case id`() = testApplication {
        // Given
        setupServer()
        val case = CaseTestUtils.getCase("Case2")
        val incomplete = IncompleteInterpretation(7L, IncompleteCategory.EVALUATION_FAILED, "hasDiabetes", "Gemini unavailable")
        val casesInfo = CasesInfo(emptyList(), emptyList(), emptyList(), kbName)
        coEvery { kbEndpoint.processCase(case) } throws InterpretationIncompleteException(incomplete)
        every { kbEndpoint.waitingCasesInfo() } returns casesInfo

        // When
        val result = httpClient.put(INTERPRET_CASE) {
            contentType(ContentType.Application.Json)
            setBody(case.serialize())
            parameter(KB_NAME, kbName)
        }

        // Then
        result.status shouldBe INCOMPLETE_INTERPRETATION_STATUS
        result.body<IncompleteInterpretation>() shouldBe incomplete
        coVerify { webSocketManager.sendCasesInfo(casesInfo) }
    }
}