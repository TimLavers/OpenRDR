package io.rippledown.llm

import com.google.genai.Client
import com.google.genai.types.*
import io.rippledown.log.lazyLogger
import kotlinx.coroutines.delay
import java.lang.System.getenv
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

val GEMINI_MODEL = "gemini-2.5-flash"
var GEMINI_API_KEY = getenv("API_KEY") ?: ""

val geminiClient: Client by lazy {
    Client.builder().apiKey(GEMINI_API_KEY).build()
}

fun generateContentConfig(
    systemInstruction: String,
    functionDeclarations: List<FunctionDeclaration> = emptyList()
): GenerateContentConfig {
    val builder = GenerateContentConfig.builder()
        .temperature(0f)
        .topP(0.995f)
        .safetySettings(noSafetySettings())
        .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))

    if (functionDeclarations.isNotEmpty()) {
        builder.tools(Tool.builder().functionDeclarations(functionDeclarations).build())
    }

    return builder.build()
}

fun noSafetySettings(): List<SafetySetting> =
    listOf(
        HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH,
        HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT,
        HarmCategory.Known.HARM_CATEGORY_SEXUALLY_EXPLICIT,
        HarmCategory.Known.HARM_CATEGORY_HARASSMENT,
        HarmCategory.Known.HARM_CATEGORY_CIVIC_INTEGRITY,
    ).map { category ->
        SafetySetting.builder()
            .category(category)
            .threshold(HarmBlockThreshold.Known.OFF)
            .build()
    }

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
            e.printStackTrace()
            if (attempt == maxRetries - 1) throw e
            Retry.lazyLogger.info("attempt $attempt failed. Waiting $currentDelay ms before retrying")
            delay(currentDelay.milliseconds)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + Random.Default.nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Max retries of $maxRetries reached")
}