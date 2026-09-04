package io.rippledown.kb.chat

import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.KBEndpoint

/**
 * What the chat is about. The context decides the system prompt, the
 * functions the model may call, the actions it may emit and the greeting.
 * See documentation/design/kb_management_by_chat.md.
 */
sealed class ChatContext {
    object NoKnowledgeBase : ChatContext()
    data class KnowledgeBaseOnly(val endpoint: KBEndpoint) : ChatContext()
    data class CaseInKnowledgeBase(val endpoint: KBEndpoint, val viewableCase: ViewableCase) : ChatContext()

    val endpointOrNull: KBEndpoint?
        get() = when (this) {
            is NoKnowledgeBase -> null
            is KnowledgeBaseOnly -> endpoint
            is CaseInKnowledgeBase -> endpoint
        }

    val kbInfoOrNull: KBInfo? get() = endpointOrNull?.kbInfo()

    val caseOrNull: ViewableCase? get() = (this as? CaseInKnowledgeBase)?.viewableCase
}
