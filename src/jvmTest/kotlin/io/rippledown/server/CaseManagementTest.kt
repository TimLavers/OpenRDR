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
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.routes.CASE
import io.rippledown.server.routes.WAITING_CASES
import org.apache.commons.io.FileUtils
import java.lang.IllegalStateException
import kotlin.test.Test

class CaseManagementTest: OpenRDRServerTestBase() {
    @Test
    fun waitingCases() = testApplication {
        setupWithMock()
        val casesInfo = CasesInfo(listOf(CaseId("Tea"), CaseId("Coffee")))
        every { serverApplicationMock.waitingCasesInfo() } returns casesInfo
        val result = httpClient.get(WAITING_CASES)
        result.status shouldBe HttpStatusCode.OK
        result.body<CasesInfo>() shouldBe casesInfo
        verify { serverApplicationMock.waitingCasesInfo() }
    }

    @Test
    fun viewableCase() = testApplication {
        setupWithSpy()
        val caseId = "Case1"
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(caseId), serverApplication.casesDir)

        val result = httpClient.get(CASE) {
            parameter("id", caseId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<ViewableCase>().name shouldBe caseId
        verify { serverApplicationSpy.viewableCase(caseId) }
    }

    @Test
    fun viewableCaseNoId() = testApplication {
        setupWithSpy()
        shouldThrow<IllegalStateException> {
            httpClient.get(CASE)
        }
    }

    @Test
    fun viewableCaseBadId() = testApplication {
        setupWithSpy()
        val result = httpClient.get(CASE) {
            parameter("id", "Case123")
        }
        result.status shouldBe HttpStatusCode.BadRequest
    }
 }