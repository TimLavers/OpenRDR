package io.rippledown.server

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenRDRServerTest: OpenRDRServerTestBase() {

    @Test
    fun testPing() = testApplication {
        setupWithSpy()
        val response = httpClient.get(PING)
        assertEquals(HttpStatusCode.OK, response.status)
    }
}