package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CopyCaseToFavouritesTest : ActionTestBase() {
    @Test
    fun `copies case to the Favourites list with null name`() = runTest {
        // Given the current case
        val caseId = 1000L
        val case = mockk<ViewableCase>()
        every { case.id } returns caseId

        // When the action is performed
        CopyCaseToFavourites().doIt(ruleService, case, modelResponder)

        // Then the case is copied to the "Favourites" list, keeping its name
        coVerify { ruleService.copyCaseToList(case, "Favourites", null) }
    }
}
