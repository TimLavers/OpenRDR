package io.rippledown.kb.chat

import io.rippledown.chat.ConversationService
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.constants.chat.AI_UNAVAILABLE_MESSAGE
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.SYSTEM_ERROR_PREFIX
import io.rippledown.extractJsonFragments
import io.rippledown.fromJsonString
import io.rippledown.kb.chat.action.ChatAction
import io.rippledown.kb.chat.action.KbManagementAction
import io.rippledown.kb.chat.action.UserAction
import io.rippledown.log.lazyLogger
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

interface ModelResponder {
    suspend fun response(message: String): ChatResponse
}

class ChatManager(
    val conversationService: ConversationService,
    val ruleService: RuleService?,
    kbService: KnowledgeBaseService,
    suggestionsBuffer: SuggestionsBuffer = SuggestionsBuffer(),
    suggestedConditionsHandler: FunctionCallHandler? = null,
) : ModelResponder {
    private val logger = lazyLogger
    private var currentCase: ViewableCase? = null
    private val knowledgeBases = KnowledgeBaseConversation(
        kbService, KbCreationReplyInterpreter(conversationService), ruleService
    )
    private val rules = RuleConversation(ruleService)
    private val responses = ChatResponseEnricher(ruleService, suggestionsBuffer, suggestedConditionsHandler)

    suspend fun startConversation(viewableCase: ViewableCase?, greeting: String? = null): ChatResponse {
        currentCase = viewableCase
        knowledgeBases.reset()
        rules.reset()
        responses.reset()
        val response = try {
            conversationService.startConversation()
        } catch (e: Exception) {
            logger.error("Failed to start conversation", e)
            return ChatResponse(AI_UNAVAILABLE_MESSAGE)
        }
        if (greeting != null) return knowledgeBases.greet(viewableCase, greeting)
        logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$response'")
        return dispatchModelResponse(response, opening = true)
    }

    override suspend fun response(message: String): ChatResponse {
        logger.info("$LOG_PREFIX_FOR_USER_MESSAGE '$message'")
        rules.cornerstoneAction(message)?.let { return executeAction(it) }
        knowledgeBases.answer(message)?.let { return it }
        rules.assignmentAction(message)?.let { return processActionComment(it) }
        val turn = rules.prepareTurn(message)
        val response = try {
            conversationService.response(turn.message)
        } catch (e: Exception) {
            logger.error("Failed to send message: $message", e)
            return ChatResponse(AI_UNAVAILABLE_MESSAGE)
        }
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $response")
        rules.completeTurn(turn)?.let { return processActionComment(it) }
        return dispatchModelResponse(response)
    }

    suspend fun processActionComment(actionComment: ActionComment): ChatResponse {
        val response = executeAction(actionComment)
        rules.rememberOffer(actionComment)
        return responses.enrich(actionComment, response, currentCase)
    }

    private suspend fun executeAction(actionComment: ActionComment): ChatResponse =
        when (val action = actionComment.createActionInstance()) {
            null -> {
                logger.error("Unknown actionComment: ${actionComment.action}")
                ChatResponse("")
            }
            is UserAction -> ChatResponse(action.message)
            is KbManagementAction -> knowledgeBases.execute(action)
            is ChatAction ->
                if (ruleService == null) ChatResponse(NO_KB_OPEN_MESSAGE)
                else action.doIt(ruleService, currentCase, this)
        }

    private suspend fun dispatchModelResponse(response: String, opening: Boolean = false): ChatResponse = try {
        val json = extractJsonFragments(response).firstOrNull()
        if (json == null) ChatResponse(response)
        else processActionComment(json.sanitizeLlmJson().fromJsonString<ActionComment>())
    } catch (e: Exception) {
        val context = if (opening) "start-conversation ActionComment" else "ActionComment"
        logger.error("Failed to process $context: $response", e)
        ChatResponse(if (opening) response else "$SYSTEM_ERROR_PREFIX: '$response'")
    }

    companion object {
        const val LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE = "Start conversation response:"
        const val LOG_PREFIX_FOR_CONVERSATION_RESPONSE = "Conversation response:"
        const val LOG_PREFIX_FOR_USER_MESSAGE = "User message:"
    }
}

fun String.sanitizeLlmJson() = replace("\\'", "'").replace("\\\\n", "\\n")
