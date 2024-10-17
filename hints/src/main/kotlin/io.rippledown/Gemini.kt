package io.rippledown

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold.NONE
import dev.shreyaspatil.ai.client.generativeai.type.HarmCategory
import dev.shreyaspatil.ai.client.generativeai.type.SafetySetting
import dev.shreyaspatil.ai.client.generativeai.type.content
import kotlinx.coroutines.runBlocking
import java.lang.System.getenv

const val GEMINI_MODEL = "gemini-1.5-flash"
const val TRAINING_SET_FILE = "/training_set.txt"

val generativeModel = GenerativeModel(
    modelName = GEMINI_MODEL,
    apiKey = getenv("GEMINI_API_KEY"),
    safetySettings = noSafetySettings()
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

fun suggestionFor(input: String): String {
    val prompt = content {
        text("Perform a task that replaces x with a particular string.")
        text("Here are some examples:")
        text(trainingSet)
        text("Here is the user input: $input")
        text("Generate output without additional string.")
    }

    val response = runBlocking {
        generativeModel.generateContent(prompt)
    }
    return response.text!!.trim()
}
