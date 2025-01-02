package io.rippledown.conditiongenerator

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ConditionSpecification(
    val predicate: FunctionSpecification = FunctionSpecification(),
    var signature: FunctionSpecification = FunctionSpecification()
) {

    override fun toString() = toJson(this)
}

@Serializable
data class FunctionSpecification(val name: String = "", val parameters: List<String> = listOf())

fun spec(
    predicateName: String,
    predicateParameters: List<String> = listOf(),
    signatureName: String,
    signatureParameters: List<String> = listOf()
) =
    ConditionSpecification(
        FunctionSpecification(predicateName, predicateParameters),
        FunctionSpecification(signatureName, signatureParameters)
    )

fun fromJson(json: String) = Json.decodeFromString<ConditionSpecification>(json)

fun toJson(conditionStructure: ConditionSpecification) = Json.encodeToString(conditionStructure)
