package io.rippledown.model.condition.tabular

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.RDCondition
import io.rippledown.model.condition.tabular.chain.ChainPredicate
import io.rippledown.model.condition.tabular.predicate.TestResultPredicate
import kotlinx.serialization.Serializable

// ORD1
@Serializable
class TabularCondition(override val id: Int? = null, val attribute: Attribute, val predicate: TestResultPredicate, val chainPredicate: ChainPredicate): RDCondition() {
    override fun holds(case: RDRCase): Boolean {
        TODO("Not yet implemented")
    }

    override fun asText(): String {
        TODO("Not yet implemented")
    }

    override fun alignAttributes(idToAttribute: (Int) -> Attribute): RDCondition {
        TODO("Not yet implemented")
    }

    override fun sameAs(other: RDCondition): Boolean {
        TODO("Not yet implemented")
    }
}