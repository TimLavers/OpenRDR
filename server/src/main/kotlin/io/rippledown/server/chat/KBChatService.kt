package io.rippledown.server.chat

import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import io.rippledown.chat.ChatService
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.chat.GeminiChatService
import io.rippledown.constants.chat.*

object KBChatService {

    private fun readPromptResource(directory: String, resource: String): String {
        val promptResource = "$directory/$resource"
        return (KBChatService::class.java.getResource(promptResource)
            ?: throw IllegalArgumentException("Prompt file not found: $promptResource")).readText()
    }

    private fun String.replacePlaceholders(): String {
        var result = this
        systemPromptVariables().forEach { (key, value) ->
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

    fun createKBChatService(): ChatService {
        val systemInstruction = systemPrompt()
        return GeminiChatService(
            systemInstruction = systemInstruction,
            functionDeclarations = listOf(reasonTransformer)
        )
    }

    val systemPromptMainSections = listOf(
        "1_task.md",
        "2_interactions.md",
        "2.1_knowledge_base_management.md",
        "3_defining_the_report_change.md",
        "4_starting_the_rule_session.md",
        "5_defining_the_reasons.md",

        "7_transform-reason.md",
        "8_allow_or_disallow_cornerstone.md",
        "9_completing_the_report_change.md",
        "10_undoing_the_report_change.md",
        "11_reordering_the_case_attributes.md",
        "12_json_format_guidelines.md",
        "13_general-guidelines.md",
    )
    val systemPromptExampleSections = listOf(
        "examples.md",
        "initial_blank_report.md",
        "initial_non_blank_report.md",
        "invalid-reason.md",
    )

    fun systemPromptVariables() = mapOf(
        "ADD" to ADD,
        "ADD_A_COMMENT" to ADD_A_COMMENT,
        "ADD_COMMENT" to ADD_COMMENT,
        "TRANSFORM_REASON" to TRANSFORM_REASON,
        "REASON" to REASON,
        "FIRST_REASON" to FIRST_REASON,
        "MORE_REASONS" to MORE_REASONS,
        "CONFIRM" to CONFIRM,
        "REMOVE_COMMENT" to REMOVE_COMMENT,
        "REPLACE_COMMENT" to REPLACE_COMMENT,
        "SHOW_CORNERSTONES" to SHOW_CORNERSTONES,
        "REMOVE" to REMOVE,
        "REPLACE" to REPLACE,
        "STOP" to STOP,
        "NO_COMMENTS" to NO_COMMENTS,
        "EXISTING_COMMENTS" to EXISTING_COMMENTS,
        "WOULD_YOU_LIKE" to WOULD_YOU_LIKE,
        "REMOVE_A_COMMENT" to REMOVE_A_COMMENT,
        "REPLACE_A_COMMENT" to REPLACE_A_COMMENT,
        "WHAT_COMMENT" to WHAT_COMMENT,
        "START_ACTION" to START_ACTION,
        "DEBUG_ACTION" to DEBUG_ACTION,
        "USER_ACTION" to USER_ACTION,
        "COMMIT_RULE" to COMMIT_RULE,
        "EXEMPT_CORNERSTONE" to EXEMPT_CORNERSTONE,
        "UNDO_LAST_RULE" to UNDO_LAST_RULE,
        "MOVE_ATTRIBUTE" to MOVE_ATTRIBUTE
    )

    fun systemPrompt(): String {
        val mainSection = systemPromptMainSections.map {
            readPromptResource("/chat/instructions", it).replacePlaceholders()
        }
        val exampleSection = systemPromptExampleSections.map {
            readPromptResource("/chat/instructions/examples", it).replacePlaceholders()
        }
        return (mainSection + exampleSection).joinToString(separator = "\n")
    }
}
