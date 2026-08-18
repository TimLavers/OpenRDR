package io.rippledown.kb

import io.rippledown.model.rule.ValueExpression
import io.rippledown.persistence.DerivedDefinitionStore

/**
 * Manages the value-expression definitions of derived attributes, keyed by
 * attribute id, mirroring ConclusionManager. Editing a definition is an
 * in-place [store]: every rule whose action assigns the attribute by
 * definition picks up the change, with no rule mutation. See
 * documentation/design/editing_derived_attribute_definitions.md.
 */
class DerivedDefinitionManager(private val definitionStore: DerivedDefinitionStore) {
    private val definitions = definitionStore.all().toMutableMap()

    fun definitionFor(attributeId: Int): ValueExpression? = definitions[attributeId]

    fun store(attributeId: Int, expression: ValueExpression) {
        definitionStore.store(attributeId, expression)
        definitions[attributeId] = expression
    }

    fun all(): Map<Int, ValueExpression> = definitions.toMap()
}
