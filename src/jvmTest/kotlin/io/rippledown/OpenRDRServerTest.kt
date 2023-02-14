package io.rippledown

import CREATE_KB
import KB_INFO
import PING
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.server.ServerApplication
import kbManagement
import serverManagement
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenRDRServerTest {
    lateinit var serverApplication: ServerApplication
    lateinit var httpClient: HttpClient

    @Test
    fun testPing() = testApplication {
        setup()
        val response = httpClient.get(PING)
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun kbName() = testApplication {
        setup()
        val result = httpClient.get(KB_INFO)
        result.status shouldBe HttpStatusCode.OK
        result.body<KBInfo>().name shouldBe serverApplication.kb.name
    }

    @Test
    fun createKB() = testApplication {
        setup()
        val result = httpClient.post(CREATE_KB)
        result.status shouldBe HttpStatusCode.OK
        result.body<OperationResult>().message shouldBe "KB created"
    }

    private fun ApplicationTestBuilder.setup() {
        serverApplication = ServerApplication()
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            serverManagement()
            kbManagement(serverApplication)
        }
        httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}