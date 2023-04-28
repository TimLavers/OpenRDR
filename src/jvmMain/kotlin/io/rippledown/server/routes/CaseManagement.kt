package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.*
import io.rippledown.model.Attribute
import io.rippledown.model.Interpretation
import io.rippledown.model.OperationResult
import io.rippledown.server.ServerApplication

fun Application.caseManagement(application: ServerApplication) {
    routing {
        get(WAITING_CASES) {
            call.respond(application.waitingCasesInfo())
        }
        get(CASE) {
            val id = call.parameters["id"] ?: error("Invalid case id.")
            val viewableCase = try {
                application.viewableCase(id)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
            call.respond(viewableCase)
        }
        post(MOVE_ATTRIBUTE_JUST_BELOW_OTHER) {
            val attributePair = call.receive<Pair<Attribute, Attribute>>()
            application.moveAttributeJustBelow(attributePair.first, attributePair.second)
            call.respond(HttpStatusCode.OK, OperationResult("Attribute moved"))
        }
        post(INTERPRETATION_SUBMITTED) {
            val interpretation = call.receive<Interpretation>()
            val result = application.saveInterpretationAndDeleteCase(interpretation)
            call.respond(HttpStatusCode.OK, result)
        }
        post(VERIFIED_INTERPRETATION_SAVED) {
            val interpretation = call.receive<Interpretation>()
            application.saveInterpretation(interpretation)
            call.respond(HttpStatusCode.OK)
        }
    }
}