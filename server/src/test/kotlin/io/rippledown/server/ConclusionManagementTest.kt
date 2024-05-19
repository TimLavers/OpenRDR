package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.GET_OR_CREATE_CONCLUSION
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.Conclusion
import kotlin.test.Test

class ConclusionManagementTest: OpenRDRServerTestBase() {
    @Test
    fun getOrCreateConclusion() = testApplication {
        setup()
        val text = "Glucose is high."
        val conclusion = Conclusion(8, text)
        every { kbEndpoint.getOrCreateConclusion(text) } returns conclusion
        val result = httpClient.post(GET_OR_CREATE_CONCLUSION) {
            parameter(KB_ID, kbId)
            setBody(text)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Conclusion>() shouldBe conclusion
        verify { kbEndpoint.getOrCreateConclusion(text) }
    }
 }