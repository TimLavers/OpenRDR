package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.demoCaseAddedMessage
import io.rippledown.model.RDRCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AddDemonstrationCaseTest : KbActionTestBase() {

    private fun caseNamed(name: String): RDRCase {
        val case = mockk<RDRCase>()
        every { case.name } returns name
        return case
    }

    @Test
    fun `the demonstration case is added to the open knowledge base`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids
        coEvery { kbService.addDemonstrationCase() } returns caseNamed("Einstein")

        // When
        val outcome = AddDemonstrationCase().doIt(kbService)

        // Then
        outcome.text() shouldBe demoCaseAddedMessage("Einstein")
    }




    @Test
    fun `no open knowledge base`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = AddDemonstrationCase().doIt(kbService)

        // Then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
        coVerify(exactly = 0) { kbService.addDemonstrationCase() }
    }

    @Test
    fun `adding a case does not change the context, the client does that when the case arrives`() {
        AddDemonstrationCase().changesContext shouldBe false
    }
}
