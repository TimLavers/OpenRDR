package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CHAT_CONTEXT
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.constants.api.START_CONVERSATION
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.kb.chat.ChatContext
import io.rippledown.log.lazyLogger
import io.rippledown.server.ServerApplication

fun Application.chatManagement(application: ServerApplication) {
    val logger = lazyLogger
    routing {
        post(path = START_CONVERSATION) {
            val userId = userId()
            val context = chatContext(application)
            logger.info("startConversation: user=$userId kbId=${call.parameters[KB_ID]} caseId=${call.parameters[CASE_ID]}")
            val response = application.chatCoordinator.startConversation(userId, context)
            call.respond(OK, response)
        }
        // A new window of a user who already has a conversation adopts its context
        // rather than opening a KB of its own, which would retarget the shared chat.
        get(path = CHAT_CONTEXT) {
            val userId = userId()
            if (application.chatCoordinator.hasConversation(userId)) {
                call.respond(OK, application.chatCoordinator.contextFor(userId).info())
            } else {
                call.respond(NoContent)
            }
        }
        post(path = SEND_USER_MESSAGE) {
            val userId = userId()
            val userMessage = call.receiveText()
            logger.info("sendUserMessage: user=$userId message='$userMessage'")
            val response = application.chatCoordinator.responseToUserMessage(userId, userMessage)
            call.respond(OK, response)
        }
    }
}

/**
 * The ids are optional: none means no knowledge base is open, a KB id alone
 * means the KB has no case to talk about.
 */
private fun RoutingContext.chatContext(application: ServerApplication): ChatContext {
    val kbId = call.parameters[KB_ID] ?: return ChatContext.NoKnowledgeBase
    val endpoint = application.kbForId(kbId)
    val caseId = call.parameters[CASE_ID]?.toLongOrNull() ?: return ChatContext.KnowledgeBaseOnly(endpoint)
    return ChatContext.CaseInKnowledgeBase(endpoint, endpoint.viewableCase(caseId))
}
