package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetKnowledgeBaseDescriptionTest : KbActionTestBase() {

    @Test
    fun `replaces the stored description verbatim`() = runTest {
        // given
        val description = "# Thyroids\n\nClinical guidance."
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.setDescription(description) } returns Unit

        // when
        val outcome = SetKnowledgeBaseDescription(description).doIt(kbService)

        // then
        outcome.text() shouldBe "Description of \"Thyroids\" updated."
        verify(exactly = 1) { kbService.setDescription(description) }
    }

    @Test
    fun `requires an open knowledge base`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns null

        // when
        val outcome = SetKnowledgeBaseDescription("Text").doIt(kbService)

        // then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
        verify(exactly = 0) { kbService.setDescription(any()) }
    }

    @Test
    fun `setting the description does not change the chat context`() {
        // given / when / then
        SetKnowledgeBaseDescription("Text").changesContext shouldBe false
    }
}
