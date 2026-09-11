package io.rippledown.constants.chat

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ConstantsTest {
    @Test
    fun `demonstration action messages`() {
        // Given
        val name = "Zoo2"
        val title = "Zoo Animals"

        // When
        val messages = listOf(
            kbNameReservedMessage(title),
            kbCopiedFromDemonstrationMessage(name, title),
            nameForDemonstrationCopyMessage(title),
            cannotDeleteDemonstrationMessage(title)
        )

        // Then
        messages shouldBe listOf(
            "\"Zoo Animals\" is the name of a demonstration knowledge base; please choose another.",
            "Created \"Zoo2\" from the Zoo Animals demonstration and opened it.",
            "You will get your own copy of the Zoo Animals demonstration. What would you like to call it?",
            "Zoo Animals is a demonstration knowledge base and cannot be deleted. Your own copies can be."
        )
    }

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

    @Test
    fun `greeting with no stored KBs and demonstrations mentions creating and opening a demonstration`() {
        // Given
        val demonstrations =
            listOf("Contact Lense Prescription", "Pathology", "Thyroid Stimulating Hormone", "Zoo Animals")

        // When
        val greeting = noKbGreeting(emptyList(), demonstrations)

        // Then
        greeting shouldBe "There are no knowledge bases yet. Do you want to create one, or open a demonstration knowledge base? " +
                "The demonstration knowledge bases are: Contact Lense Prescription, Pathology, Thyroid Stimulating Hormone, Zoo Animals."
    }

    @Test
    fun `greeting with stored KBs and demonstrations lists both before the question`() {
        // Given
        val available = listOf("Glucose", "Thyroids")
        val demonstrations =
            listOf("Contact Lense Prescription", "Pathology", "Thyroid Stimulating Hormone", "Zoo Animals")

        // When
        val greeting = noKbGreeting(available, demonstrations)

        // Then
        greeting shouldBe "No knowledge base is open. The knowledge bases are:\nGlucose\nThyroids\n. " +
                "The demonstration knowledge bases are: Contact Lense Prescription, Pathology, Thyroid Stimulating Hormone, Zoo Animals. " +
                "Do you want to open one or create a new one?"
    }

    @Test
    fun `greeting with no stored KBs and no demonstrations preserves old text`() {
        // When
        val greeting = noKbGreeting(emptyList())

        // Then
        greeting shouldBe "There are no knowledge bases yet. Do you want to create one?"
    }

    @Test
    fun `greeting with stored KBs and no demonstrations preserves old text`() {
        // Given
        val available = listOf("Glucose", "Thyroids")

        // When
        val greeting = noKbGreeting(available)

        // Then
        greeting shouldBe "No knowledge base is open. The knowledge bases are:\nGlucose\nThyroids\n. " +
                "Do you want to open one or create a new one?"
    }
}
