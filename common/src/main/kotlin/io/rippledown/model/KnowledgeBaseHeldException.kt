package io.rippledown.model

/**
 * The client's view of a request the server refused with 409 because another
 * user is editing the knowledge base. The message is the server's sentence, which
 * names the KB and the editor. See documentation/design/concurrent_users.md.
 */
class KnowledgeBaseHeldException(override val message: String) : RuntimeException(message)
