package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CopyCaseToListTest : ActionTestBase() {

    @Test
    fun `copies the current case to the named list`() = runTest {
        // Given the current case
        val case = mockk<ViewableCase>()

        // When the action is performed with a list name only
        val response = CopyCaseToList("good").doIt(ruleService, case, modelResponder)

        // Then the case is copied to that list, keeping its name
        coVerify { ruleService.copyCaseToList(case, "good", null) }
        response.text shouldBe "case copied"
    }

    @Test
    fun `copies the current case to the named list with a new case name`() = runTest {
        // Given the current case and a new case name
        val case = mockk<ViewableCase>()
        val newName = "Great case!"

        // When the action is performed with a list name and the new case name
        val response = CopyCaseToList("good", newName).doIt(ruleService, case, modelResponder)

        // Then the case is copied to that list with the new name
        coVerify { ruleService.copyCaseToList(case, "good", newName) }
        response.text shouldBe "case copied"
    }

    @Test
    fun `returns the server's refusal when the list name is not allowed`() = runTest {
        // Given the server refuses the list name
        val case = mockk<ViewableCase>()
        val refusal = "Cannot copy the case to \"Cornerstone Cases\" as that is the name of a built-in list."
        every { ruleService.copyCaseToList(case, "Cornerstone Cases", null) } throws IllegalArgumentException(refusal)

        // When the action is performed
        val response = CopyCaseToList("Cornerstone Cases").doIt(ruleService, case, modelResponder)

        // Then the refusal is the chat response
        response.text shouldBe refusal
    }

    @Test
    fun `reports when there is no current case`() = runTest {
        // Given no current case
        // When the action is performed
        val response = CopyCaseToList("good").doIt(ruleService, null, modelResponder)

        // Then the response says that no case is selected
        response.text shouldBe "No case is selected."
    }
}
