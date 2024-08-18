package io.rippledown.model.rule

import io.rippledown.model.*

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