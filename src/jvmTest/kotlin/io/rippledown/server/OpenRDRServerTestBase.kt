package io.rippledown.server

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.mockkClass
import io.mockk.spyk
import io.rippledown.server.routes.attributeManagement
import io.rippledown.server.routes.caseManagement
import io.rippledown.server.routes.kbManagement
import io.rippledown.server.routes.serverManagement

open class OpenRDRServerTestBase {
    lateinit var serverApplication: ServerApplication
    lateinit var serverApplicationSpy: ServerApplication
    lateinit var serverApplicationMock: ServerApplication
    lateinit var httpClient: HttpClient

    fun ApplicationTestBuilder.setupWithSpy() {
        setup()
    }

    fun ApplicationTestBuilder.setupWithMock() {
        setup(false)
    }

    private fun ApplicationTestBuilder.setup(useSpy: Boolean = true) {
        serverApplication = ServerApplication()
        serverApplicationSpy = spyk(serverApplication)
        serverApplicationMock = mockkClass(ServerApplication::class)
        httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val applicationToUse = if (useSpy) serverApplicationSpy else serverApplicationMock
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            serverManagement()
            kbManagement(applicationToUse)
            caseManagement(applicationToUse)
            attributeManagement(applicationToUse)
        }
    }
}