package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.PROVIDE_CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.model.OperationResult
import io.rippledown.model.condition.Condition
import io.rippledown.model.external.ExternalCase
import io.rippledown.server.ServerApplication
import kotlinx.serialization.json.Json

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
        put(PROVIDE_CASE) {
            val str = call.receiveText()
            val externalCase = Json.decodeFromString(ExternalCase.serializer(), str)
            val case = application.supplyCase(externalCase)
            call.respond(case)
        }
    }
}