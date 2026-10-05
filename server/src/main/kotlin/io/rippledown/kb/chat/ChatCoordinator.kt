package io.rippledown.kb.chat

import io.rippledown.constants.chat.emptyKbGreeting
import io.rippledown.constants.chat.noKbGreeting
import io.rippledown.log.lazyLogger
import io.rippledown.model.UserId
import io.rippledown.model.chat.ChatResponse
import io.rippledown.server.KBEndpoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns one conversation per user. The client starts a conversation whenever
 * its context changes (a KB is opened or closed, a case is selected); the
 * server never starts one on its own. Conversations are never evicted;
 * see documentation/design/concurrent_users.md.
 */
class ChatCoordinator(
    private val factory: ChatManagerFactory,
    private val kbServiceFor: (UserId) -> KnowledgeBaseService,
) : OpenKnowledgeBases {
    private val logger = lazyLogger

    private class Conversation {
        var chatManager: ChatManager? = null
        var context: ChatContext = ChatContext.NoKnowledgeBase

        // A user message typed while the client is still starting the conversation for
        // a new context must wait for the opening turn, or the two turns would run
        // concurrently against the same model chat.
        val oneTurnAtATime = Mutex()

        fun reset() {
            context = ChatContext.NoKnowledgeBase
            chatManager = null
        }
    }

    private val conversations = ConcurrentHashMap<UserId, Conversation>()

    private fun conversationFor(userId: UserId) = conversations.computeIfAbsent(userId) { Conversation() }

    fun contextFor(userId: UserId): ChatContext = conversations[userId]?.context ?: ChatContext.NoKnowledgeBase

    override fun openEndpointFor(userId: UserId): KBEndpoint? = contextFor(userId).endpointOrNull

    fun usersOn(kbId: String): Set<UserId> = conversationsOn(kbId).keys

    private fun conversationsOn(kbId: String) = conversations.filterValues { it.context.kbInfoOrNull?.id == kbId }

    suspend fun startConversation(userId: UserId, context: ChatContext): ChatResponse {
        val conversation = conversationFor(userId)
        return conversation.oneTurnAtATime.withLock {
            conversation.context = context
            logger.info("Starting conversation for user '$userId' in context ${context::class.simpleName}")
            val manager = factory.create(userId, context)
            conversation.chatManager = manager
            manager.startConversation(context.caseOrNull, greetingFor(userId, context))
        }
    }

    suspend fun responseToUserMessage(userId: UserId, message: String): ChatResponse {
        val conversation = conversationFor(userId)
        return conversation.oneTurnAtATime.withLock {
            val manager = conversation.chatManager
            if (manager == null) {
                logger.warn("responseToUserMessage for user '$userId' before startConversation; message='$message'")
                return ChatResponse(NO_CONVERSATION_MESSAGE)
            }
            manager.response(message)
        }
    }

    /**
     * Called from within the user's own turn, when the action being run has
     * closed the open knowledge base. Until the client starts the next
     * conversation, a message must not be answered against a knowledge base
     * that is no longer open.
     */
    override fun knowledgeBaseClosed(userId: UserId) {
        conversations[userId]?.reset()
    }

    /**
     * Unlike closing, deleting affects every user on the KB, not just the
     * caller, who need not even have had it open.
     */
    override fun knowledgeBaseDeleted(kbId: String): Set<UserId> {
        val affected = conversationsOn(kbId)
        affected.values.forEach { it.reset() }
        return affected.keys
    }

    private fun greetingFor(userId: UserId, context: ChatContext): String? = when (context) {
        is ChatContext.NoKnowledgeBase -> {
            val kbService = kbServiceFor(userId)
            noKbGreeting(
                kbService.knowledgeBases().map { it.name },
                kbService.demonstrations().map { it.title() }
            )
        }
        is ChatContext.KnowledgeBaseOnly -> emptyKbGreeting(context.endpoint.kbInfo().name)
        is ChatContext.CaseInKnowledgeBase -> null
    }

    companion object {
        const val NO_CONVERSATION_MESSAGE =
            "No conversation has been started. Please open a knowledge base or select a case."
    }
}
