package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.rippledown.constants.api.CASE_REPORT
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.report.CaseReport
import kotlin.test.Test

class ReportManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate report generation to the server application`() = testApplication {
        //Given
        setupServer()
        val caseId = 42L
        val report = CaseReport(markdown = "The haemoglobin is elevated.")
        coEvery { kbEndpoint.caseReport(caseId) } returns report

        //When
        val result = httpClient.get(CASE_REPORT) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, caseId)
        }

        //Then
        coVerify { kbEndpoint.caseReport(caseId) }
        result.status shouldBe HttpStatusCode.OK
        result.body<CaseReport>() shouldBe report
    }

    @Test
    fun `should respond with 500 when report generation fails`() = testApplication {
        //Given
        setupServer()
        val caseId = 42L
        coEvery { kbEndpoint.caseReport(caseId) } throws Exception("LLM call failed")

        //When
        val result = httpClient.get(CASE_REPORT) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, caseId)
        }

        //Then
        result.status shouldBe HttpStatusCode.InternalServerError
    }
}
