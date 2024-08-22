package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.ALL_CONCLUSIONS
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

    @Test
    fun `should return all conclusions`() = testApplication {
        setup()
        val text1 = "Glucose is high."
        val text2 = "Fructose is high."
        val conclusion1 = Conclusion(8, text1)
        val conclusion2 = Conclusion(9, text2)
        every { kbEndpoint.allConclusions() } returns setOf(conclusion1, conclusion2)
        val result = httpClient.get(ALL_CONCLUSIONS) {
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Set<Conclusion>>() shouldBe setOf(conclusion1, conclusion2)
        verify { kbEndpoint.allConclusions() }
    }
 }