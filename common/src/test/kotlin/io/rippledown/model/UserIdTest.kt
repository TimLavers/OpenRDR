package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UserIdTest {

    @Test
    fun `holds the value it was given`() {
        // Given
        val id = UserId("alice")

        // Then
        id.value shouldBe "alice"
        id.toString() shouldBe "alice"
    }

    @Test
    fun `equal when the values are equal`() {
        // Given
        val a = UserId("alice")
        val b = UserId("alice")

        // Then
        a shouldBe b
    }

    @Test
    fun `rejects a blank value`() {
        // When / Then
        shouldThrow<IllegalArgumentException> { UserId("") }
        shouldThrow<IllegalArgumentException> { UserId("   ") }
    }
}
