package io.rippledown.kb.chat

import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import io.rippledown.chat.ChatService
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.chat.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString

object KBChatService {
    private val logger = lazyLogger

    private fun readPromptResource(directory: String, resource: String): String {
        val promptResource = "$directory/$resource"
        return (KBChatService::class.java.getResource(promptResource)
            ?: throw IllegalArgumentException("Prompt file not found: $promptResource")).readText()
    }

    private fun String.replacePlaceholders(case: RDRCase): String {
        var result = this
        systemPromptVariables(case).forEach { key, value ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    private val reasonTransformer = FunctionDeclaration(
        name = TRANSFORM_REASON,
        description = "This function transforms a user-entered natural language reason into a formal condition. ",
        parameters = listOf(
            Schema.str(
                name = REASON_PARAMETER,
                description = "The user-entered natural language reason for a condition, e.g. 'The patient has a fever'."
            )
        ),
        requiredParameters = listOf(REASON_PARAMETER)
    )

    fun createKBChatService(case: RDRCase): ChatService {
        val systemInstruction = systemPrompt(case)
        return GeminiChatService(
            systemInstruction = systemInstruction,
            functionDeclarations = listOf(reasonTransformer)
        )
    }

    val systemPromptMainSections = listOf(
        "context.md",
        "task.md",
        "instructions.md",
        "transform-reason.md",
        "confirm-details.md",
        "generate-output.md",
        "general-guidelines.md",
    )
    val systemPromptExampleSections = listOf(
        "examples.md",
        "initial_blank_report.md",
        "initial_non_blank_report.md",
        "invalid-reason.md",
    )

    fun systemPromptVariables(case: RDRCase) = mapOf(
        "COMMENTS" to case.interpretation.toComments(),
        "TRANSFORM_REASON" to TRANSFORM_REASON,
        "REASON" to REASON,
        "FIRST_REASON" to FIRST_REASON,
        "MORE_REASONS" to MORE_REASONS,
        "CONFIRM" to CONFIRM,
        "ADD" to ADD,
        "REMOVE" to REMOVE,
        "REPLACE" to REPLACE,
        "STOP" to STOP,
        "NO_COMMENTS" to NO_COMMENTS,
        "EXISTING_COMMENTS" to EXISTING_COMMENTS,
        "WOULD_YOU_LIKE" to WOULD_YOU_LIKE,
        "ADD_A_COMMENT" to ADD_A_COMMENT,
        "REMOVE_A_COMMENT" to REMOVE_A_COMMENT,
        "REPLACE_A_COMMENT" to REPLACE_A_COMMENT,
        "WHAT_COMMENT" to WHAT_COMMENT,
        "START_ACTION" to START_ACTION,
        "STOP_ACTION" to STOP_ACTION,
        "DEBUG_ACTION" to DEBUG_ACTION,
        "USER_ACTION" to USER_ACTION,
        "ADD_ACTION" to ADD_ACTION,
        "REMOVE_ACTION" to REMOVE_ACTION,
        "REPLACE_ACTION" to REPLACE_ACTION
    )

    fun systemPrompt(case: RDRCase): String {
        val mainSection = systemPromptMainSections.map { it ->
            readPromptResource("/chat/instructions", it).replacePlaceholders(case)
        }
        val exampleSection = systemPromptExampleSections.map { it ->
            readPromptResource("/chat/instructions/examples", it).replacePlaceholders(case)
        }
        return (mainSection + exampleSection).joinToString(separator = "\n")
    }
}

private fun Interpretation.toComments() = conclusionTexts().toJsonString()
