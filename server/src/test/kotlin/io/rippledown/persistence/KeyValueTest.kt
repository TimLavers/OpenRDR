package io.rippledown.persistence

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class KeyValueTest {

    @Test
    fun `restrictions on key`() {
        val errorMessage = "Key must match [A-Za-z0-9_]+."
        shouldThrow<IllegalArgumentException> {
            KeyValue(100, "","whatever")
        }.message shouldBe errorMessage
        shouldThrow<IllegalArgumentException> {
            KeyValue(100, " blah","whatever")
        }.message shouldBe errorMessage
        shouldThrow<IllegalArgumentException> {
            KeyValue(100, "blah\nwhatever","whatever")
        }.message shouldBe errorMessage
    }
}