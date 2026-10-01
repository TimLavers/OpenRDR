package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteCaseFromListTest : ActionTestBase() {

    @Test
    fun `deletes the current case from its user-defined list`() = runTest {
        // Given the current case
        val case = mockk<ViewableCase>()

        // When the action is performed
        val response = DeleteCaseFromList().doIt(ruleService, case, modelResponder)

        // Then the case is deleted from its list
        coVerify { ruleService.deleteCaseFromUserList(case) }
        response.text shouldBe "case deleted"
    }

    @Test
    fun `returns the server's refusal when the case is not in a user-defined list`() = runTest {
        // Given the server refuses the deletion
        val case = mockk<ViewableCase>()
        val refusal = "Cannot delete the case as the \"Processed\" list is built in."
        every { ruleService.deleteCaseFromUserList(case) } throws IllegalArgumentException(refusal)

        // When the action is performed
        val response = DeleteCaseFromList().doIt(ruleService, case, modelResponder)

        // Then the refusal is the chat response
        response.text shouldBe refusal
    }

    @Test
    fun `reports when there is no current case`() = runTest {
        // Given no current case
        // When the action is performed
        val response = DeleteCaseFromList().doIt(ruleService, null, modelResponder)

        // Then the response says that no case is selected
        response.text shouldBe "No case is selected."
    }
}
