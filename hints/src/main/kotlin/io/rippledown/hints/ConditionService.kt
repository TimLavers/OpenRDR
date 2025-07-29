package io.rippledown.hints

import io.rippledown.hints.ConditionSpecification.Companion.decode
import io.rippledown.llm.generativeModel
import io.rippledown.llm.retry
import io.rippledown.log.lazyLogger
import kotlinx.coroutines.runBlocking

object ConditionService {
    private val logger = lazyLogger
    private const val RESOURCE_DIR = "/prompt"
    const val EPISODIC_PREDICATES = "EPISODIC_PREDICATES"
    const val SERIES_PREDICATES = "SERIES_PREDICATES"
    const val CASE_STRUCTURE_PREDICATES = "CASE_STRUCTURE_PREDICATES"
    const val EPISODIC_SIGNATURES = "EPISODIC_SIGNATURES"
    const val SINGLE_EXPRESSION_EXAMPLES = "SINGLE_EXPRESSION_EXAMPLES"
    const val MULTIPLE_EXPRESSION_EXAMPLES = "MULTIPLE_EXPRESSION_EXAMPLES"
    const val PROMPT_TEMPLATE = "PROMPT_TEMPLATE"
    const val INPUT_EXPRESSIONS = "INPUT_EXPRESSIONS"

    private fun readResource(resourceKey: String): String {
        val path = "$RESOURCE_DIR/${resourceKey.toResourceFileName()}"
        return (ConditionService::class.java.getResource(path)
            ?: throw IllegalArgumentException("Resource file not found: $path")).readText()
    }

    private fun String.toResourceFileName() = lowercase() + ".txt"

    private fun examples() = examplesFrom(readResource(SINGLE_EXPRESSION_EXAMPLES).split("\n"))

    fun promptFor(vararg inputExpressions: String): String {
        val promptVariables: Map<String, String> = mapOf(
            EPISODIC_PREDICATES to readResource(EPISODIC_PREDICATES),
            EPISODIC_SIGNATURES to readResource(EPISODIC_SIGNATURES),
            SERIES_PREDICATES to readResource(SERIES_PREDICATES),
            CASE_STRUCTURE_PREDICATES to readResource(CASE_STRUCTURE_PREDICATES),
            SINGLE_EXPRESSION_EXAMPLES to examples(),
//            MULTIPLE_EXPRESSION_EXAMPLES to readResource(MULTIPLE_EXPRESSION_EXAMPLES),
            INPUT_EXPRESSIONS to inputExpressions.joinToString(separator = "\n")
        )
        val templateText = readResource(PROMPT_TEMPLATE)
        return templateText.replacePlaceholders(promptVariables)
    }

    fun conditionSpecificationsFor(vararg inputExpressions: String): List<ConditionSpecification> {
        val prompt = promptFor(*inputExpressions)
        logger.info("---START PROMPT---\n$prompt\n---END PROMPT---\n")
        val response = runBlocking {
            retry {
                generativeModel()
                    .generateContent(prompt)
                    .text
            }
        }
        logger.info("\n---START RESPONSE---\n$response\n---END RESPONSE---")
        return response?.let { decode(it) } ?: emptyList()
    }
}

fun String.replacePlaceholders(placeholders: Map<String, String>) =
    placeholders.entries.fold(this) { result, (key, value) ->
        result.replace("{{$key}}", value)
    }
