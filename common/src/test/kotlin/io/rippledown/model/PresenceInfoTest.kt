package io.rippledown.model

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import org.junit.jupiter.api.Test

class PresenceInfoTest {
    @Test
    fun `presence retains the KB identity and editing flags through serialization`() {
        // Given
        val presence = PresenceInfo("clinic", listOf(PresentUser("Alice", true), PresentUser("Bob", false)))

        // When
        val restored = serializeDeserialize(presence)

        // Then
        restored shouldBe presence
    }

    @Test
    fun `empty presence survives serialization`() {
        // Given
        val presence = PresenceInfo("clinic", emptyList())

        // When
        val restored = serializeDeserialize(presence)

        // Then
        restored shouldBe presence
    }
}
