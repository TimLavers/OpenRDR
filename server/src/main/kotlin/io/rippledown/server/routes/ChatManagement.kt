package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.server.ServerApplication

fun Application.chatManagement(application: ServerApplication) {
    routing {
        post(SEND_USER_MESSAGE) {
            val userMessage = call.receiveText()
            val caseId = caseId()
            val response = kbEndpoint(application).botResponseToUserMessage(userMessage, caseId)
            call.respond(OK, response)
        }
    }
}
