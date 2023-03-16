package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.server.routes.GET_OR_CREATE_ATTRIBUTE
import kotlin.test.Test

class AttributeManagementTest: OpenRDRServerTestBase() {
    @Test
    fun getOrCreateAttribute() = testApplication {
        setupWithMock()
        val glucose = "Glucose"
        val attribute = Attribute(glucose, 4000)
        every { serverApplicationMock.getOrCreateAttribute(glucose) } returns attribute
        val result = httpClient.post(GET_OR_CREATE_ATTRIBUTE) {
            setBody(glucose)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<Attribute>() shouldBe attribute
        verify { serverApplicationMock.getOrCreateAttribute(glucose) }
    }
 }