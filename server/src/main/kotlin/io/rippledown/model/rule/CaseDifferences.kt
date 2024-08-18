package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase

class CaseDifferences(private val sessionCase: RDRCase, private val conflictingCase: RDRCase) {
    fun valuesFor(attribute: Attribute): Pair<String?, String?> {
        return Pair(sessionCase.latestValue(attribute), conflictingCase.latestValue(attribute))
    }
}