package io.rippledown.conditiongenerator

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ConditionSpecification(val predicate: FunctionSpecification, val signature: FunctionSpecification)

@Serializable
data class FunctionSpecification(val name: String, val parameters: List<String>)

fun fromJson(json: String) = Json.decodeFromString<ConditionSpecification>(json)
