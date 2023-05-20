package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CONDITION_HINTS
import io.rippledown.server.ServerApplication

fun Application.conditionManagement(application: ServerApplication) {
    routing {
        get(CONDITION_HINTS) {
            val id = call.parameters["id"] ?: error("Invalid case id.")
            val conditionHints = try {
                application.conditionHintsForCase(id)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
            call.respond(conditionHints)
        }
    }
}