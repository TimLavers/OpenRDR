package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.server.*
import io.rippledown.constants.server.*

fun Application.serverManagement() {
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