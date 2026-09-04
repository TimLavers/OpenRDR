package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
            val context = chatContext(application)
            logger.info("startConversation: kbId=${call.parameters[KB_ID]} caseId=${call.parameters[CASE_ID]}")
            val response = application.chatCoordinator.startConversation(context)
            call.respond(OK, response)
        }
        post(path = SEND_USER_MESSAGE) {
            val userMessage = call.receiveText()
            logger.info("sendUserMessage: message='$userMessage'")
            val response = application.chatCoordinator.responseToUserMessage(userMessage)
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
