package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.DELETE_CASE_WITH_NAME
import io.rippledown.constants.api.PROCESS_CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.model.external.ExternalCase
import io.rippledown.server.ServerApplication
import io.rippledown.server.logger
import kotlinx.serialization.json.Json

private val jsonAllowSMK = Json {
    allowStructuredMapKeys = true
}

fun Application.caseManagement(application: ServerApplication) {
    routing {
        get(WAITING_CASES) {
            call.respond(kbEndpoint(application).waitingCasesInfo())
        }
        get(CASE) {
            logger.info("Getting case...")
            val viewableCase = try {
                val kbEndpoint = kbEndpoint(application)
                val caseId = caseId()
                kbEndpoint.viewableCase(caseId)
            } catch (e: Exception) {
                call.respond(BadRequest, e.message.toString())
                return@get
            }
            logger.info("viewable case retrieved")
            call.respond(viewableCase)
            logger.info("viewable case written")

        }
        put(PROCESS_CASE) {
            val str = call.receiveText()
            val externalCase = jsonAllowSMK.decodeFromString(ExternalCase.serializer(), str)
            val case = kbEndpoint(application).processCase(externalCase)
            call.respond(HttpStatusCode.Accepted, case)
        }
        delete(DELETE_CASE_WITH_NAME) {
            val caseName = call.parameters["name"] ?: error("Invalid case name.")
            kbEndpoint(application).deleteCase(caseName)
            call.respond(HttpStatusCode.OK)
        }
    }
}