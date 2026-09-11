package io.rippledown.kb.chat.action

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.sample.SampleKB.ZOO
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class CopyDemonstrationKnowledgeBaseTest : KbActionTestBase() {
    @BeforeTest
    fun stubDemonstrationTitles() {
        every { kbService.isDemonstrationTitle(any()) } returns false
    }

    @Test
    fun `blank name is refused before any lookup`() = runTest {
        // Given
        val action = CopyDemonstrationKnowledgeBase(ZOO, "  ")

        // When
        val outcome = action.doIt(kbService)

        // Then
        outcome.text() shouldBe BLANK_NAME_MESSAGE
        verify(exactly = 0) { kbService.resolve(any()) }
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
    }

    @Test
    fun `reserved title is refused before checking stored names`() = runTest {
        // Given
        every { kbService.isDemonstrationTitle("pathology") } returns true

        // When
        val outcome = CopyDemonstrationKnowledgeBase(ZOO, " pathology ").doIt(kbService)

        // Then
        outcome.text() shouldBe kbNameReservedMessage("pathology")
        verify(exactly = 0) { kbService.resolve(any()) }
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
    }

    @Test
    fun `existing name is refused`() = runTest {
        // Given
        every { kbService.resolve("thyroids") } returns KbResolution.Exact(thyroids)

        // When
        val outcome = CopyDemonstrationKnowledgeBase(ZOO, "thyroids").doIt(kbService)

        // Then
        outcome.text() shouldBe kbAlreadyExistsMessage("Thyroids")
        verify(exactly = 0) { kbService.nearDuplicateOf(any()) }
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
    }

    @Test
    fun `near duplicate waits for acceptance before copying`() = runTest {
        // Given
        val created = KBInfo("thyroid_2", "Thyroid")
        every { kbService.resolve("Thyroid") } returns KbResolution.Partial(thyroids)
        every { kbService.nearDuplicateOf("Thyroid") } returns thyroids
        coEvery { kbService.createFromSample("Thyroid", ZOO) } returns created

        // When
        val ask = CopyDemonstrationKnowledgeBase(ZOO, " Thyroid ").doIt(kbService).asAsk()

        // Then
        ask.question shouldBe confirmKbCreateMessage("Thyroid", "Thyroids")
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }

        // When
        val response = ask.accept()

        // Then
        response shouldBe ChatResponse(kbCopiedFromDemonstrationMessage("Thyroid", "Zoo Animals"))
        coVerify(exactly = 1) { kbService.createFromSample("Thyroid", ZOO) }
    }

    @Test
    fun `new trimmed name creates a copy and reports its returned name`() = runTest {
        // Given
        every { kbService.resolve("Zoo2") } returns KbResolution.NotFound("Zoo2", emptyList())
        every { kbService.nearDuplicateOf("Zoo2") } returns null
        coEvery { kbService.createFromSample("Zoo2", ZOO) } returns KBInfo("zoo_2", "Zoo2")

        // When
        val outcome = CopyDemonstrationKnowledgeBase(ZOO, " Zoo2 ").doIt(kbService)

        // Then
        outcome.text() shouldBe "Created \"Zoo2\" from the Zoo Animals demonstration and opened it."
        coVerify(exactly = 1) { kbService.createFromSample("Zoo2", ZOO) }
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `creation failure propagates without reporting success`() = runTest {
        // Given
        every { kbService.resolve("Zoo2") } returns KbResolution.NotFound("Zoo2", emptyList())
        every { kbService.nearDuplicateOf("Zoo2") } returns null
        val failure = IllegalStateException("Cannot create")
        coEvery { kbService.createFromSample("Zoo2", ZOO) } throws failure

        // When
        val thrown = shouldThrow<IllegalStateException> {
            CopyDemonstrationKnowledgeBase(ZOO, "Zoo2").doIt(kbService)
        }

        // Then
        thrown shouldBe failure
    }

    @Test
    fun `copying changes the context`() {
        // Given
        val action = CopyDemonstrationKnowledgeBase(ZOO, "Zoo2")

        // When
        val changesContext = action.changesContext

        // Then
        changesContext shouldBe true
    }
}
