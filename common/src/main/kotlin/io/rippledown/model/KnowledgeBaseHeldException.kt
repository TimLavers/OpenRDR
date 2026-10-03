package io.rippledown.model

/**
 * The client's view of a request the server refused with 409 because another
 * user holds the knowledge base. The message is the server's sentence, which
 * names the KB and the holder. See documentation/design/concurrent_users.md.
 */
class KnowledgeBaseHeldException(message: String) : RuntimeException(message)
