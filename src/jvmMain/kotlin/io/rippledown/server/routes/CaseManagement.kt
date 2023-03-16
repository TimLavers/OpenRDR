package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.model.Interpretation
import io.rippledown.server.ServerApplication

const val WAITING_CASES = "/api/waitingCasesInfo"
const val CASE = "/api/case"
const val INTERPRETATION_SUBMITTED = "/api/interpretationSubmitted"

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
        post(INTERPRETATION_SUBMITTED) {
            val interpretation = call.receive<Interpretation>()
            val result = application.saveInterpretation(interpretation)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}