package io.rippledown.standalone

import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class StandAloneInterpreterTest: StandAloneInterpreterTestBase() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun `handle empty map`() {
        interpreter.interpretStringMap(emptyMap()) shouldBe ""
    }

    @Test
    fun `single comment returned`() {
        val inputs = mapOf(a.name to valueBlah, b.name to "ignored", c.name to "also ignored")
        val interpretation = interpreter.interpretStringMap(inputs)
        interpretation shouldBe COMMENT_1
    }

    @Test
    fun `multiple comments returned`() {
        val caseCountBefore = kb.allProcessedCases().size
        val inputs = mapOf(a.name to valueBlah, b.name to "ignored", c.name to valueSuch)
        val interpretation = interpreter.interpretStringMap(inputs)
        interpretation shouldBe COMMENT_3 + "\n" + COMMENT_1
        // No new cases should have been stored in the KB.
        kb.allProcessedCases().size shouldBe caseCountBefore
    }

    @Test
    fun `unknown attribute name`() {
        val inputs = mapOf("unknown" to valueBlah)
        val interpretation = interpreter.interpretStringMap(inputs)
        interpretation shouldBe ""
    }

    @Test
    fun `attribute matching is case-sensitive`() {
        val inputs = mapOf(a.name.lowercase() to valueBlah, b.name to valueWhatever)
        val interpretation = interpreter.interpretStringMap(inputs)
        interpretation shouldBe COMMENT_2
    }

    @Test
    fun `multiple comments are sorted`() {
        val inputs = mapOf(a.name to valueBlah, b.name to valueWhatever, c.name to valueSuch)
        val interpretation = interpreter.interpretStringMap(inputs)
        interpretation shouldBe "$COMMENT_2\n$COMMENT_3\n$COMMENT_1"
    }

    @Test
    fun `no matching rules`() {
        val inputs = mapOf(a.name to "something else")
        interpreter.interpretStringMap(inputs) shouldBe ""
    }
}