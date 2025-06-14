package io.rippledown.kb.chat

import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import io.rippledown.chat.ChatService
import io.rippledown.chat.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString

private const val systemInstructionResource = "/chat/system-instruction.md"

object KBChatService {
    private val logger = lazyLogger

    private val genericSystemInstruction: String =
        this::class.java.getResource(systemInstructionResource)?.readText()
            ?: throw IllegalStateException("System instruction file '${systemInstructionResource}' not found")

    private val IS_EXPRESSION_VALID = "isExpressionValid"

    private val placeholders = mapOf(
        "{{CASE_JSON}}" to { case: RDRCase -> case.toJsonString() },
        "{{IS_EXPRESSION_VALID}}" to { IS_EXPRESSION_VALID },
        "{{PLEASE_CONFIRM}}" to { PLEASE_CONFIRM },
        "{{ADD}}" to { ADD },
        "{{REMOVE}}" to { REMOVE },
        "{{REPLACE}}" to { REPLACE },
        "{{NO_COMMENTS}}" to { NO_COMMENTS },
        "{{EXISTING_COMMENTS}}" to { EXISTING_COMMENTS },
        "{{WOULD_YOU_LIKE}}" to { WOULD_YOU_LIKE },
        "{{ADD_A_COMMENT}}" to { ADD_A_COMMENT },
        "{{REMOVE_A_COMMENT}}" to { REMOVE_A_COMMENT },
        "{{REPLACE_A_COMMENT}}" to { REPLACE_A_COMMENT },
        "{{WHAT_COMMENT}}" to { WHAT_COMMENT },
        "{{ANY_CONDITIONS}}" to { ANY_CONDITIONS },
        "{{ANY_MORE_CONDITIONS}}" to { ANY_MORE_CONDITIONS },
        "{{FIRST_CONDITION}}" to { FIRST_CONDITION },
        "{{START_ACTION}}" to { START_ACTION },
        "{{STOP_ACTION}}" to { STOP_ACTION },
        "{{DEBUG_ACTION}}" to { DEBUG_ACTION },
        "{{USER_ACTION}}" to { USER_ACTION },
        "{{ADD_ACTION}}" to { ADD_ACTION },
        "{{REMOVE_ACTION}}" to { REMOVE_ACTION },
        "{{REPLACE_ACTION}}" to { REPLACE_ACTION }
    )

    private fun insertValuesIntoPromptPlaceholders(case: RDRCase): String {
        var result = genericSystemInstruction
        placeholders.forEach { (placeholder, valueProvider) ->
            val value = try {
                valueProvider(case)
            } catch (e: Exception) {
                logger.warn("Failed to replace placeholder $placeholder", e)
                placeholder // Fallback to placeholder to avoid corruption
            }
            result = result.replace(placeholder, value)
        }
        return result
    }

    private val expressionCheck = FunctionDeclaration(
        name = IS_EXPRESSION_VALID,
        description = "Check if the user-entered expression represents a valid condition",
        parameters = listOf(
            Schema.str(
                name = "expression",
                description = "The expression to check for validity"
            )
        ),
        requiredParameters = listOf("expression")
    )

    fun createKBChatService(case: RDRCase): ChatService {
        val systemInstruction = insertValuesIntoPromptPlaceholders(case)
        logger.info("system instruction:\n$systemInstruction")
        return GeminiChatService(
            systemInstruction = systemInstruction,
            functionDeclarations = listOf(expressionCheck)
        )
    }
}