package io.rippledown.model.condition.tabular

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.RDCondition
import io.rippledown.model.condition.tabular.chain.ChainPredicate
import io.rippledown.model.condition.tabular.predicate.TestResultPredicate
import kotlinx.serialization.Serializable

// ORD1
@Serializable
class TabularCondition(override val id: Int? = null,
                       val attribute: Attribute,
                       val predicate: TestResultPredicate,
                       val chainPredicate: ChainPredicate): RDCondition() {

    override fun holds(case: RDRCase): Boolean {
        val values = case.values(attribute) ?: return false
        return chainPredicate.matches(values.map { predicate.evaluate(it) })
    }

    override fun asText() = "${chainPredicate.description()} ${attribute.name} ${predicate.description(chainPredicate.plurality())}".trim()

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = TabularCondition(id, idToAttribute(attribute.id), predicate, chainPredicate)

    override fun sameAs(other: RDCondition): Boolean {
        TODO("Not yet implemented")
    }
}