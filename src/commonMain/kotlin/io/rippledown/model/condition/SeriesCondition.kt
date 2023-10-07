package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.series.SeriesPredicate
import kotlinx.serialization.Serializable

// ORD1
@Serializable
data class SeriesCondition(override val id: Int? = null,
                           val attribute: Attribute,
                           val seriesPredicate: SeriesPredicate
): Condition() {
    override fun holds(case: RDRCase): Boolean {
        val values = case.values(attribute) ?: return false
        return seriesPredicate.evaluate(values)
    }

    override fun asText() = seriesPredicate.description(attribute.name)

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = SeriesCondition(id, idToAttribute(attribute.id), seriesPredicate)

    override fun sameAs(other: Condition): Boolean {
        return if (other is SeriesCondition) {
            other.attribute.isEquivalent(attribute) && other.seriesPredicate == seriesPredicate
        } else false
    }
}