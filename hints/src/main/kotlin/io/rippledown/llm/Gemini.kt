package io.rippledown.llm

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.*
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold.NONE
import io.rippledown.conditiongenerator.ConditionSpecification
import io.rippledown.conditiongenerator.fromJson
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.System.getenv
import java.lang.Thread.sleep
import kotlin.random.Random.Default.nextLong

const val GEMINI_MODEL = "gemini-1.5-flash"

val logger: Logger = LoggerFactory.getLogger("rdr")

//Set the model to be as deterministic as possible
val generativeConfig = GenerationConfig.builder().apply { temperature = 0.0f }.build()

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
        text("For an episodic predicate name, the possible values for the signature name are: $episodicSignatures.")
        text("Depending on the predicate, the signature may be an empty object '{}'.")
        text("For a case structure predicate, the signature name should be blank.")
        text("For a series predicate, the signature name should be blank.")
        text("There may be no parameter or just one parameter for the predicate.")
        text("There may be no parameter or just one parameter for the signature.")
        text("---EXAMPLES---")
        text("Examples of expressions with the expected output are:")
        text(examples)
        text("---INPUT EXPRESSION---")
        text("Here is the expression: $input")
        text("---OUTPUT FORMAT---")
        text("Generate output without a leading ```json and without a trailing ```.")
    }
    printPrompt(prompt)
    val json = retry {
        runBlocking {
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
fun <T> retry(
    maxRetries: Int = 10,
    initialDelay: Long = 1_000,
    maxDelay: Long = 32_000,
    block: () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            println("attempt $attempt failed. Waiting $currentDelay ms before retrying")
            sleep(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Max retries of $maxRetries reached")
}