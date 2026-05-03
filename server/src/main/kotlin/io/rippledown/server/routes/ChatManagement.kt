package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.constants.api.START_CONVERSATION
import io.rippledown.log.lazyLogger
import io.rippledown.server.ServerApplication

fun Application.chatManagement(application: ServerApplication) {
    val logger = lazyLogger
    routing {
        post(path = START_CONVERSATION) {
            val kbEndpoint = kbEndpoint(application)
            val cid = caseId()
            logger.info("startConversation: kb='${kbEndpoint.kbInfo().name}' caseId=$cid")
            val response = kbEndpoint.startConversation(cid)
            call.respond(OK, response)
        }
        post(path = SEND_USER_MESSAGE) {
            val kbEndpoint = kbEndpoint(application)
            val userMessage = call.receiveText()
            logger.info("sendUserMessage: kb='${kbEndpoint.kbInfo().name}' message='$userMessage'")
            val response = kbEndpoint.responseToUserMessage(userMessage)
            call.respond(OK, response)
        }
    }
}
