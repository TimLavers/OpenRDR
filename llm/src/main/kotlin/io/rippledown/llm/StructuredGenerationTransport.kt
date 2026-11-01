package io.rippledown.llm

import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.ThinkingConfig
import com.google.genai.types.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

/**
 * One schema-constrained generation: no tools, no history, no grounding.
 * The system instruction and the user message are the whole prompt.
 */
data class StructuredGenerationRequest(
    val model: String,
    val temperature: Double,
    val systemInstruction: String,
    val userMessage: String
)

/**
 * What came back, reduced to the fields the classifier inspects. A blocked
 * prompt has a [blockReason] and no text; a stopped candidate has a
 * [finishReason] such as `STOP`, `MAX_TOKENS` or `SAFETY`.
 */
data class StructuredGenerationResponse(
    val text: String?,
    val finishReason: String?,
    val blockReason: String?,
    val modelVersion: String?
)

interface StructuredGenerationTransport {
    /**
     * Performs the request. Provider errors are thrown as the SDK's
     * exceptions; the classifier maps them to typed failures.
     */
    suspend fun generate(request: StructuredGenerationRequest): StructuredGenerationResponse
}

/**
 * Sends the request to Gemini asking for the Boolean response schema. The
 * blocking SDK call runs on the IO dispatcher and is interrupted if the
 * caller's attempt deadline cancels it.
 */
class GeminiStructuredTransport(private val client: Client = geminiClient) : StructuredGenerationTransport {

    override suspend fun generate(request: StructuredGenerationRequest): StructuredGenerationResponse {
        val config = GenerateContentConfig.builder()
            .temperature(request.temperature.toFloat())
            .candidateCount(1)
            .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build())
            .safetySettings(noSafetySettings())
            .systemInstruction(Content.fromParts(Part.fromText(request.systemInstruction)))
            .responseMimeType("application/json")
            .responseSchema(booleanResponseSchema)
            .build()
        val content = Content.fromParts(Part.fromText(request.userMessage))
        val response = runInterruptible(Dispatchers.IO) {
            geminiApiCallCount.incrementAndGet()
            client.models.generateContent(request.model, content, config)
        }
        val candidate = response.candidates().orElse(emptyList()).firstOrNull()
        val finishReason = candidate?.finishReason()?.map { it.toString() }?.orElse(null)
        return StructuredGenerationResponse(
            text = candidate?.content()?.flatMap { it.parts() }?.orElse(null)
                ?.mapNotNull { it.text().orElse(null) }?.joinToString(""),
            finishReason = finishReason,
            blockReason = response.promptFeedback().flatMap { it.blockReason() }.map { it.toString() }.orElse(null),
            modelVersion = response.modelVersion().orElse(null)
        )
    }

    private val booleanResponseSchema: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(
            mapOf(
                "value" to Schema.builder().type(Type.Known.BOOLEAN).build(),
                "evidence" to Schema.builder().type(Type.Known.STRING).nullable(true).build()
            )
        )
        .required("value", "evidence")
        .propertyOrdering("value", "evidence")
        .build()
}
