package io.rippledown.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UtilTest {

    @Test
    fun randomStringTest() {
        randomString(0) shouldBe ""

        val checker = Regex("[\\da-z]{8}")

        // Create 1000 random strings of length 8. Check that they
        // all match the pattern, and check that they are distinct.
        val created = mutableSetOf<String>()
        repeat(1000000) {
            val string = randomString(8)
            checker.matches(string) shouldBe true
            created.add(string)
        }
        created.size shouldBe 1000000
    }
}