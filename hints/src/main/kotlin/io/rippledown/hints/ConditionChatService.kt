package io.rippledown.hints

import com.google.genai.Chat
import io.rippledown.hints.ConditionSpecification.Companion.decodeOne
import io.rippledown.llm.*
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
class ConditionChatService : ConditionTransformer {
    private val logger = lazyLogger
    private val systemPrompt: String

    private val chatFactory: (String) -> Chat
    private var chat: Chat

    private var attributeNames: List<String> = emptyList()

    constructor() : this(chatFactory = { prompt ->
        val config = generateContentConfig(systemInstruction = prompt)
        geminiClient.chats.create(GEMINI_MODEL, config)
    })

    internal constructor(chatFactory: (String) -> Chat) {
        this.chatFactory = chatFactory
        systemPrompt = buildSystemPrompt()
        logger.debug("---START SYSTEM PROMPT---\n$systemPrompt\n---END SYSTEM PROMPT---\n")
        chat = chatFactory(systemPrompt)
    }

    override fun setAttributeNames(attributeNames: List<String>) {
        this.attributeNames = attributeNames
    }

    /**
     * Transform a user expression into a condition specification.
     * Attribute names, if previously set via [setAttributeNames], are included in the message.
     * 
     * @param expression The natural language expression to transform (e.g., "glucose is high")
     * @return The parsed condition specification, or null if transformation failed
     */
    override suspend fun transform(expression: String): ConditionSpecification? {
        logger.info("Transforming: $expression")
        val message = buildTransformMessage(expression, attributeNames)
        val response = try {
            retry(maxRetries = 3) {
                logger.info("Sending expression to LLM...")
                callWithTimeout(timeoutMs = GEMINI_CALL_TIMEOUT) { chat.sendMessage(message).text() }
            }
        } catch (e: Exception) {
            logger.warn("Transform failed, resetting chat and retrying: ${e.message}")
            chat = chatFactory(systemPrompt)
            retry(maxRetries = 3) {
                logger.info("Sending expression to LLM (after reset)...")
                callWithTimeout(timeoutMs = GEMINI_CALL_TIMEOUT) { chat.sendMessage(message).text() }
            }
        }
        logger.info("Transform response: $response")
        return response?.let { decodeOne(it) }
    }

    private fun buildTransformMessage(expression: String, attributeNames: List<String>): String {
        return if (attributeNames.isNotEmpty()) {
            "${buildAttributePrompt(attributeNames)}\n\n$expression"
        } else {
            expression
        }
    }

    companion object {
        private const val GEMINI_CALL_TIMEOUT = 30_000L
        private const val RESOURCE_DIR = "/prompt"
        private const val CHAT_SYSTEM_PROMPT = "CHAT_SYSTEM_PROMPT"
        internal const val CHAT_ATTRIBUTE_PROMPT = "chat_attribute_prompt"
        const val EPISODIC_PREDICATES = "EPISODIC_PREDICATES"
        const val SERIES_PREDICATES = "SERIES_PREDICATES"
        const val CASE_STRUCTURE_PREDICATES = "CASE_STRUCTURE_PREDICATES"
        const val EPISODIC_SIGNATURES = "EPISODIC_SIGNATURES"
        const val SINGLE_EXPRESSION_EXAMPLES = "SINGLE_EXPRESSION_EXAMPLES"
        const val ATTRIBUTE_NAMES = "ATTRIBUTE_NAMES"

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
            return readResource(CHAT_SYSTEM_PROMPT)
                .replacePromptPlaceholders(promptVariables)
        }

        internal fun buildAttributePrompt(attributeNames: List<String>): String {
            val promptVariables: Map<String, String> = mapOf(
                ATTRIBUTE_NAMES to attributeNames.joinToString(separator = "\n") { "\"$it\"" },
            )
            return readResource(CHAT_ATTRIBUTE_PROMPT)
                .replacePromptPlaceholders(promptVariables)
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
