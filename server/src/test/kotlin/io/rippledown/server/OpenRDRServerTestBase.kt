package io.rippledown.server

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.server.USER_ID_HEADER
import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.ChatCoordinator
import io.rippledown.model.UserId
import io.rippledown.server.routes.*
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.serialization.json.Json

open class OpenRDRServerTestBase {
    val kbId = "2023"
    val kbName = "Wisdom"
    val testUser = UserId("test-user")
    lateinit var kbEndpoint: KBEndpoint
    lateinit var kbSession: KBSession
    lateinit var serverApplication: ServerApplication
    lateinit var webSocketManager: WebSocketManager
    lateinit var chatCoordinator: ChatCoordinator
    lateinit var httpClient: HttpClient

    fun ApplicationTestBuilder.clientFor(userId: UserId): HttpClient = createClient {
        defaultRequest {
            header(USER_ID_HEADER, userId.value)
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                allowStructuredMapKeys = true
            })
        }
    }

    fun ApplicationTestBuilder.setupServer() {
        kbEndpoint = mockk<KBEndpoint>()
        kbSession = mockk<KBSession>()
        every { kbEndpoint.session } returns kbSession
        serverApplication = mockk<ServerApplication>()
        webSocketManager = mockk<WebSocketManager>()
        chatCoordinator = mockk<ChatCoordinator>()
        coEvery { webSocketManager.sendCasesInfo(any()) } returns Unit
        every { serverApplication.webSocketManager } returns webSocketManager
        every { serverApplication.chatCoordinator } returns chatCoordinator
        every { serverApplication.kbForId(kbId) } returns kbEndpoint
        every { serverApplication.kbForName(kbName) } returns kbEndpoint
        httpClient = clientFor(testUser)
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            refusals()
            serverManagement()
            kbManagement(serverApplication)
            kbEditing(serverApplication)
            caseManagement(serverApplication)
            interpreter(serverApplication)
            attributeManagement(serverApplication)
            conditionManagement(serverApplication)
            ruleSession(serverApplication)
            chatManagement(serverApplication)
            reportManagement(serverApplication)
        }
    }
}