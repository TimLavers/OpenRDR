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
            val kbId = kbIdOrNull()
            val caseId = caseIdOrNull()
            logger.info("Starting conversation... kbId: $kbId, caseId: $caseId")
            val response = application.startConversation(kbId, caseId)
            logger.info("Conversation started: $response")
            call.respond(OK, response)
        }
        post(path = SEND_USER_MESSAGE) {
            val userMessage = call.receiveText()
            val kdbId = kbIdOrNull()
            logger.info("Send user message: $userMessage, kbId: $kdbId")
            try {
                val reply = application.processUserRequest(userMessage, kdbId)
                logger.info("Response for user message: $reply")
                call.respond(OK, reply)
            } catch (e: Exception) {
                log.error("Could not process message: $e")
                call.respond(OK, "Could not process message: $e")
            }
        }
    }
}
