package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.server.ServerApplication

fun Application.caseManagement(application: ServerApplication) {
    routing {
        get(WAITING_CASES) {
            call.respond(application.waitingCasesInfo())
        }
        get(CASE) {
            val id = call.parameters["id"] ?: error("Invalid case id.")
            val idLong = id.toLongOrNull() ?: error("Case id should be a long.") // todo test
            val viewableCase = try {
                application.viewableCase(idLong)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
            call.respond(viewableCase)
        }
    }
}