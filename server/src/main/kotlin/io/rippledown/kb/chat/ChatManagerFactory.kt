package io.rippledown.kb.chat

import io.rippledown.chat.Conversation
import io.rippledown.chat.Conversation.Companion.GET_SUGGESTED_CONDITIONS
import io.rippledown.chat.Conversation.Companion.SELECT_SUGGESTED_CONDITION
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.kb.RuleSessionManager
import io.rippledown.model.caseview.ViewableCase

/**
 * Builds the [ChatManager] for a [ChatContext]. With a case the manager has the
 * rule service, the reason transformer and the suggestion handlers; without one
 * it has none of them, and the model can only talk and manage knowledge bases.
 */
class ChatManagerFactory(private val kbService: KnowledgeBaseService) {

    fun create(context: ChatContext): ChatManager {
        val kbNames = kbService.knowledgeBases().map { it.name }
        val kbName = context.kbInfoOrNull?.name
        return when (context) {
            is ChatContext.CaseInKnowledgeBase ->
                forCase(context.viewableCase, context.endpoint.session.ruleSessionManager, kbName, kbNames)

            else -> caseLess(kbName, kbNames)
        }
    }

    private fun caseLess(kbName: String?, kbNames: List<String>): ChatManager {
        val chatService = KBChatService.createKBChatService(null, kbName, kbNames)
        val conversation = Conversation(chatService, emptyMap(), openingMessage = null)
        return ChatManager(conversation, null, kbService)
    }

    private fun forCase(
        viewableCase: ViewableCase,
        ruleSessionManager: RuleSessionManager,
        kbName: String?,
        kbNames: List<String>
    ): ChatManager {
        val chatService = KBChatService.createKBChatService(
            viewableCase,
            kbName,
            kbNames,
            ruleSessionManager::attributeById,
            ruleSessionManager.allAttributes()
        )
        // The reason transformer needs the chat manager, which is created after the conversation.
        lateinit var chatManager: ChatManager
        val modelResponder = object : ModelResponder {
            override suspend fun response(message: String) = chatManager.response(message)
        }
        val reasonTransformer = createReasonTransformer(viewableCase, ruleSessionManager, modelResponder)
        val suggestionsBuffer = SuggestionsBuffer()
        val suggestedConditionsHandler =
            SuggestedConditionsHandler(viewableCase.case, ruleSessionManager, suggestionsBuffer)
        val selectSuggestionHandler =
            SelectSuggestionHandler(viewableCase.case, ruleSessionManager, suggestionsBuffer)
        val functionCallHandlers: Map<String, FunctionCallHandler> = mapOf(
            TRANSFORM_REASON to ReasonTransformHandler(reasonTransformer, ruleSessionManager),
            GET_SUGGESTED_CONDITIONS to suggestedConditionsHandler,
            SELECT_SUGGESTED_CONDITION to selectSuggestionHandler
        )
        val conversation = Conversation(chatService, functionCallHandlers)
        chatManager =
            ChatManager(conversation, ruleSessionManager, kbService, suggestionsBuffer, suggestedConditionsHandler)
        return chatManager
    }

    companion object {
        fun createReasonTransformer(
            viewableCase: ViewableCase,
            ruleService: RuleService,
            modelResponder: ModelResponder
        ) = KBReasonTransformer(viewableCase.case, ruleService, modelResponder)
    }
}
