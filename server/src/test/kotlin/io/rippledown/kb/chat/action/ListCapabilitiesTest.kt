package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.kb.chat.ActionComment
import org.junit.jupiter.api.Test

class ListCapabilitiesTest {
    @Test
    fun `help without a case contains only KB operations and their prerequisites`() {
        // Given
        val action = ActionComment("ListCapabilities").createActionInstance().shouldBeInstanceOf<ListCapabilities>()

        // When
        val response = action.response(hasCase = false)

        // Then
        response.capabilities.map { it.heading } shouldBe listOf("Knowledge bases")
        response.text shouldContain "import a knowledge base from a ZIP archive"
        response.text shouldContain "export the open knowledge base to a ZIP archive"
        response.text shouldContain "your own named copy of a demonstration knowledge base"
        response.text shouldContain "show or change a knowledge base description"
        response.text shouldContain "demonstration case"
        response.text shouldContain "Import does not require an open knowledge base; export does."
        response.suggestions shouldBe emptyList()
        response.kbFileDialogRequest shouldBe null
    }

    @Test
    fun `help with a case contains the complete catalogue and a plain text equivalent`() {
        // Given
        val action = ListCapabilities()

        // When
        val response = action.response(hasCase = true)

        // Then
        response.capabilities.map { it.heading } shouldBe listOf(
            "Knowledge bases",
            "Report comments",
            "Derived attributes",
            "Building a rule",
            "Names and layout",
            "Favourite cases"
        )
        response.text shouldContain "edit a derived attribute's definition everywhere it is used (without building a rule)"
        response.text shouldContain "add, list or remove reasons in the current rule"
        response.text shouldContain "copy the current case to favourites, optionally with a new name"
        response.text shouldContain "delete the current case from favourites"
        response.capabilities.forEach { section ->
            response.text shouldContain section.heading
            section.items.forEach { response.text shouldContain it }
        }
    }
}
