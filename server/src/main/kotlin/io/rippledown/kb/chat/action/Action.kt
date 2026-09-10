package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse

/**
 * Anything the model can ask the server to do. [ChatAction] works on the open
 * knowledge base and its current case; [KbManagementAction] works on the set of
 * knowledge bases. See documentation/design/kb_management_by_chat.md.
 */
sealed interface Action

sealed class KbManagementOutcome {
    data class Done(val response: ChatResponse) : KbManagementOutcome()

    /**
     * The action wants the user's say-so first. [question] goes to the user and
     * [thenDo] is held by the chat manager for one turn; a plain acceptance runs
     * it without consulting the model. A lambda rather than an action class so
     * that there is nothing the model could name to skip the question.
     */
    class Ask(val question: String, val thenDo: suspend (KnowledgeBaseService) -> ChatResponse) : KbManagementOutcome()
}

interface KbManagementAction : Action {
    /**
     * Whether carrying out the action changes what the chat is about, in which
     * case it is refused while a rule is being built.
     */
    val changesContext: Boolean get() = true

    suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome
}

fun done(text: String) = KbManagementOutcome.Done(ChatResponse(text))
