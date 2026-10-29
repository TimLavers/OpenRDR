package io.rippledown.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.rippledown.constants.api.PORT
import io.rippledown.constants.server.IN_MEMORY
import io.rippledown.constants.server.STARTING_SERVER
import io.rippledown.constants.server.TEST_RESET_FLAG
import io.rippledown.log.lazyLogger
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.persistence.postgres.PostgresPersistenceProvider
import io.rippledown.server.routes.*
import io.rippledown.server.websocket.WebSocketManager
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

private lateinit var persistenceProvider: PersistenceProvider

object OpenRDRServer {
    val logger = OpenRDRServer.lazyLogger
}

fun main(args: Array<String>) {
    OpenRDRServer.logger.info("Starting server with args: ${args.joinToString(", ")}")
    persistenceProvider = if (args.isNotEmpty() && args[0] == IN_MEMORY) {
        InMemoryPersistenceProvider()
    } else {
        PostgresPersistenceProvider()
    }
    server = embeddedServer(factory = Netty, port = PORT) {
        module(enableTestReset = testResetEnabled(args))
    }
    OpenRDRServer.logger.info(STARTING_SERVER)
    server.start(wait = true)
}

internal fun testResetEnabled(args: Array<String>) = TEST_RESET_FLAG in args

fun Application.module(
    persistence: PersistenceProvider = persistenceProvider,
    enableTestReset: Boolean = false,
    resetGate: TestRequestGate? = null
) {
    val gate = if (enableTestReset) resetGate ?: TestRequestGate() else null
    if (gate != null) testRequestAdmission(gate)
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging) {
        level = Level.DEBUG
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.header("User-Agent")
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
        }
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
    }
    install(Compression) {
        gzip()
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        get("/") {
            call.respondText(
                this::class.java.classLoader.getResource("index.html")!!.readText(),
                ContentType.Text.Html
            )
        }
        staticResources("/", "")
    }
    val webSocketManager = WebSocketManager(gate, scope = this)
    val application = ServerApplication(persistence, webSocketManager, gate)
    refusals()
    serverManagement()
    if (enableTestReset) testServerReset(application)
    kbManagement(application)
    kbEditing(application)
    caseManagement(application)
    interpreter(application)
    attributeManagement(application)
    conditionManagement(application)
    ruleSession(application)
    chatManagement(application)
    reportManagement(application)
    webSockets(webSocketManager)
}
