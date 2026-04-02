package io.rippledown.kb.chat

import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.Schema
import com.google.genai.types.Type
import io.rippledown.chat.ChatService
import io.rippledown.chat.Conversation.Companion.GET_SUGGESTED_CONDITIONS
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.chat.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.toJsonString

object KBChatService {
    private val logger = lazyLogger

    private fun readPromptResource(directory: String, resource: String): String {
        val promptResource = "$directory/$resource"
        return (KBChatService::class.java.getResource(promptResource)
            ?: throw IllegalArgumentException("Prompt file not found: $promptResource")).readText()
    }

    private fun String.replacePlaceholders(viewableCase: ViewableCase): String {
        var result = this
        systemPromptVariables(viewableCase).forEach { key, value ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    private val reasonTransformer = FunctionDeclaration.builder()
        .name(TRANSFORM_REASON)
        .description("This function transforms a user-entered natural language reason into a formal condition. ")
        .parameters(
            Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(
                    mapOf(
                        REASON_PARAMETER to Schema.builder()
                            .type(Type.Known.STRING)
                            .description("The user-entered natural language reason for a condition, e.g. 'The patient has a fever'.")
                            .build()
                    )
                )
                .required(listOf(REASON_PARAMETER))
                .build()
        )
        .build()


    private val suggestedConditionsRetriever = FunctionDeclaration.builder()
        .name(GET_SUGGESTED_CONDITIONS)
        .description("Retrieves a numbered list of suggested conditions for the current case. Call this after the rule session has been started and before asking the user for reasons.")
        .parameters(
            Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(emptyMap())
                .build()
        )
        .build()
    fun createKBChatService(viewableCase: ViewableCase): ChatService {
        val systemInstruction = systemPrompt(viewableCase)
        return GeminiChatService(
            systemInstruction = systemInstruction,
            functionDeclarations = listOf(reasonTransformer, suggestedConditionsRetriever)
        )
    }

    val systemPromptMainSections = listOf(
        "1_task.md",
        "2_interactions.md",
        "3_defining_the_report_change.md",
        "4_starting_the_rule_session.md",
        "5_defining_the_reasons.md",
        "6_suggested_conditions.md",
        "7_transform-reason.md",
        "8_allow_or_disallow_cornerstone.md",
        "9_completing_the_report_change.md",
        "10_undoing_the_report_change.md",
        "11_reordering_the_case_attributes.md",
        "12_json_format_guidelines.md",
        "13_general-guidelines.md",
        "14_cancelling_the_rule.md",
    )
    val systemPromptExampleSections = listOf(
        "examples.md",
        "initial_blank_report.md",
        "initial_non_blank_report.md",
        "invalid-reason.md",
    )

    fun systemPromptVariables(viewableCase: ViewableCase) = mapOf(
        "ADD" to ADD,
        "ADD_A_COMMENT" to ADD_A_COMMENT,
        "ADD_COMMENT" to ADD_COMMENT,
        "ATTRIBUTES" to viewableCase.attributes().joinToString("\n") { it.name },
        "COMMENTS" to viewableCase.case.interpretation.toComments(),
        "TRANSFORM_REASON" to TRANSFORM_REASON,
        "GET_SUGGESTED_CONDITIONS" to GET_SUGGESTED_CONDITIONS,
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
        "NEXT_CORNERSTONE" to NEXT_CORNERSTONE,
        "PREVIOUS_CORNERSTONE" to PREVIOUS_CORNERSTONE,
        "UNDO_LAST_RULE" to UNDO_LAST_RULE,
        "MOVE_ATTRIBUTE" to MOVE_ATTRIBUTE,
        "REMOVE_REASON" to REMOVE_REASON,
        "CANCEL_RULE" to CANCEL_RULE,

    )

    fun systemPrompt(viewableCase: ViewableCase): String {
        val mainSection = systemPromptMainSections.map { it ->
            readPromptResource("/chat/instructions", it).replacePlaceholders(viewableCase)
        }
        val exampleSection = systemPromptExampleSections.map { it ->
            readPromptResource("/chat/instructions/examples", it).replacePlaceholders(viewableCase)
        }
        return (mainSection + exampleSection).joinToString(separator = "\n")
    }
}

private fun Interpretation.toComments() = conclusionTexts().toJsonString()
