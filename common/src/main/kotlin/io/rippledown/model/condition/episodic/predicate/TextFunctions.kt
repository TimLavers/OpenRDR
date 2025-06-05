package io.rippledown.model.condition.episodic.predicate

import io.rippledown.model.TestResult
import kotlinx.serialization.Serializable

@Serializable
data object IsNotBlank : TestResultPredicate {
    override fun evaluate(result: TestResult) = result.value.text.isNotBlank()

    override fun description(plural: Boolean) = if (plural) "are in case" else "is in case"
}

@Serializable
data object IsBlank : TestResultPredicate {
    override fun evaluate(result: TestResult) = result.value.text.isBlank()

    override fun description(plural: Boolean) = if (plural) "are blank" else "is blank"
}

@Serializable
data object IsNumeric : TestResultPredicate {
    override fun evaluate(result: TestResult) = result.value.real != null

    override fun description(plural: Boolean) = if (plural) "are numeric" else "is numeric"
}

@Serializable
data object IsNotNumeric : TestResultPredicate {
    override fun evaluate(result: TestResult) = result.value.real == null

    override fun description(plural: Boolean) = if (plural) "are not numeric" else "is not numeric"
}

@Serializable
data class Contains(val toFind: String) : TestResultPredicate {
    override fun evaluate(result: TestResult) = result.value.text.contains(toFind)

    override fun description(plural: Boolean) = if (plural) "contain \"$toFind\"" else "contains \"$toFind\""
}

@Serializable
data class DoesNotContain(val toFind: String) : TestResultPredicate {
    override fun evaluate(result: TestResult) = !result.value.text.contains(toFind)

    override fun description(plural: Boolean) =
        if (plural) "do not contain \"$toFind\"" else "does not contain \"$toFind\""
}

@Serializable
data class Is(val toFind: String) : TestResultPredicate {
    private val cleanedToFind: String = toFind
        .removeSurrounding("\"")
        .removeSurrounding("'")

    override fun evaluate(result: TestResult): Boolean {
        val cleanedResult = result.value.text
            .removeSurrounding("\"")
            .removeSurrounding("'")
        val bool = cleanedResult == cleanedToFind
        println("Evaluating Is predicate: '$cleanedResult' == '$cleanedToFind' : $bool")
        return bool
    }

    override fun description(plural: Boolean): String =
        if (plural) "are \"$cleanedToFind\"" else "is \"$cleanedToFind\""
}

@Serializable
data class IsNot(val toFind: String) : TestResultPredicate {
    override fun evaluate(result: TestResult) = result.value.text != toFind

    override fun description(plural: Boolean) = if (plural) "are not \"$toFind\"" else "is not \"$toFind\""
}