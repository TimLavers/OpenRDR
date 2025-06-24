package io.rippledown.kb.chat

import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import io.rippledown.chat.ChatService
import io.rippledown.chat.Conversation.Companion.EXPRESSION_PARAMETER
import io.rippledown.chat.Conversation.Companion.IS_EXPRESSION_VALID
import io.rippledown.chat.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.ViewableCase
import io.rippledown.toJsonString

object KBChatService {
    private val logger = lazyLogger

    // Cache for markdown file contents
    private val fileCache = mutableMapOf<String, String>()

    // Placeholder value providers
    private sealed class PlaceholderValue {
        abstract fun getValue(case: ViewableCase?): String
    }

    private class CaseDependentValue(private val provider: (ViewableCase) -> String) : PlaceholderValue() {
        override fun getValue(case: ViewableCase?): String =
            provider(case ?: throw IllegalArgumentException("Case required"))
    }

    private class ConstantValue(private val provider: () -> String) : PlaceholderValue() {
        override fun getValue(case: ViewableCase?): String = provider()
    }

    private val placeholders = mapOf(
        "{{case_json}}" to CaseDependentValue { case -> case.toJsonString() },
        "{{objective}}" to ConstantValue { readResource("objective.md") },
        "{{initial-instructions}}" to ConstantValue { readResource("initial-instructions.md") },
        "{{adding-a-comment}}" to ConstantValue { readResource("adding-a-comment.md") },
        "{{removing-a-comment}}" to ConstantValue { readResource("removing-a-comment.md") },
        "{{replacing-a-comment}}" to ConstantValue { readResource("replacing-a-comment.md") },
        "{{providing-conditions}}" to ConstantValue { readResource("providing-conditions.md") },
        "{{validating-a-condition}}" to ConstantValue { readResource("validating-a-condition.md") },
        "{{formatting-rules}}" to ConstantValue { readResource("formatting-rules.md") },
        "{{general-guidelines}}" to ConstantValue { readResource("general-guidelines.md") },
        "{{IS_EXPRESSION_VALID}}" to ConstantValue { IS_EXPRESSION_VALID },
        "{{PLEASE_CONFIRM}}" to ConstantValue { PLEASE_CONFIRM },
        "{{ADD}}" to ConstantValue { ADD },
        "{{REMOVE}}" to ConstantValue { REMOVE },
        "{{REPLACE}}" to ConstantValue { REPLACE },
        "{{STOP}}" to ConstantValue { STOP },
        "{{NO_COMMENTS}}" to ConstantValue { NO_COMMENTS },
        "{{EXISTING_COMMENTS}}" to ConstantValue { EXISTING_COMMENTS },
        "{{WOULD_YOU_LIKE}}" to ConstantValue { WOULD_YOU_LIKE },
        "{{ADD_A_COMMENT}}" to ConstantValue { ADD_A_COMMENT },
        "{{REMOVE_A_COMMENT}}" to ConstantValue { REMOVE_A_COMMENT },
        "{{REPLACE_A_COMMENT}}" to ConstantValue { REPLACE_A_COMMENT },
        "{{WHAT_COMMENT}}" to ConstantValue { WHAT_COMMENT },
        "{{ANY_CONDITIONS}}" to ConstantValue { ANY_CONDITIONS },
        "{{ANY_MORE_CONDITIONS}}" to ConstantValue { ANY_MORE_CONDITIONS },
        "{{FIRST_CONDITION}}" to ConstantValue { FIRST_CONDITION },
        "{{START_ACTION}}" to ConstantValue { START_ACTION },
        "{{STOP_ACTION}}" to ConstantValue { STOP_ACTION },
        "{{DEBUG_ACTION}}" to ConstantValue { DEBUG_ACTION },
        "{{USER_ACTION}}" to ConstantValue { USER_ACTION },
        "{{ADD_ACTION}}" to ConstantValue { ADD_ACTION },
        "{{REMOVE_ACTION}}" to ConstantValue { REMOVE_ACTION },
        "{{REPLACE_ACTION}}" to ConstantValue { REPLACE_ACTION }
    )

    private fun readResource(resource: String): String {
        return fileCache.computeIfAbsent(resource) {
            val text = KBChatService::class.java.getResource("/chat/instructions/$resource")?.readText()
                ?: throw IllegalStateException("Resource '$resource' not found")
            text.trimIndent()
        }
    }

    private fun String.replacePlaceholders(case: ViewableCase?): String {
        return placeholders.entries.fold(this) { text, (placeholder, valueProvider) ->
            try {
                text.replace(placeholder, valueProvider.getValue(case))
            } catch (e: Exception) {
                logger.warn("Failed to replace placeholder $placeholder", e)
                text // Fallback to original text with placeholder intact
            }
        }
    }

    private val expressionCheck = FunctionDeclaration(
        name = IS_EXPRESSION_VALID,
        description = "Check if the user-entered expression represents a valid condition",
        parameters = listOf(
            Schema.str(
                name = EXPRESSION_PARAMETER,
                description = "The expression to check for validity"
            )
        ),
        requiredParameters = listOf(EXPRESSION_PARAMETER)
    )

    fun createKBChatService(case: ViewableCase): ChatService {
        val systemInstruction = systemInstruction(case)
        logger.info("system instruction:\n$systemInstruction")
        return GeminiChatService(
            systemInstruction = systemInstruction,
            functionDeclarations = listOf(expressionCheck)
        )
    }

    fun systemInstruction(case: ViewableCase): String {
        return readResource("system.md").replacePlaceholders(case)
    }
}