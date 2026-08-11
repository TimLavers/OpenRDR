package io.rippledown.persistence

import io.rippledown.model.rule.ValueExpression
import kotlinx.serialization.json.Json

/**
 * Persists the value-expression definition of each derived attribute,
 * keyed by attribute id. Rules whose action is `ByDefinition` resolve
 * against these definitions, so an in-place [store] applies everywhere the
 * attribute is given by its definition — the editing primitive, mirroring
 * ConclusionStore.store. See
 * documentation/design/editing_derived_attribute_definitions.md.
 *
 * The interface is deliberately kind-agnostic (any attribute id maps to a
 * definition) so that Phase 2 of repeat inferencing can fold comment
 * definitions into the same store.
 */
interface DerivedDefinitionStore {
    fun all(): Map<Int, ValueExpression>
    fun definitionFor(attributeId: Int): ValueExpression?
    fun store(attributeId: Int, expression: ValueExpression)
    fun load(definitions: Map<Int, ValueExpression>)
}

fun expressionToString(expression: ValueExpression): String = Json.encodeToString(expression)

fun expressionFromString(string: String): ValueExpression = Json.decodeFromString(string)
