package io.rippledown.kb

import io.rippledown.model.condition.Condition
import io.rippledown.persistence.ConditionStore

class ConditionManager(private val attributeManager: AttributeManager, private val conditionStore: ConditionStore) {
    private val idToCondition = mutableMapOf<Int, Condition>()

    init {
        conditionStore.all().forEach {
            idToCondition[it.id!!] = it.alignAttributes(::attributeForId)
        }
    }

    fun getOrCreate(condition: Condition): Condition {
        require(condition.id == null) {
            "Cannot store a condition that has a non-null id."
        }
        val existing = idToCondition.values.firstOrNull {
            condition.sameAs(it)
        }
        return if (existing != null) existing else {
            val created = conditionStore.create(condition)
            val aligned = created.alignAttributes( ::attributeForId )
            idToCondition[aligned.id!!] = aligned
            aligned
        }
    }

    fun getById(id: Int): Condition? = idToCondition[id]

    fun all() = idToCondition.values.toSet()

    private fun attributeForId(id: Int) = attributeManager.getById(id)
}