package io.rippledown.kb.chat

import io.rippledown.model.UserId
import io.rippledown.server.KBEndpoint

/**
 * Which knowledge base each user has open. A user's open KB is the one their
 * chat is about, so the chat coordinator is the authority. The knowledge base
 * service tells it when a KB is closed or deleted so that no message is
 * answered against a knowledge base that is gone.
 */
interface OpenKnowledgeBases {
    fun openEndpointFor(userId: UserId): KBEndpoint?

    fun knowledgeBaseClosed(userId: UserId)

    /** Resets every conversation on the KB and returns the users affected. */
    fun knowledgeBaseDeleted(kbId: String): Set<UserId>
}
