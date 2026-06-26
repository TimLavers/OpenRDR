package io.rippledown.kb.chat

import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.ADD_COMMENT
import io.rippledown.constants.chat.COMMENT_VARIABLE_TIP_KEYWORD
import io.rippledown.extractJsonFragments
import io.rippledown.fromJsonString
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.log.lazyLogger
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

interface ModelResponder {
    suspend fun response(message: String): ChatResponse
}

/**
 * Manages the chat conversation with the user, processing messages and actions based on the AI model's responses.
 *
 * @author Cascade AI
 */
class ChatManager(
    val conversationService: ConversationService,
    val ruleService: RuleService,
    private val suggestionsBuffer: SuggestionsBuffer = SuggestionsBuffer(),
) : ModelResponder {
    private val logger = lazyLogger
    private var currentCase: ViewableCase? = null

    // The once-per-session tip telling the user they can embed case values in a comment using braces.
    // Reset implicitly because a new ChatManager is created for each conversation (i.e. each case selection).
    private var commentVariableTipShown = false

    suspend fun startConversation(viewableCase: ViewableCase): ChatResponse {
        currentCase = viewableCase
        val response = try {
            conversationService.startConversation()
        } catch (e: Exception) {
            logger.error("Failed to start conversation", e)
            return ChatResponse(AI_UNAVAILABLE_MESSAGE)
        }
        logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$response'")
        // When the case already has comments the model replies in prose
        // (e.g. "This case has the following comments: ... Would you
        // like to add another one, or replace or remove one of them?")
        // instead of emitting a JSON ActionComment. Mirror the robustness
        // of `processConversationResponse` here: extract any JSON
        // fragments and, if there are none, surface the raw text as a
        // plain bot message rather than 500ing.
        return try {
            val jsonFragments = extractJsonFragments(response)
            if (jsonFragments.isEmpty()) {
                ChatResponse(response)
            } else {
                processActionComment(jsonFragments.first().sanitizeLlmJson().fromJsonString<ActionComment>())
            }
        } catch (e: Exception) {
            logger.error("Failed to process start-conversation ActionComment: $response", e)
            ChatResponse(response)
        }
    }

    override suspend fun response(message: String): ChatResponse {
        return processConversationResponse(message)
    }

    private suspend fun processConversationResponse(message: String): ChatResponse {
        logger.info("$LOG_PREFIX_FOR_USER_MESSAGE '$message'")
        val messageToSend = augmentWithCornerstoneStatus(message)
        val response = try {
            conversationService.response(messageToSend)
        } catch (e: Exception) {
            logger.error("Failed to send message: $message", e)
            return ChatResponse(AI_UNAVAILABLE_MESSAGE)
        }
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $response")
        try {
            // Extract the first JSON object from the response (the model may sometimes
            // return multiple JSON objects, but only the first should be processed since
            // actions like ExemptCornerstone handle continuations via recursive calls)
            val jsonFragments = extractJsonFragments(response)
            if (jsonFragments.isEmpty()) {
                // Mirror startConversation: when the model replies in prose
                // rather than JSON (e.g. an off-script clarifying question),
                // surface the raw text as a plain bot message rather than
                // returning an empty response that leaves the chat panel
                // silent. An empty ChatResponse here previously caused
                // cucumber scenarios such as "The comments given for a case
                // are returned by the interpretation service" to hang for
                // 60s waiting for suggestions that never arrived.
                return ChatResponse(response)
            }
            return processActionComment(jsonFragments.first().sanitizeLlmJson().fromJsonString<ActionComment>())
        } catch (e: Exception) {
            logger.error("Failed to process ActionComment: $response", e)
            return ChatResponse("System error. See server.log: '$response'")
        }
    }

    //Either pass on the model's response to the user or take some action
    suspend fun processActionComment(actionComment: ActionComment): ChatResponse {
        val chatAction = actionComment.createActionInstance()
        val chatResponse = if (chatAction != null) {
            chatAction.doIt(ruleService, currentCase, this)
        } else {
            logger.error("Unknown actionComment: ${actionComment.action}")
            ChatResponse("")
        }
        val tip = commentVariableTipFor(actionComment, chatResponse)
        val bufferedSuggestions = suggestionsBuffer.consume()
        return when {
            bufferedSuggestions != null -> ChatResponse(chatResponse.text, bufferedSuggestions, tip)
            !actionComment.suggestions.isNullOrEmpty() -> ChatResponse(
                chatResponse.text,
                actionComment.suggestions,
                tip
            )

            else -> chatResponse.copy(tip = tip ?: chatResponse.tip)
        }
    }

    /**
     * The first time the user adds a comment in a session, return a short one-line tip explaining that a
     * comment can include a case value by wrapping an attribute name in braces (e.g. {Glucose}). The tip
     * is delivered on the [ChatResponse.tip] channel so the UI can render it distinctly. It is shown at
     * most once per session and is suppressed when the user has already used the facility (i.e. the comment
     * already contains a placeholder) or when the add was rejected because a rule session was already active.
     */
    private fun commentVariableTipFor(actionComment: ActionComment, chatResponse: ChatResponse): String? {
        if (commentVariableTipShown) return null
        if (actionComment.action != ADD_COMMENT) return null
        if (chatResponse.text == RULE_SESSION_ALREADY_ACTIVE_ERROR) return null
        val comment = actionComment.comment ?: return null
        if (comment.contains("{")) return null // the user has already used the facility
        commentVariableTipShown = true
        // Use the first attribute of the displayed case as the example, falling back to a generic name
        // if the case has no attributes, so the tip is concrete and relevant to what the user is seeing.
        val exampleAttribute = currentCase?.attributes()?.firstOrNull()?.name ?: DEFAULT_TIP_EXAMPLE_ATTRIBUTE
        return commentVariableTip(exampleAttribute)
    }

    private fun augmentWithCornerstoneStatus(message: String): String {
        if (!ruleService.isRuleSessionActive()) return message
        val status = ruleService.cornerstoneStatus()
        return "$CURRENT_CORNERSTONE_STATUS_PREFIX${status.summary()}]\n$message"
    }

    companion object {
        const val LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE = "Start conversation response:"
        const val LOG_PREFIX_FOR_CONVERSATION_RESPONSE = "Conversation response:"
        const val LOG_PREFIX_FOR_USER_MESSAGE = "User message:"
        const val AI_UNAVAILABLE_MESSAGE = "The AI assistant is temporarily unavailable. Please try again later."
        const val CURRENT_CORNERSTONE_STATUS_PREFIX = "[Current cornerstone status: "
        const val DEFAULT_TIP_EXAMPLE_ATTRIBUTE = "TSH"
        fun commentVariableTip(exampleAttributeName: String) =
            "Tip: you can include a case value in a comment by wrapping an attribute name in " +
                    "$COMMENT_VARIABLE_TIP_KEYWORD, e.g. {$exampleAttributeName}."
    }
}

fun String.sanitizeLlmJson() = replace("\\'", "'").replace("\\\\n", "\\n")
