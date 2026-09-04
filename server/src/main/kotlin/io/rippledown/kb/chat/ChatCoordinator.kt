package io.rippledown.kb.chat

import io.rippledown.constants.chat.emptyKbGreeting
import io.rippledown.constants.chat.noKbGreeting
import io.rippledown.log.lazyLogger
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the one conversation the application has. The client starts a
 * conversation whenever its context changes (a KB is opened or closed, a case
 * is selected); the server never starts one on its own.
 */
class ChatCoordinator(
    private val factory: ChatManagerFactory,
    private val kbService: KnowledgeBaseService,
) {
    private val logger = lazyLogger
    private var chatManager: ChatManager? = null
    private var context: ChatContext = ChatContext.NoKnowledgeBase

    // A user message typed while the client is still starting the conversation for
    // a new context must wait for the opening turn, or the two turns would run
    // concurrently against the same model chat.
    private val oneTurnAtATime = Mutex()

    fun context() = context

    suspend fun startConversation(context: ChatContext): ChatResponse = oneTurnAtATime.withLock {
        this.context = context
        logger.info("Starting conversation in context ${context::class.simpleName}")
        val manager = factory.create(context)
        chatManager = manager
        manager.startConversation(context.caseOrNull, greetingFor(context))
    }

    suspend fun responseToUserMessage(message: String): ChatResponse = oneTurnAtATime.withLock {
        val manager = chatManager
        if (manager == null) {
            logger.warn("responseToUserMessage called before startConversation; message='$message'")
            return ChatResponse(NO_CONVERSATION_MESSAGE)
        }
        manager.response(message)
    }

    private fun greetingFor(context: ChatContext): String? = when (context) {
        is ChatContext.NoKnowledgeBase -> noKbGreeting(kbService.knowledgeBases().map { it.name })
        is ChatContext.KnowledgeBaseOnly -> emptyKbGreeting(context.endpoint.kbInfo().name)
        is ChatContext.CaseInKnowledgeBase -> null
    }

    companion object {
        const val NO_CONVERSATION_MESSAGE =
            "No conversation has been started. Please open a knowledge base or select a case."
    }
}
