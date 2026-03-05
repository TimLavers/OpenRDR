package io.rippledown.hints

/**
 * Interface for transforming user expressions into condition specifications.
 */
interface ConditionTransformer {
    fun setAttributeNames(attributeNames: List<String>)
    suspend fun transform(expression: String): ConditionSpecification?
}
