package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.server.PING
import io.rippledown.constants.server.SHUTDOWN
import io.rippledown.constants.server.STOPPING_SERVER
import io.rippledown.log.lazyLogger
import io.rippledown.server.server

fun Application.serverManagement() {
    val logger = lazyLogger
    routing {
        get(PING) {
            call.respond(HttpStatusCode.OK, "OK")
        }
        post(SHUTDOWN) {
            logger.info(STOPPING_SERVER)
            server.stop(0, 0)
        }
    }
}