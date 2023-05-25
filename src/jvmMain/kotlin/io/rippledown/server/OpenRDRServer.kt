package io.rippledown.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.server.routes.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

const val SHUTDOWN = "/api/shutdown"
const val PING = "/api/ping"

const val STARTING_SERVER = "Starting server"
const val STOPPING_SERVER = "Stopping server"

lateinit var server: NettyApplicationEngine

val logger = LoggerFactory.getLogger("rdr")

fun main() {

    server = embeddedServer(Netty, 9090, module = Application::applicationModule)
    logger.info(STARTING_SERVER)
    server.start(wait = true)
}

fun Application.applicationModule() {
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            call.request.httpMethod
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
        }
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        anyHost()
    }
    install(Compression) {
        gzip()
    }
    routing {
        get("/") {
            call.respondText(
                this::class.java.classLoader.getResource("index.html")!!.readText(),
                ContentType.Text.Html
            )
        }
        staticResources(remotePath = "/", basePackage = "")
    }
    val application = ServerApplication()
    serverManagement()
    kbManagement(application)
    caseManagement(application)
    interpManagement(application)
    ruleSession(application)
    conditionManagement(application)
}