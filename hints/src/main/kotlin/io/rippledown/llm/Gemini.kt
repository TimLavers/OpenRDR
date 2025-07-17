package io.rippledown.llm

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.*
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold.NONE
import io.rippledown.conditiongenerator.ConditionSpecification
import io.rippledown.conditiongenerator.fromJson
import io.rippledown.log.lazyLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.System.getenv
import kotlin.random.Random.Default.nextLong
import kotlin.time.Duration.Companion.milliseconds

const val GEMINI_MODEL = "gemini-2.5-flash"

//Set the model to be as deterministic as possible
val generativeConfig = GenerationConfig.builder().apply {
    temperature = 0.0f
    topP = 0.995f
}.build()

val generativeModel = GenerativeModel(
    modelName = GEMINI_MODEL,
    apiKey = getenv("GEMINI_API_KEY"),
    safetySettings = noSafetySettings(),
    generationConfig = generativeConfig
)

val episodicPredicates = linesFrom("/prompt/episodic_predicates.txt").joinToString("\n")
val episodicSignatures = linesFrom("/prompt/episodic_signatures.txt").joinToString("\n")
val seriesPredicates = linesFrom("/prompt/series_predicates.txt").joinToString("\n")
val caseStructurePredicates = linesFrom("/prompt/case_structure_predicates.txt").joinToString("\n")
val examples = examplesFrom(linesFrom("/prompt/examples.txt"))

fun noSafetySettings() =
    HarmCategory.entries
        .filter { harmCategory -> harmCategory != HarmCategory.UNKNOWN } //Invalid safety setting
        .map { harmCategory ->
            SafetySetting(
                harmCategory = harmCategory,
                threshold = NONE
            )
        }

fun conditionSpecificationFor(input: String): ConditionSpecification {
    val prompt = content {
        text("---TASK SUMMARY---")
        text("Your task is to create a json object from an expression.")
        text("---TASK DETAILS---")
        text("The json object should have two fields: predicate and signature.")
        text("The predicate field should have two fields: name and parameters.")
        text("The signature field should have two fields: name and parameters.")
        text("There are three types of predicate names: episodic, case structure and series.")
        text("The possible values for an episodic predicate name are: $episodicPredicates.")
        text("The possible values for a case structure predicate name are: $caseStructurePredicates.")
        text("The possible values for a series predicate name are: $seriesPredicates.")
        text("The predicate name should be blank if the expression does not refer to one of the possible predicates.")
        text("For an episodic predicate: $episodicPredicates, the possible values for the signature name are only: $episodicSignatures.")
        text("For a case structure predicate: ${caseStructurePredicates} or a series predicate: $seriesPredicates, there must be no signature.")
        text("There may be no parameter or just one parameter for a predicate.")
        text("If there is a signature, it may have either no parameter or just one parameter.")
        text("---EXAMPLES---")
        text("Examples of expressions with the expected output are:")
        text(examples)
        text("---INPUT EXPRESSION---")
        text("Here is the expression: $input")
        text("---OUTPUT FORMAT---")
        text("Generate output without a leading ```json and without a trailing ```.")
    }
    val json = runBlocking {
        retry {
            generativeModel.generateContent(prompt).text
        }
    }
    return if (json != null) fromJson(json) else ConditionSpecification()
}

private fun printPrompt(prompt: Content) {
    prompt.parts.forEach { System.out.println(it.asTextOrNull()) }
}

private fun linesFrom(resourceFileName: String) =
    object {}.javaClass.getResourceAsStream(resourceFileName)?.bufferedReader()!!.readLines()

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
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Max retries of $maxRetries reached")
}