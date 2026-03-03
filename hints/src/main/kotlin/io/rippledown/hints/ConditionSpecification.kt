package io.rippledown.hints

import io.rippledown.json
import io.rippledown.stripEnclosingJson
import kotlinx.serialization.Serializable

/**
 * This is the output of the model in response to a prompt to transform a user expression into a condition.
 * It contains all the information needed to create a condition, including the attribute name, function name and parameters.
 * Note: only single attribute conditions are supported at the moment.
 */
@Serializable
data class ConditionSpecification(
    val userExpression: String,
    val attributeName: String?,
    val predicate: FunctionSpecification = FunctionSpecification(),
    var signature: FunctionSpecification = FunctionSpecification()
) {
    constructor(
        userExpression: String,
        attributeName: String?,
        predicateName: String,
        predicateParameters: List<String> = listOf(),
        signatureName: String,
        signatureParameters: List<String> = listOf()
    ) : this(
        userExpression.replace("\\r", ""),
        attributeName,
        predicate = FunctionSpecification(predicateName, predicateParameters),
        signature = FunctionSpecification(signatureName, signatureParameters)
    )

    override fun toString() = encode(this)

    companion object {
        fun decode(text: String): List<ConditionSpecification> {
            val stripped = text.stripEnclosingJson().replace("\\r", "")
            return json.decodeFromString<List<ConditionSpecification>>(stripped)
        }

        //This is only necessary for tests where we ask the model to transform several expressions
        //in the one chat. It sometimes responds by returning 2 duplicate JSON blocks for a single expression.
        //In production, we don't do that so we expect the model to always return a single JSON object.
        fun decodeOne(text: String): ConditionSpecification {
            val stripped = text.stripEnclosingJson().replace("\\r", "")
            val firstObject = extractFirstJsonObject(stripped)
            return json.decodeFromString<ConditionSpecification>(firstObject)
        }

        internal fun extractFirstJsonObject(text: String): String {
            val start = text.indexOf('{')
            if (start == -1) return text
            var depth = 0
            for (i in start until text.length) {
                when (text[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1)
                    }
                }
            }
            return text.substring(start)
        }

        fun encode(conditionStructure: ConditionSpecification) =
            json.encodeToString(conditionStructure)
    }
}