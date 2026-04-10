package io.rippledown.kb

import io.rippledown.chat.Conversation
import io.rippledown.chat.Conversation.Companion.GET_SUGGESTED_CONDITIONS
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.chat.Conversation.Companion.SELECT_SUGGESTED_CONDITION
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.kb.chat.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class ChatSessionManager(
    private val ruleSessionManager: RuleSessionManager
) {
    private lateinit var chatManager: ChatManager

    /**
     * Starts a new conversation for the given viewable case.
     *
     * @param viewableCase The case to start a conversation about
     * @return A string representing the conversation ID or initial response
     */
    suspend fun startConversation(viewableCase: ViewableCase): ChatResponse {
        val chatService = KBChatService.createKBChatService(viewableCase)
        // Use a lazy ModelResponder since chatManager isn't created until after Conversation
        val modelResponder = object : ModelResponder {
            override suspend fun response(message: String): ChatResponse = chatManager.response(message)
        }
        val reasonTransformer = createReasonTransformer(viewableCase, ruleSessionManager, modelResponder)
        val suggestedConditionsHandler = SuggestedConditionsHandler(viewableCase.case, ruleSessionManager)
        val selectSuggestionHandler = SelectSuggestionHandler(viewableCase.case, ruleSessionManager)
        val functionCallHandlers: Map<String, FunctionCallHandler> = mapOf(
            TRANSFORM_REASON to ReasonTransformHandler(reasonTransformer),
            GET_SUGGESTED_CONDITIONS to suggestedConditionsHandler,
            SELECT_SUGGESTED_CONDITION to selectSuggestionHandler
        )
        val conversationService = Conversation(
            chatService = chatService,
            functionCallHandlers = functionCallHandlers
        )
        chatManager = ChatManager(conversationService, ruleSessionManager)
        return chatManager.startConversation(viewableCase)
    }

    /**
     * Creates a transformer that converts a natural language reason into a rule condition and
     * adds it to the current rule session if it is valid.
     */
    fun createReasonTransformer(viewableCase: ViewableCase, ruleService: RuleService, modelResponder: ModelResponder) =
        KBReasonTransformer(viewableCase.case, ruleService, modelResponder)

    suspend fun responseToUserMessage(message: String): ChatResponse = chatManager.response(message)
}
