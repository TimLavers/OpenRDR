package io.rippledown.hints

import com.google.genai.Chat
import io.rippledown.hints.ConditionSpecification.Companion.decodeOne
import io.rippledown.llm.*
import io.rippledown.log.lazyLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    private val systemPrompt: String

    private val chatFactory: (String) -> Chat
    private var chat: Chat

    // Eager background initialization support
    private var pendingAttributeNames: List<String>? = null
    private var attributesInitialized = false
    private var initJob: Job? = null

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

    /**
     * Set attribute names and eagerly start sending them to the LLM in the background.
     * This way, the initialization is already done (or nearly done) by the time
     * the first transform call comes in.
     */
    fun setAttributeNames(attributeNames: List<String>) {
        initJob?.cancel()
        pendingAttributeNames = attributeNames
        attributesInitialized = false
        initJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                updateChatWithAttributeNames(attributeNames)
            } catch (e: Exception) {
                logger.warn("Background attribute initialization failed: ${e.message}")
            }
        }
    }

    private suspend fun ensureAttributesInitialized() {
        if (!attributesInitialized && pendingAttributeNames != null) {
            initJob?.join()
            if (!attributesInitialized) {
                updateChatWithAttributeNames(pendingAttributeNames!!)
                attributesInitialized = true
            }
        }
    }

    suspend fun updateChatWithAttributeNames(attributeNames: List<String>) {
        val message = buildAttributePrompt(attributeNames)
        logger.info("Providing attribute names: ${attributeNames.joinToString { it }}")
        try {
            retry(maxRetries = 3) {
                logger.info("Sending attribute names to LLM...")
                callWithTimeout(timeoutMs = GEMINI_CALL_TIMEOUT) { chat.sendMessage(message) }
            }
        } catch (e: Exception) {
            logger.warn("Attribute update failed, resetting chat and retrying: ${e.message}")
            chat = chatFactory(systemPrompt)
            retry(maxRetries = 3) {
                logger.info("Sending attribute names to LLM (after reset)...")
                callWithTimeout(timeoutMs = GEMINI_CALL_TIMEOUT) { chat.sendMessage(message) }
            }
        }
        logger.info("Attribute names provided successfully")
        attributesInitialized = true
    }

    /**
     * Transform a user expression into a condition specification.
     * 
     * @param expression The natural language expression to transform (e.g., "glucose is high")
     * @return The parsed condition specification, or null if transformation failed
     */
    suspend fun transform(expression: String): ConditionSpecification? {
        ensureAttributesInitialized()
        logger.info("Transforming: $expression")
        val response = try {
            retry(maxRetries = 3) {
                logger.info("Sending expression to LLM...")
                callWithTimeout(timeoutMs = GEMINI_CALL_TIMEOUT) { chat.sendMessage(expression).text() }
            }
        } catch (e: Exception) {
            logger.warn("Transform failed, resetting chat and retrying: ${e.message}")
            resetChat()
            retry(maxRetries = 3) {
                logger.info("Sending expression to LLM (after reset)...")
                callWithTimeout(timeoutMs = GEMINI_CALL_TIMEOUT) { chat.sendMessage(expression).text() }
            }
        }
        logger.info("Transform response: $response")
        return response?.let { decodeOne(it) }
    }

    private suspend fun resetChat() {
        chat = chatFactory(systemPrompt)
        attributesInitialized = false
        ensureAttributesInitialized()
    }

    companion object {
        private const val GEMINI_CALL_TIMEOUT = 30_000L
        private const val RESOURCE_DIR = "/prompt"
        private const val CHAT_SYSTEM_PROMPT = "CHAT_SYSTEM_PROMPT"
        private const val CHAT_ATTRIBUTE_PROMPT = "chat_attribute_prompt"
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
