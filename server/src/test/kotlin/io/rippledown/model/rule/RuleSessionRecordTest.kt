package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RuleSessionRecordTest {
    @Test
    fun idsStringTest() {
        val idsSet = setOf(1, 59, 39, 100)
        val str = RuleSessionRecord(23, 1, idsSet).idsString()
        parseToIds(str) shouldBe idsSet
    }

    @Test
    fun `one rule id only`() {
        val idsSet = setOf(100)
        val str = RuleSessionRecord(45,1, idsSet).idsString()
        parseToIds(str) shouldBe idsSet
    }
}