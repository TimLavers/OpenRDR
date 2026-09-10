package io.rippledown.kb.chat

import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.Schema
import com.google.genai.types.Type
import io.rippledown.chat.ChatService
import io.rippledown.chat.Conversation.Companion.CONDITION_TEXT_PARAMETER
import io.rippledown.chat.Conversation.Companion.GET_SUGGESTED_CONDITIONS
import io.rippledown.chat.Conversation.Companion.NEW_VALUE_PARAMETER
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.Conversation.Companion.SELECT_SUGGESTED_CONDITION
import io.rippledown.chat.Conversation.Companion.SUGGESTION_NUMBER_PARAMETER
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.chat.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.Attribute
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.toJsonString

object KBChatService {
    private val logger = lazyLogger

    private fun readPromptResource(directory: String, resource: String): String {
        val promptResource = "$directory/$resource"
        return (KBChatService::class.java.getResource(promptResource)
            ?: throw IllegalArgumentException("Prompt file not found: $promptResource")).readText()
    }

    private fun String.replacePlaceholders(variables: Map<String, String>): String {
        var result = this
        variables.forEach { (key, value) ->
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

    private val selectSuggestionDeclaration = FunctionDeclaration.builder()
        .name(SELECT_SUGGESTED_CONDITION)
        .description(
            "Adds the suggested condition the user chose to the rule session. Use this, not " +
                    "$TRANSFORM_REASON, whenever the user chooses one of the suggestions they were shown, " +
                    "whether it is editable or not. Identify the suggestion by its number in that list: the " +
                    "system resolves the number, so you never have to reproduce the condition's text."
        )
        .parameters(
            Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(
                    mapOf(
                        SUGGESTION_NUMBER_PARAMETER to Schema.builder()
                            .type(Type.Known.INTEGER)
                            .description("The number of the chosen suggestion in the list shown to the user, counting from 1.")
                            .build(),
                        NEW_VALUE_PARAMETER to Schema.builder()
                            .type(Type.Known.STRING)
                            .description(
                                "For an [editable] suggestion only: the value the user gave when asked what " +
                                        "value they wanted. Omit it when first told that an editable " +
                                        "suggestion was chosen, and omit it entirely for a suggestion that " +
                                        "is not editable."
                            )
                            .build(),
                        CONDITION_TEXT_PARAMETER to Schema.builder()
                            .type(Type.Known.STRING)
                            .description(
                                "The exact text of the chosen suggestion. Give this only when you do not " +
                                        "know its number; the number is preferred."
                            )
                            .build()
                    )
                )
                .required(listOf(SUGGESTION_NUMBER_PARAMETER))
                .build()
        )
        .build()

    fun createKBChatService(
        viewableCase: ViewableCase?,
        kbName: String?,
        kbNames: List<String>,
        attributeById: (Int) -> Attribute? = { null },
        allAttributes: Set<Attribute> = emptySet()
    ): ChatService {
        val systemInstruction = systemPrompt(viewableCase, kbName, kbNames, attributeById, allAttributes)
        val functionDeclarations =
            if (viewableCase == null) emptyList()
            else listOf(reasonTransformer, suggestedConditionsRetriever, selectSuggestionDeclaration)
        return GeminiChatService(
            systemInstruction = systemInstruction,
            functionDeclarations = functionDeclarations
        )
    }

    const val NO_KB_NAME = "none"
    const val NO_KB_NAMES = "there are none"

    // The sections that do not refer to the current case, so are meaningful when there is no case.
    val caseLessSections = listOf(
        "1_task.md",
        "2_interactions.md",
        "13_json_format_guidelines.md",
        "14_general-guidelines.md",
        "16_listing_capabilities.md",
        "20_knowledge_base_management.md",
    )

    val systemPromptMainSections = listOf(
        "1_task.md",
        "2_interactions.md",
        "3_defining_the_report_change.md",
        "4_comment_variables.md",
        "5_starting_the_rule_session.md",
        "6_defining_the_reasons.md",
        "7_suggested_conditions.md",
        "8_transform-reason.md",
        "9_allow_or_disallow_cornerstone.md",
        "10_completing_the_report_change.md",
        "11_undoing_the_report_change.md",
        "12_reordering_the_case_attributes.md",
        "13_json_format_guidelines.md",
        "14_general-guidelines.md",
        "15_cancelling_the_rule.md",
        "16_listing_capabilities.md",
        "17_assigning_derived_values.md",
        "18_editing_derived_definition.md",
        "19_naming_and_renaming.md",
        "20_knowledge_base_management.md",
        "25_favourite_cases.md",
    )

    fun mainSectionsFor(hasCase: Boolean) = if (hasCase) systemPromptMainSections else caseLessSections

    val systemPromptExampleSections = listOf(
        "examples.md",
        "initial_blank_report.md",
        "initial_non_blank_report.md",
        "invalid-reason.md",
        "non_english_comment.md",
    )

    fun systemPromptVariables(
        viewableCase: ViewableCase?,
        kbName: String? = null,
        kbNames: List<String> = emptyList(),
        attributeById: (Int) -> Attribute? = { null },
        allAttributes: Set<Attribute> = emptySet()
    ) = mapOf(
        "ADD" to ADD,
        "ADD_A_COMMENT" to ADD_A_COMMENT,
        "ADD_COMMENT" to ADD_COMMENT,
        "ATTRIBUTES" to (viewableCase?.attributes()?.joinToString("\n") { it.name } ?: ""),
        "ALL_ATTRIBUTES" to allAttributes.joinToString("\n") { it.name },
        // The viewable interpretation holds the resolved copy of the case's
        // interpretation, in which ByDefinition comment assignments have been
        // substituted with their stored definitions.
        "COMMENTS" to (viewableCase?.let {
            it.viewableInterpretation.interpretation.toComments(it.case, attributeById)
        } ?: "[]"),
        "KB_NAME" to (kbName ?: NO_KB_NAME),
        "KB_NAMES" to (kbNames.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: NO_KB_NAMES),
        "TRANSFORM_REASON" to TRANSFORM_REASON,
        "GET_SUGGESTED_CONDITIONS" to GET_SUGGESTED_CONDITIONS,
        "REASON" to REASON,
        "FIRST_REASON" to FIRST_REASON,
        "MORE_REASONS" to MORE_REASONS,
        "CONFIRM" to CONFIRM,
        "REMOVE_COMMENT" to REMOVE_COMMENT,
        "REPLACE_COMMENT" to REPLACE_COMMENT,
        "ASSIGN_DERIVED_VALUE" to ASSIGN_DERIVED_VALUE,
        "REMOVE_DERIVED_VALUE" to REMOVE_DERIVED_VALUE,
        "REPLACE_DERIVED_VALUE" to REPLACE_DERIVED_VALUE,
        "EDIT_DERIVED_DEFINITION" to EDIT_DERIVED_DEFINITION,
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
        "SHOW_LAST_RULE_FOR_UNDO" to SHOW_LAST_RULE_FOR_UNDO,
        "MOVE_ATTRIBUTE" to MOVE_ATTRIBUTE,
        "RENAME_ATTRIBUTE" to RENAME_ATTRIBUTE,
        "REMOVE_REASON" to REMOVE_REASON,
        "CANCEL_RULE" to CANCEL_RULE,
        "SELECT_SUGGESTION" to SELECT_SUGGESTED_CONDITION,
        "COPY_CASE_TO_FAVOURITES" to COPY_CASE_TO_FAVOURITES,
        "DELETE_CASE_FROM_FAVOURITES" to DELETE_CASE_FROM_FAVOURITES,
        "COPY_CASE_TO_FAVOURITES_WITH_NEW_NAME" to COPY_CASE_TO_FAVOURITES_WITH_NEW_NAME,
        "LIST_KNOWLEDGE_BASES" to LIST_KNOWLEDGE_BASES,
        "OPEN_KNOWLEDGE_BASE" to OPEN_KNOWLEDGE_BASE,
        "CREATE_KNOWLEDGE_BASE" to CREATE_KNOWLEDGE_BASE,
        "CLOSE_KNOWLEDGE_BASE" to CLOSE_KNOWLEDGE_BASE,
        "DELETE_KNOWLEDGE_BASE" to DELETE_KNOWLEDGE_BASE,
        "ADD_DEMONSTRATION_CASE" to ADD_DEMONSTRATION_CASE,
        "RENAME_KNOWLEDGE_BASE" to RENAME_KNOWLEDGE_BASE,
        "SHOW_KNOWLEDGE_BASE_DESCRIPTION" to SHOW_KNOWLEDGE_BASE_DESCRIPTION,
        "SET_KNOWLEDGE_BASE_DESCRIPTION" to SET_KNOWLEDGE_BASE_DESCRIPTION,
    )

    fun systemPrompt(
        viewableCase: ViewableCase?,
        kbName: String? = null,
        kbNames: List<String> = emptyList(),
        attributeById: (Int) -> Attribute? = { null },
        allAttributes: Set<Attribute> = emptySet()
    ): String {
        val variables = systemPromptVariables(viewableCase, kbName, kbNames, attributeById, allAttributes)
        val mainSection = mainSectionsFor(hasCase = viewableCase != null).map {
            readPromptResource("/chat/instructions", it).replacePlaceholders(variables)
        }
        val exampleSections = if (viewableCase == null) emptyList() else systemPromptExampleSections
        val exampleSection = exampleSections.map {
            readPromptResource("/chat/instructions/examples", it).replacePlaceholders(variables)
        }
        return (mainSection + exampleSection).joinToString(separator = "\n")
    }
}

private fun Interpretation.toComments() = assignments()
    .mapNotNull { (it.expression as? CommentTemplate)?.textWithVariableNames() }
    .toSet()
    .toJsonString()
