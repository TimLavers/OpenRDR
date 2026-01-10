package io.rippledown.server.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.KB_DESCRIPTION
import io.rippledown.constants.api.KB_INFO
import io.rippledown.constants.api.LAST_RULE_DESCRIPTION
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.KBInfo
import io.rippledown.model.rule.UndoRuleDescription
import io.rippledown.server.OpenRDRServerTestBase
import kotlin.test.Test

class KBEditingTest: OpenRDRServerTestBase() {

    @Test
    fun kbDescription() = testApplication {
        setupServer()
        val description = """
            # OpenRDR

            An open source knowledge acquisition system using the RippleDown approach.

            RippleDown Rules (RDR) was initially developed by [Paul Compton](https://cgi.cse.unsw.edu.au/~compton/) in the 1980s. A
            good introduction is provided in "A Philosophical Basis for Knowledge Acquisition." Compton, P and Jansen, R, 1990.
            *Knowledge Acquisition* 2:241-257.

            A comprehensive description is given in his
            book ["Ripple-Down Rules, the Alternative to Machine Learning"](https://www.amazon.com.au/Ripple-Down-Rules-Alternative-Machine-Learning-ebook/dp/B092KVD3HQ)
            Paul Compton and Byeong Ho Kang, 2021. CRC Press.

            ## Requirements documentation

            The background, requirements, design principles, and so on to the project are documented in the
            `documentation` directory that is a sibling to this file. The starting point for the
            documentation is [OpenRDR](./documentation/openrdr.md).
        """.trimIndent()
        every { kbEndpoint.description() } returns description
        val result = httpClient.get(KB_DESCRIPTION) {
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.body<String>() shouldBe description
        verify { kbEndpoint.description() }
    }

    @Test
    fun setDescription() = testApplication {
        setupServer()
        val newDescription = "Whatever"
        val result = httpClient.post(KB_DESCRIPTION) {
            parameter(KB_ID, kbId)
            setBody(newDescription)
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        verify { kbEndpoint.setDescription(newDescription) }

    }

    @Test
    fun kbName() = testApplication {
        setupServer()
        val kbInfo = KBInfo("Glucose")
        every { kbEndpoint.kbInfo() } returns kbInfo
        val result = httpClient.get(KB_INFO) {
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.body<KBInfo>() shouldBe kbInfo
        verify { kbEndpoint.kbInfo() }
    }

    @Test
    fun lastUndoableRule() = testApplication {
        setupServer()
        val description = """
            This is an amazing
            rule that can be undone!
        """.trimIndent()
        val undoRuleDescription = UndoRuleDescription(description, true)
        every { kbEndpoint.descriptionOfMostRecentRule() } returns undoRuleDescription
        val result = httpClient.get(LAST_RULE_DESCRIPTION) {
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.body<UndoRuleDescription>() shouldBe undoRuleDescription
        verify { kbEndpoint.descriptionOfMostRecentRule() }
    }

    @Test
    fun undoLastRule() = testApplication {
        setupServer()
        val result = httpClient.delete(LAST_RULE_DESCRIPTION) {
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        verify { kbEndpoint.undoLastRule() }
    }
 }