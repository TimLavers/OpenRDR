package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.rippledown.constants.chat.confirmKbCreateMessage
import io.rippledown.constants.chat.kbAlreadyExistsMessage
import io.rippledown.constants.chat.kbCreatedMessage
import io.rippledown.kb.KbResolution
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CreateKnowledgeBaseTest : KbActionTestBase() {

    @Test
    fun `a new name is created at once`() = runTest {
        // Given
        val lipids = KBInfo("lipids_1", "Lipids")
        every { kbService.resolve("Lipids") } returns KbResolution.NotFound("Lipids", listOf("Thyroids"))
        every { kbService.nearDuplicateOf("Lipids") } returns null
        coEvery { kbService.create("Lipids") } returns lipids

        // When
        val outcome = CreateKnowledgeBase("Lipids").doIt(kbService)

        // Then
        outcome.text() shouldBe kbCreatedMessage("Lipids")
    }

    @Test
    fun `the name is trimmed`() = runTest {
        // Given
        every { kbService.resolve("Lipids") } returns KbResolution.NotFound("Lipids", emptyList())
        every { kbService.nearDuplicateOf("Lipids") } returns null
        coEvery { kbService.create("Lipids") } returns KBInfo("lipids_1", "Lipids")

        // When
        val outcome = CreateKnowledgeBase("  Lipids ").doIt(kbService)

        // Then
        outcome.text() shouldBe kbCreatedMessage("Lipids")
        coVerify(exactly = 1) { kbService.create("Lipids") }
    }

    @Test
    fun `a blank name is refused`() = runTest {
        // When
        val outcome = CreateKnowledgeBase("   ").doIt(kbService)

        // Then
        outcome.text() shouldBe CreateKnowledgeBase.BLANK_NAME_MESSAGE
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `an existing name, ignoring case, is refused`() = runTest {
        // Given
        every { kbService.resolve("thyroids") } returns KbResolution.Exact(thyroids)

        // When
        val outcome = CreateKnowledgeBase("thyroids").doIt(kbService)

        // Then
        outcome.text() shouldBe kbAlreadyExistsMessage("Thyroids")
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `a near duplicate asks first and creates on acceptance`() = runTest {
        // Given
        val thyroid = KBInfo("thyroid_1", "Thyroid")
        every { kbService.resolve("Thyroid") } returns KbResolution.Partial(thyroids)
        every { kbService.nearDuplicateOf("Thyroid") } returns thyroids
        coEvery { kbService.create("Thyroid") } returns thyroid

        // When
        val ask = CreateKnowledgeBase("Thyroid").doIt(kbService).asAsk()

        // Then
        ask.question shouldBe confirmKbCreateMessage("Thyroid", "Thyroids")
        coVerify(exactly = 0) { kbService.create(any()) }

        // When the user accepts
        val response = ask.accept()

        // Then
        response shouldBe ChatResponse(kbCreatedMessage("Thyroid"))
        coVerify(exactly = 1) { kbService.create("Thyroid") }
    }

    @Test
    fun `creating changes the context`() {
        CreateKnowledgeBase("x").changesContext shouldBe true
    }
}
