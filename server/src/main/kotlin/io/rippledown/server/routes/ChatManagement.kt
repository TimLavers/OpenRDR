package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.constants.api.START_CONVERSATION
import io.rippledown.server.ServerApplication

fun Application.chatManagement(application: ServerApplication) {
    routing {
        post(path = START_CONVERSATION) {
            val kbEndpoint = kbEndpoint(application)
            val response = kbEndpoint.startConversation(caseId())
            call.respond(OK, response)
        }
        post(path = SEND_USER_MESSAGE) {
            val kbEndpoint = kbEndpoint(application)
            val userMessage = call.receiveText()
            val response = kbEndpoint.responseToUserMessage(userMessage, caseId())
            call.respond(OK, response)
        }
    }
}
