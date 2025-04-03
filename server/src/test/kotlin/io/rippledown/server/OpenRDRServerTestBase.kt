package io.rippledown.server

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.rippledown.server.routes.*
import kotlinx.serialization.json.Json

open class OpenRDRServerTestBase {
    val kbId = "2023"
    lateinit var kbEndpoint: KBEndpoint
    lateinit var serverApplication: ServerApplication
    lateinit var httpClient: HttpClient

    fun ApplicationTestBuilder.setup() {
        kbEndpoint = mockk<KBEndpoint>(relaxed = true)
        serverApplication = mockk<ServerApplication>(relaxed = true)
        every { serverApplication.kbForId(kbId) } returns kbEndpoint
        httpClient = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    allowStructuredMapKeys = true
                })
            }
        }
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            serverManagement()
            kbManagement(serverApplication)
            kbEditing(serverApplication)
            caseManagement(serverApplication)
            attributeManagement(serverApplication)
            conclusionManagement(serverApplication)
            conditionManagement(serverApplication)
            ruleSession(serverApplication)
        }
    }
}