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
        response.text shouldContain "import a knowledge base from a ZIP archive (no knowledge base needs to be open)"
        response.text shouldContain "export the open knowledge base to a ZIP archive"
        response.text shouldContain "your own named copy of a demonstration knowledge base"
        response.text shouldContain "show or change a knowledge base description"
        response.text shouldContain "demonstration case"
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
            "User-defined case lists"
        )
        response.text shouldContain "edit a derived attribute's definition everywhere it is used (without building a rule)"
        response.text shouldContain "add, list or remove reasons in the current rule"
        response.text shouldContain "rename a condition's phrase everywhere it is used"
        response.text shouldContain "copy the current case to a named case list, optionally with a new case name"
        response.text shouldContain "delete the current case from its case list"
        response.capabilities.forEach { section ->
            response.text shouldContain section.heading
            section.items.forEach { response.text shouldContain it }
        }
    }
}
