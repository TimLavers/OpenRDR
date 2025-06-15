package io.rippledown.model.condition.episodic.predicate

fun isOrAre(plural: Boolean) = if (plural) "are" else "is"

fun checkIsReasonablePercentage(value: Int) {
    require(value in 0..100) {
        "Value should be an integer in the range [0, 100]."
    }
}

fun String.unquoted() = removeSurrounding("\"").removeSurrounding("'")
