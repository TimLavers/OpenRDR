package io.rippledown.llm

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold.NONE
import dev.shreyaspatil.ai.client.generativeai.type.GenerationConfig
import dev.shreyaspatil.ai.client.generativeai.type.HarmCategory
import dev.shreyaspatil.ai.client.generativeai.type.SafetySetting
import dev.shreyaspatil.ai.client.generativeai.type.content
import io.rippledown.conditiongenerator.ConditionSpecification
import io.rippledown.conditiongenerator.fromJson
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

//todo remove
fun tokensFor(input: String): List<String> {
    return emptyList()
}

fun conditionSpecificationFor(input: String): ConditionSpecification {
    val prompt = content {
        text("Your task is to create a json object from an expression.")
        text("The json object should have two fields: predicate and signature.")
        text("The predicate field should have two fields: name and parameters.")
        text("The signature field should have two fields: name and parameters.")
        text("The possible values for the predicate name are: Contains, DoesNotContain, Is, Low, High, Normal, GreaterThanOrEquals, LessThanOrEquals.")
        text("The predicate name should be blank if the expression does not refer to one of the possible predicates.")
        text("The possible values for the signature name are: All, Current, No, AtLeast, AtMost.")
        text("There may be no parameter or just one parameter for the predicate.")
        text("There may be no parameter or just one parameter for the signature.")
        text("Examples of expressions with the expected output are:")
        text(trainingSet)
        text("Here is the expression: $input")
        text("Generate output without a leading ```json or trailing ```.")
    }

    val json = retry {
        runBlocking {
            generativeModel.generateContent(prompt).text
        }
    }
    return if (json != null) fromJson(json) else ConditionSpecification()
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