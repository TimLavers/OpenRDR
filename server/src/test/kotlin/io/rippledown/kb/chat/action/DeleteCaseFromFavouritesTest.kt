package io.rippledown.kb.chat.action

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteCaseFromFavouritesTest : ActionTestBase() {
    @Test
    fun `copies case with null name`() = runTest {
        val caseId = 1000L
        val case = mockk<ViewableCase>()
        every { case.id } returns caseId
        CopyCaseToFavourites().doIt(ruleService, case, modelResponder)
        coVerify { ruleService.copyCaseToFavourites(case, null) }
    }
}