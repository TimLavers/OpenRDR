package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.model.chat.ChatResponse
import io.rippledown.sample.SampleKB.ZOO
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteKnowledgeBaseTest : KbActionTestBase() {

    @Test
    fun `unknown name includes demonstrations from the resolution`() = runTest {
        // Given
        val available = listOf("Thyroids")
        val demonstrations = listOf("Pathology", "Zoo Animals")
        every { kbService.resolve("Nothing") } returns KbResolution.NotFound("Nothing", available, demonstrations)

        // When
        val outcome = DeleteKnowledgeBase("Nothing").doIt(kbService)

        // Then
        outcome.text() shouldBe kbNotFoundMessage("Nothing", available, demonstrations)
        coVerify(exactly = 0) { kbService.delete(any()) }
    }

    @Test
    fun `a demonstration cannot be deleted`() = runTest {
        // Given
        every { kbService.resolve("Zoo") } returns KbResolution.Demonstration(ZOO)

        // When
        val outcome = DeleteKnowledgeBase("Zoo").doIt(kbService)

        // Then
        outcome.text() shouldBe "Zoo Animals is a demonstration knowledge base and cannot be deleted. Your own copies can be."
        coVerify(exactly = 0) { kbService.delete(any()) }
    }

    @Test
    fun `an exact match still asks, and deletes on acceptance`() = runTest {
        // Given
        every { kbService.resolve("Scratch") } returns KbResolution.Exact(scratch)
        coEvery { kbService.delete(scratch) } just Runs

        // When
        val ask = DeleteKnowledgeBase("Scratch").doIt(kbService).asAsk()

        // Then
        ask.question shouldBe confirmKbDeletionMessage("Scratch")
        coVerify(exactly = 0) { kbService.delete(any()) }

        // When the user accepts
        val response = ask.accept()

        // Then
        response shouldBe ChatResponse(kbDeletedMessage("Scratch"))
        coVerify(exactly = 1) { kbService.delete(scratch) }
    }

    @Test
    fun `a partial match asks naming the resolved knowledge base`() = runTest {
        // Given
        every { kbService.resolve("scrat") } returns KbResolution.Partial(scratch)

        // When
        val ask = DeleteKnowledgeBase("scrat").doIt(kbService).asAsk()

        // Then
        ask.question shouldBe confirmKbDeletionMessage("Scratch")
    }

    @Test
    fun `with no name the open knowledge base is meant`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.resolve("Thyroids") } returns KbResolution.Exact(thyroids)

        // When
        val ask = DeleteKnowledgeBase().doIt(kbService).asAsk()

        // Then
        ask.question shouldBe confirmKbDeletionMessage("Thyroids")
    }

    @Test
    fun `with no name and nothing open there is nothing to delete`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = DeleteKnowledgeBase().doIt(kbService)

        // Then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
    }

    @Test
    fun `an ambiguous name is put back to the user`() = runTest {
        // Given
        every { kbService.resolve("thyroid") } returns
                KbResolution.Ambiguous("thyroid", listOf("Thyroids", "Thyroids (old)"))

        // When
        val outcome = DeleteKnowledgeBase("thyroid").doIt(kbService)

        // Then
        outcome.text() shouldBe kbAmbiguousMessage("thyroid", listOf("Thyroids", "Thyroids (old)"))
    }

    @Test
    fun `an unknown name lists what there is`() = runTest {
        // Given
        every { kbService.resolve("Lipids") } returns KbResolution.NotFound("Lipids", listOf("Thyroids"))

        // When
        val outcome = DeleteKnowledgeBase("Lipids").doIt(kbService)

        // Then
        outcome.text() shouldBe kbNotFoundMessage("Lipids", listOf("Thyroids"))
    }

    @Test
    fun `deleting changes the context`() {
        DeleteKnowledgeBase("x").changesContext shouldBe true
    }
}
