package io.rippledown.persistence.inmemory

import io.rippledown.model.rule.ValueExpression
import io.rippledown.persistence.DerivedDefinitionStore

class InMemoryDerivedDefinitionStore : DerivedDefinitionStore {
    private val definitions = mutableMapOf<Int, ValueExpression>()

    override fun all() = definitions.toMap()

    override fun definitionFor(attributeId: Int) = definitions[attributeId]

    override fun store(attributeId: Int, expression: ValueExpression) {
        definitions[attributeId] = expression
    }

    override fun load(definitions: Map<Int, ValueExpression>) {
        require(this.definitions.isEmpty()) {
            "Cannot load definitions into a non-empty derived definition store."
        }
        this.definitions.putAll(definitions)
    }
}
