package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.demoCaseAddedMessage
import io.rippledown.kb.chat.DemonstrationCase
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
    fun `the pathology case is added to the open knowledge base`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids
        coEvery { kbService.addDemonstrationCase(DemonstrationCase.Pathology) } returns caseNamed("Einstein")

        // When
        val outcome = AddDemonstrationCase("pathology").doIt(kbService)

        // Then
        outcome.text() shouldBe demoCaseAddedMessage("Einstein")
    }

    @Test
    fun `the minimal case is added to the open knowledge base`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids
        coEvery { kbService.addDemonstrationCase(DemonstrationCase.Minimal) } returns caseNamed("Demo")

        // When
        val outcome = AddDemonstrationCase("Minimal").doIt(kbService)

        // Then
        outcome.text() shouldBe demoCaseAddedMessage("Demo")
    }

    @Test
    fun `an unknown kind is refused`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids

        // When
        val outcome = AddDemonstrationCase("surfing").doIt(kbService)

        // Then
        outcome.text() shouldBe AddDemonstrationCase.unknownKindMessage("surfing")
        coVerify(exactly = 0) { kbService.addDemonstrationCase(any()) }
    }

    @Test
    fun `no open knowledge base`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = AddDemonstrationCase("pathology").doIt(kbService)

        // Then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
        coVerify(exactly = 0) { kbService.addDemonstrationCase(any()) }
    }

    @Test
    fun `adding a case does not change the context, the client does that when the case arrives`() {
        AddDemonstrationCase("pathology").changesContext shouldBe false
    }
}
