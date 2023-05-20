package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.Conclusion
import io.rippledown.server.routes.GET_OR_CREATE_CONCLUSION
import kotlin.test.Test

class ConclusionManagementTest: OpenRDRServerTestBase() {
    @Test
    fun getOrCreateConclusion() = testApplication {
        setup()
        val text = "Glucose is high."
        val conclusion = Conclusion(8, text)
        every { serverApplication.getOrCreateConclusion(text) } returns conclusion
        val result = httpClient.post(GET_OR_CREATE_CONCLUSION) {
            setBody(text)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Conclusion>() shouldBe conclusion
        verify { serverApplication.getOrCreateConclusion(text) }
    }
 }