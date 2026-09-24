package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteCaseFromFavouritesTest : ActionTestBase() {
    @Test
    fun `deletes the case from its user-defined list`() = runTest {
        // Given the current case
        val caseId = 1000L
        val case = mockk<ViewableCase>()
        every { case.id } returns caseId

        // When the action is performed
        DeleteCaseFromFavourites().doIt(ruleService, case, modelResponder)

        // Then the case is deleted from its list
        coVerify { ruleService.deleteCaseFromUserList(case) }
    }
}
