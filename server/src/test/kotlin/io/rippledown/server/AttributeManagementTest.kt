package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.GET_OR_CREATE_ATTRIBUTE
import io.rippledown.constants.api.SET_ATTRIBUTE_ORDER
import io.rippledown.model.Attribute
import io.rippledown.server.routes.KB_ID
import kotlinx.serialization.json.Json
import kotlin.test.Test

class AttributeManagementTest: OpenRDRServerTestBase() {
    @Test
    fun getOrCreateAttribute() = testApplication {
        setup()
        val glucose = "Glucose"
        val attribute = Attribute(4000, glucose)
        every { kbEndpoint.getOrCreateAttribute(glucose) } returns attribute
        val result = httpClient.post(GET_OR_CREATE_ATTRIBUTE) {
            parameter(KB_ID, kbId)
            setBody(glucose)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Attribute>() shouldBe attribute
        verify { kbEndpoint.getOrCreateAttribute(glucose) }
    }

    @Test
    fun setAttributeOder() = testApplication {
        setup()
        val tsh = Attribute(100,"TSH")
        val ft3 = Attribute(110,"FT3")
        val ft4 = Attribute(120,"FT4")
        val attributesInOrder = listOf(tsh, ft3, ft4)
        every { kbEndpoint.setAttributeOrder(attributesInOrder) } returns Unit
        val result = httpClient.post(SET_ATTRIBUTE_ORDER) {
            parameter(KB_ID, kbId)
            contentType(ContentType.Application.Json)
            setBody(attributesInOrder)
        }
        result.status shouldBe HttpStatusCode.OK
        verify { kbEndpoint.setAttributeOrder(attributesInOrder) }
    }
 }