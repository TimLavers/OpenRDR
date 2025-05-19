package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.condition.tr
import io.rippledown.utils.daysAfter
import io.rippledown.utils.defaultDate

fun case(vararg pairs: Pair<Attribute, String>, name: String = "SessionCase"): RDRCase {
    val builder = RDRCaseBuilder()
    pairs.forEach {
        builder.addValue(it.first, defaultDate, it.second)
    }
    return builder.build(name)
}
fun makeCase(vararg pairs: Pair<Attribute, TestResult>, name: String = "SessionCase"): RDRCase {
    val builder = RDRCaseBuilder()
    pairs.forEach {
        builder.addResult(it.first, defaultDate, it.second)
    }
    return builder.build(name)
}
fun multiEpisodeCase(attribute: Attribute, vararg values: String): RDRCase {
    val builder = RDRCaseBuilder()
    values.forEachIndexed { index, s ->
        builder.addResult(attribute, daysAfter(index), tr(s))
    }
    return builder.build("Some Case")
}