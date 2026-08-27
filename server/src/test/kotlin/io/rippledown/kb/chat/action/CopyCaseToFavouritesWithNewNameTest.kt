package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CopyCaseToFavouritesWithNewNameTest : ActionTestBase() {
    @Test
    fun `copies case with new name`() = runTest {
        val caseId = 1000L
        val newName = "New name"
        val case = mockk<ViewableCase>()
        every { case.id } returns caseId
        CopyCaseToFavouritesWithNewName(newName).doIt(ruleService, case, modelResponder)
        coVerify { ruleService.copyCaseToFavourites(case, newName) }
    }
}