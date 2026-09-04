package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ShowKnowledgeBaseDescriptionTest : KbActionTestBase() {

    @Test
    fun `returns the stored description verbatim`() = runTest {
        // given
        val description = "# Thyroids\n\nClinical guidance."
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.description() } returns description

        // when
        val outcome = ShowKnowledgeBaseDescription().doIt(kbService)

        // then
        outcome.text() shouldBe description
    }

    @Test
    fun `reports an empty description`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.description() } returns ""

        // when
        val outcome = ShowKnowledgeBaseDescription().doIt(kbService)

        // then
        outcome.text() shouldBe "\"Thyroids\" has no description."
    }

    @Test
    fun `requires an open knowledge base`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns null

        // when
        val outcome = ShowKnowledgeBaseDescription().doIt(kbService)

        // then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
    }

    @Test
    fun `showing the description does not change the chat context`() {
        // given / when / then
        ShowKnowledgeBaseDescription().changesContext shouldBe false
    }
}
