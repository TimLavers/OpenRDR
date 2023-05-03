package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.util.shouldContainSameAs
import kotlin.test.Test

internal class ActionTest : RuleTestBase() {
    private val A = "A"
    private val B = "B"
    private val C = "C"

}