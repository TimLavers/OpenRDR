package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.INTERPRET_CASE
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
            val endpoint = kbEndpointByName(application)
            val case = endpoint.processCase(externalCase)
            call.respond(HttpStatusCode.Accepted, case)
            application.webSocketManager.sendCasesInfo(endpoint.waitingCasesInfo())
        }
    }
}