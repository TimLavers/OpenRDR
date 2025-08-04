package io.rippledown.hints

import io.rippledown.stripEnclosingJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class ConditionSpecification(
    val predicate: FunctionSpecification = FunctionSpecification(),
    var signature: FunctionSpecification = FunctionSpecification()
) {
    constructor(
        predicateName: String,
        predicateParameters: List<String> = listOf(),
        signatureName: String,
        signatureParameters: List<String> = listOf()
    ) : this(
        predicate = FunctionSpecification(predicateName, predicateParameters),
        signature = FunctionSpecification(signatureName, signatureParameters)
    )

    override fun toString() = encode(this)

    companion object {
        fun decode(json: String): List<ConditionSpecification> {
            val stripped = json.stripEnclosingJson().replace("\n", "")
            return Json.decodeFromString<List<ConditionSpecification>>(stripped)
        }

        fun encode(conditionStructure: ConditionSpecification) =
            Json.encodeToString(conditionStructure)
    }
}