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
import io.rippledown.constants.api.INTERPRET_CASE
import io.rippledown.constants.api.PROCESS_CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.KB_NAME
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.external.serialize
import io.rippledown.server.routes.ID_SHOULD_BE_A_LONG
import io.rippledown.server.routes.MISSING_CASE_ID
import io.rippledown.utils.createViewableCase
import kotlin.test.Test

class InterpreterTest : OpenRDRServerTestBase() {

    @Test
    fun interpretCase() = testApplication {
        setupServer()
        val case = CaseTestUtils.getCase("Case2")
        val caseData = case.serialize()
        val returnCase = createViewableCase("Case2").case
        every { kbEndpoint.processCase(case) } returns returnCase
        val result = httpClient.put(INTERPRET_CASE) {
            contentType(ContentType.Application.Json)
            setBody(caseData)
            parameter(KB_NAME, kbName)
        }
        result.status shouldBe HttpStatusCode.Accepted
        result.body<RDRCase>() shouldBe returnCase
        verify { kbEndpoint.processCase(case) }
    }
}