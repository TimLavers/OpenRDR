package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.DELETE_PROCESSED_CASE_WITH_NAME
import io.rippledown.constants.api.PROCESS_CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.model.CaseName
import io.rippledown.model.external.ExternalCase
import io.rippledown.server.ServerApplication
import kotlinx.serialization.json.Json

private val jsonAllowSMK = Json {
    allowStructuredMapKeys = true
}

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
        put(PROCESS_CASE) {
            val str = call.receiveText()
            val externalCase = jsonAllowSMK.decodeFromString(ExternalCase.serializer(), str)
            val case = application.processCase(externalCase)
            call.respond(HttpStatusCode.Accepted, case)
        }
        delete(DELETE_PROCESSED_CASE_WITH_NAME) {
            val str = call.receiveText()
            val caseName = jsonAllowSMK.decodeFromString(CaseName.serializer(), str)
            application.deleteProcessedCase(caseName.name)
            call.respond(HttpStatusCode.OK)
        }
    }
}