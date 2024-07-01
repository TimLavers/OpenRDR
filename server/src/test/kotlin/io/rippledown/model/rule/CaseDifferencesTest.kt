package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlin.test.Test

internal class CaseDifferencesTest {
    val a = Attribute(0, "A")
    val b = Attribute(1, "B")
    val c = Attribute(2, "C")
    val d = Attribute(3, "D")

    @Test
    fun valuesFor() {
        val sessionCase = case(a to "1", c to "3")
        val conflictingCase = case(a to "2", d to "4")
        val differences = CaseDifferences(sessionCase, conflictingCase)
        differences.valuesFor(a) shouldBe Pair("1", "2")
        differences.valuesFor(b) shouldBe Pair(null, null)
        differences.valuesFor(c) shouldBe Pair("3", null)
        differences.valuesFor(d) shouldBe Pair(null, "4")
    }
}