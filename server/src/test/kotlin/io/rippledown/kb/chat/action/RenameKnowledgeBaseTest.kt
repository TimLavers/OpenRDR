package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbAlreadyExistsMessage
import io.rippledown.constants.chat.kbRenamedMessage
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RenameKnowledgeBaseTest : KbActionTestBase() {

    @Test
    fun `renames the open knowledge base`() = runTest {
        // given
        val renamed = KBInfo(thyroids.id, "Thyroid Function")
        every { kbService.openKnowledgeBase() } returns thyroids
        coEvery { kbService.rename(renamed.name) } returns renamed

        // when
        val outcome = RenameKnowledgeBase(renamed.name).doIt(kbService)

        // then
        outcome.text() shouldBe kbRenamedMessage(thyroids.name, renamed.name)
        coVerify(exactly = 1) { kbService.rename(renamed.name) }
    }

    @Test
    fun `refuses a name already used by another knowledge base`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        coEvery { kbService.rename("glucose") } throws IllegalArgumentException("clash")

        // when
        val outcome = RenameKnowledgeBase("glucose").doIt(kbService)

        // then
        outcome.text() shouldBe kbAlreadyExistsMessage("glucose")
    }

    @Test
    fun `refuses a blank name`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids

        // when
        val outcome = RenameKnowledgeBase("   ").doIt(kbService)

        // then
        outcome.text() shouldBe "A knowledge base name cannot be blank."
        coVerify(exactly = 0) { kbService.rename(any()) }
    }

    @Test
    fun `requires an open knowledge base`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns null

        // when
        val outcome = RenameKnowledgeBase("New name").doIt(kbService)

        // then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
    }

    @Test
    fun `renaming does not change the chat context`() {
        // given / when / then
        RenameKnowledgeBase("New name").changesContext shouldBe false
    }
}
