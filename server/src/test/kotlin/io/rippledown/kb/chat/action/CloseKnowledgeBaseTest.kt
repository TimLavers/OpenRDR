package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbClosedMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CloseKnowledgeBaseTest : KbActionTestBase() {

    @Test
    fun `the open knowledge base is closed`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids
        coEvery { kbService.close() } just Runs

        // When
        val outcome = CloseKnowledgeBase().doIt(kbService)

        // Then
        outcome.text() shouldBe kbClosedMessage("Thyroids")
        coVerify(exactly = 1) { kbService.close() }
    }

    @Test
    fun `nothing to close`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = CloseKnowledgeBase().doIt(kbService)

        // Then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
        coVerify(exactly = 0) { kbService.close() }
    }

    @Test
    fun `closing changes the context`() {
        CloseKnowledgeBase().changesContext shouldBe true
    }
}
