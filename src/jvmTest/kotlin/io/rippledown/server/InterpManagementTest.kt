package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.CASE_NAME
import io.rippledown.constants.api.DIFF
import io.rippledown.model.diff.*
import kotlin.test.Test

class InterpManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate diff list to the server application`() = testApplication {
        setup()
        val case = "Case A"
        val diffList = DiffList(
            listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            )
        )
        every { serverApplication.diffListForCase(case) } returns diffList

        val result = httpClient.get(DIFF) {
            parameter(CASE_NAME, case)
        }
        verify { serverApplication.diffListForCase(case) }
        result.status shouldBe HttpStatusCode.OK
        result.body<DiffList>() shouldBe diffList
    }
}