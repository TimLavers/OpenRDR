package io.rippledown.model.condition.structural

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data object IsSingleEpisodeCase: CaseStructurePredicate {
    override fun evaluate(case: RDRCase) = case.numberOfEpisodes() == 1
    override fun description() = "case is for a single date"
    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = this
}