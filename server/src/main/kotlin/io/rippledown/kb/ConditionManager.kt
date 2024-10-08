package io.rippledown.kb

import io.rippledown.model.ConditionFactory
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.predicate.IsNotBlank
import io.rippledown.persistence.ConditionStore

class ConditionManager(private val attributeManager: AttributeManager,
                       private val conditionStore: ConditionStore): ConditionFactory {
    private val idToCondition = mutableMapOf<Int, Condition>()

    init {
        conditionStore.all().forEach {
            idToCondition[it.id!!] = it.alignAttributes(::attributeForId)
        }
    }

    override fun getOrCreate(condition: Condition): Condition {
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