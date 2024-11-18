package io.rippledown.expressionparser

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.*
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold.NONE
import kotlinx.coroutines.runBlocking
import java.lang.System.getenv

const val GEMINI_MODEL = "gemini-1.5-flash"
const val TRAINING_SET_FILE = "/training_set.txt"

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
        text("Your task is to identify the components in an expression E.")
        text("Output the single component, or if several components, separate them by a comma.")
        text("Examples of expressions with the expected output are:")
        text(trainingSet)
        text("Here is the expression: $input")
        text("Generate output without additional string.")
    }

    val response = runBlocking {
        generativeModel.generateContent(prompt)
    }
    return response.text!!.trim().split(", ").toTypedArray()
}
