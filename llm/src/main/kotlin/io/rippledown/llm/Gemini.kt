package io.rippledown.llm

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.*
import io.rippledown.log.lazyLogger
import kotlinx.coroutines.delay
import java.lang.System.getenv
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

val GEMINI_MODEL = "gemini-2.5-flash"
var GEMINI_API_KEY = getenv("API_KEY") ?: ""

fun generativeModel(
    systemInstruction: String = "",
    functionDeclarations: List<FunctionDeclaration> = emptyList()
) = GenerativeModel(
    modelName = GEMINI_MODEL,
    apiKey = GEMINI_API_KEY,
    safetySettings = noSafetySettings(),
    generationConfig = generativeConfig(),
    systemInstruction = content { text(systemInstruction) },
    tools = if (functionDeclarations.isNotEmpty()) listOf(Tool(functionDeclarations)) else emptyList()
)

fun noSafetySettings() =
    HarmCategory.entries
        .filter { harmCategory -> harmCategory != HarmCategory.UNKNOWN } //Invalid safety setting
        .map { harmCategory ->
            SafetySetting(
                harmCategory = harmCategory,
                threshold = BlockThreshold.NONE
            )
        }

//Set the model to be as deterministic as possible
fun generativeConfig() = GenerationConfig.Companion.builder().apply {
    temperature = 0f
    topP = 0.995f
}.build()

/**
 * Retry when receiving the 503 error from the API due to rate limiting.
 */
object Retry

suspend fun <T> retry(
    maxRetries: Int = 10,
    initialDelay: Long = 1_000,
    maxDelay: Long = 32_000,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            Retry.lazyLogger.info("attempt $attempt failed. Waiting $currentDelay ms before retrying")
            delay(currentDelay.milliseconds)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + Random.Default.nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Max retries of $maxRetries reached")
}