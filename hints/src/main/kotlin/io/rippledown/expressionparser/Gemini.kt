package io.rippledown.expressionparser

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold.NONE
import dev.shreyaspatil.ai.client.generativeai.type.GenerationConfig
import dev.shreyaspatil.ai.client.generativeai.type.HarmCategory
import dev.shreyaspatil.ai.client.generativeai.type.SafetySetting
import dev.shreyaspatil.ai.client.generativeai.type.content
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.System.getenv
import java.lang.Thread.sleep
import kotlin.random.Random.Default.nextLong

const val GEMINI_MODEL = "gemini-1.5-flash"
const val TRAINING_SET_FILE = "/training_set.txt"

val logger: Logger = LoggerFactory.getLogger("rdr")

//Set the model to be as deterministic as possible
val generativeConfig = GenerationConfig.builder().apply { temperature = 0.0f }.build()

val generativeModel = GenerativeModel(
    modelName = GEMINI_MODEL,
    apiKey = getenv("GEMINI_API_KEY"),
    safetySettings = noSafetySettings(),
    generationConfig = generativeConfig
)

val trainingSet = trainingSet(TRAINING_SET_FILE)

fun noSafetySettings() =
    HarmCategory.entries
        .filter { harmCategory -> harmCategory != HarmCategory.UNKNOWN } //Invalid safety setting
        .map { harmCategory ->
            SafetySetting(
                harmCategory = harmCategory,
                threshold = NONE
            )
        }

fun tokensFor(input: String): Array<String> {
    val prompt = content {
        text("Your task is to identify the components in an expression.")
        text("Output the single component, or if several components, separate them by a comma.")
        text("Examples of expressions with the expected output are:")
        text(trainingSet)
        text("Here is the expression: $input")
        text("Generate output without additional string.")
    }

    val tokens = retry {
        runBlocking {
            generativeModel.generateContent(prompt).text
        }
    }
    return if (tokens != null) {
        tokens.trim().split(", ").toTypedArray()
    } else emptyArray()
}

/**
 * Try to get around the 503 error from the API due to rate limiting.
 */
fun <T> retry(
    maxRetries: Int = 5,
    initialDelay: Long = 1_000,
    maxDelay: Long = 16_000,
    block: () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            println("Retry attempt $attempt failed. Waiting $currentDelay ms before trying again.")
            sleep(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Should not reach here")
}