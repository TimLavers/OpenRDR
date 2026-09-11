package io.rippledown.constants.chat

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ConstantsTest {
    @Test
    fun `not found message lists stored knowledge bases and demonstrations`() {
        // Given
        val available = listOf("Glucose", "Thyroids")
        val demonstrations = listOf("Pathology", "Zoo Animals")

        // When
        val message = kbNotFoundMessage("Nothing", available, demonstrations)

        // Then
        message shouldBe "There is no knowledge base named \"Nothing\". The knowledge bases are: Glucose, Thyroids. The demonstration knowledge bases are: Pathology, Zoo Animals."
    }

    @Test
    fun `not found message lists demonstrations when no stored knowledge bases exist`() {
        // Given
        val demonstrations = listOf("Pathology")

        // When
        val message = kbNotFoundMessage("Nothing", emptyList(), demonstrations)

        // Then
        message shouldBe "There is no knowledge base named \"Nothing\". There are no knowledge bases. The demonstration knowledge bases are: Pathology."
    }

    @Test
    fun `not found message without demonstrations preserves stored names message`() {
        // Given
        val available = listOf("Thyroids")

        // When
        val message = kbNotFoundMessage("Nothing", available, emptyList())

        // Then
        message shouldBe "There is no knowledge base named \"Nothing\". The knowledge bases are: Thyroids."
    }

    @Test
    fun `not found message without knowledge bases preserves empty message`() {
        // Given
        val available = emptyList<String>()

        // When
        val message = kbNotFoundMessage("Nothing", available)

        // Then
        message shouldBe "There is no knowledge base named \"Nothing\". There are no knowledge bases."
    }
}
