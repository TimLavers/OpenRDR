package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CopyCaseToFavouritesWithNewNameTest : ActionTestBase() {
    @Test
    fun `copies case to the Favourites list with new name`() = runTest {
        // Given the current case and a new name
        val caseId = 1000L
        val newName = "New name"
        val case = mockk<ViewableCase>()
        every { case.id } returns caseId

        // When the action is performed
        CopyCaseToFavouritesWithNewName(newName).doIt(ruleService, case, modelResponder)

        // Then the case is copied to the "Favourites" list with the new name
        coVerify { ruleService.copyCaseToList(case, "Favourites", newName) }
    }
}
