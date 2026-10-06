package io.rippledown.constants.main

import io.kotest.matchers.shouldBe
import io.rippledown.model.UserId
import org.junit.jupiter.api.Test

class WindowTitleTest {
    @Test
    fun `window titles distinguish users`() {
        // Given
        val users = listOf(UserId("Alice"), UserId("Bob"))

        // When
        val titles = users.map(::windowTitle)

        // Then
        titles shouldBe listOf("OpenRDR — Alice", "OpenRDR — Bob")
    }

    @Test
    fun `window title preserves the full user identity`() {
        // Given
        val userId = UserId("Élodie Smith (lab-2)")

        // When
        val title = windowTitle(userId)

        // Then
        title shouldBe "OpenRDR — Élodie Smith (lab-2)"
    }
}
