package io.rippledown.server.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.rippledown.constants.api.CASE_REPORT
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.kb.ai.InterpretationIncompleteException
import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.caseview.IncompleteInterpretation
import io.rippledown.model.report.CaseReport
import io.rippledown.server.OpenRDRServerTestBase
import kotlin.test.Test

class ReportManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should return case report for valid case id`() = testApplication {
        // Given
        setupServer()
        val caseId = 42L
        val report = CaseReport(markdown = "Test report", generated = true)
        coEvery { kbEndpoint.caseReport(caseId) } returns report

        // When
        val result = httpClient.get(CASE_REPORT) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, caseId)
        }

        // Then
        result.status shouldBe HttpStatusCode.OK
        result.body<CaseReport>() shouldBe report
    }

    @Test
    fun `should return 500 when report generation fails`() = testApplication {
        // Given
        setupServer()
        coEvery { kbEndpoint.caseReport(any()) } throws Exception("Case not found")

        // When
        val result = httpClient.get(CASE_REPORT) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, 99999)
        }

        // Then
        result.status shouldBe HttpStatusCode.InternalServerError
    }

    @Test
    fun `an incomplete interpretation is answered with the diagnosis rather than a report`() = testApplication {
        // Given
        setupServer()
        val incomplete = IncompleteInterpretation(42L, IncompleteCategory.ABANDONED, null, "The case no longer exists.")
        coEvery { kbEndpoint.caseReport(42L) } throws InterpretationIncompleteException(incomplete)

        // When
        val result = httpClient.get(CASE_REPORT) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, 42L)
        }

        // Then
        result.status shouldBe INCOMPLETE_INTERPRETATION_STATUS
        result.body<IncompleteInterpretation>() shouldBe incomplete
    }
}
