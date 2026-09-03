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
class DerivedDefinitionManager(
    private val definitionStore: DerivedDefinitionStore,
    private val attributeProvider: AttributeProvider
) {
    private val definitions = definitionStore.all().mapValues { aligned(it.value) }.toMutableMap()

    fun definitionFor(attributeId: Int): ValueExpression? = definitions[attributeId]

    fun store(attributeId: Int, expression: ValueExpression) {
        definitionStore.store(attributeId, expression)
        definitions[attributeId] = expression
    }

    fun all(): Map<Int, ValueExpression> = definitions.toMap()

    /**
     * The stored expression with its attributes replaced by those held by the
     * attribute manager, so that a definition stored before an attribute was
     * renamed reads with the attribute's current name. A missing attribute is
     * inconsistent persisted state and prevents the knowledge base loading.
     */
    private fun aligned(expression: ValueExpression) =
        expression.alignAttributes { id -> attributeProvider.getById(id) }
}
