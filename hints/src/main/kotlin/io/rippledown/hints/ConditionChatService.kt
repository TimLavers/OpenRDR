package io.rippledown.hints

import dev.shreyaspatil.ai.client.generativeai.Chat
import io.rippledown.hints.ConditionSpecification.Companion.decodeOne
import io.rippledown.llm.generativeModel
import io.rippledown.llm.retry
import io.rippledown.log.lazyLogger

/**
 * A chat-style service for transforming user expressions into condition specifications.
 * 
 * This service maintains a chat session where the system prompt is processed once
 * at initialization. Subsequent transformations only send the expression as a message,
 * significantly reducing latency.
 * 
 * Create one instance per rule-building session and reuse it for all expression transformations.
 *
 * @author Cascade AI
 */
class ConditionChatService {
    private val logger = lazyLogger
    private val chat: Chat

    init {
        val systemPrompt = buildSystemPrompt()
        logger.debug("---START SYSTEM PROMPT---\n$systemPrompt\n---END SYSTEM PROMPT---\n")
        val model = generativeModel(systemInstruction = systemPrompt)
        chat = model.startChat()
    }

    /**
     * Transform a user expression into a condition specification.
     * 
     * @param expression The natural language expression to transform (e.g., "glucose is high")
     * @param attributeNames The list of attribute names defined in the KB, used for case-sensitive matching
     * @return The parsed condition specification, or null if transformation failed
     */
    suspend fun transform(expression: String, attributeNames: List<String>): ConditionSpecification? {
        if (attributeNames.isNotEmpty()) {
            val message =
                "The attribute names defined in the knowledge base are: ${attributeNames.joinToString(", ")}. " +
                        "When transforming expressions, use one of these exact attribute names (case-sensitive) if the user's " +
                        "expression refers to an attribute."
            logger.debug("Providing attribute names: $message")
            retry { chat.sendMessage(message) }
        }
        logger.debug("Transforming: $expression")
        val response = retry {
            chat.sendMessage(expression).text
        }
        logger.debug("Response: $response")
        return response?.let { decodeOne(it) }
    }

    companion object {
        private const val RESOURCE_DIR = "/prompt"
        private const val CHAT_SYSTEM_PROMPT = "CHAT_SYSTEM_PROMPT"
        const val EPISODIC_PREDICATES = "EPISODIC_PREDICATES"
        const val SERIES_PREDICATES = "SERIES_PREDICATES"
        const val CASE_STRUCTURE_PREDICATES = "CASE_STRUCTURE_PREDICATES"
        const val EPISODIC_SIGNATURES = "EPISODIC_SIGNATURES"
        const val SINGLE_EXPRESSION_EXAMPLES = "SINGLE_EXPRESSION_EXAMPLES"

        private fun readResource(resourceKey: String): String {
            val path = "$RESOURCE_DIR/${resourceKey.lowercase()}.txt"
            return (ConditionChatService::class.java.getResource(path)
                ?: throw IllegalArgumentException("Resource file not found: $path")).readText()
        }

        private fun examples() = examplesFrom(
            readResource(SINGLE_EXPRESSION_EXAMPLES).split("\n")
        )

        internal fun buildSystemPrompt(): String {
            val promptVariables: Map<String, String> = mapOf(
                EPISODIC_PREDICATES to readResource(EPISODIC_PREDICATES),
                EPISODIC_SIGNATURES to readResource(EPISODIC_SIGNATURES),
                SERIES_PREDICATES to readResource(SERIES_PREDICATES),
                CASE_STRUCTURE_PREDICATES to readResource(CASE_STRUCTURE_PREDICATES),
                SINGLE_EXPRESSION_EXAMPLES to examples(),
            )
            val templateText = readResource(CHAT_SYSTEM_PROMPT)
            return templateText.replacePromptPlaceholders(promptVariables)
        }
    }
}

/**
 * Replaces placeholders in a template string with their corresponding values.
 * Placeholders are in the format {{KEY}}.
 */
fun String.replacePromptPlaceholders(variables: Map<String, String>): String {
    var result = this
    variables.forEach { (key, value) ->
        result = result.replace("{{$key}}", value)
    }
    return result
}
