package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.DELETE_CASE_WITH_NAME
import io.rippledown.constants.api.INTERPRET_CASE
import io.rippledown.constants.api.PROCESS_CASE
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.log.lazyLogger
import io.rippledown.model.external.ExternalCase
import io.rippledown.server.ServerApplication
import kotlinx.serialization.json.Json

private val jsonAllowSMK = Json {
    allowStructuredMapKeys = true
}

fun Application.interpreter(application: ServerApplication) {
    val logger = lazyLogger
    routing {
        put(INTERPRET_CASE) {
            val str = call.receiveText()
            val externalCase = jsonAllowSMK.decodeFromString(ExternalCase.serializer(), str)
            val case = kbEndpoint(application).processCase(externalCase)
            call.respond(HttpStatusCode.Accepted, case)
        }
    }
}