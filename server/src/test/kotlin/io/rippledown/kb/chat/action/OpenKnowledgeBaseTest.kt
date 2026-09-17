package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.model.chat.ChatResponse
import io.rippledown.sample.SampleKB.ZOO
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class OpenKnowledgeBaseTest : KbActionTestBase() {

    @Test
    fun `unknown name includes demonstrations from the resolution`() = runTest {
        // Given
        val available = listOf("Thyroids")
        val demonstrations = listOf("Pathology", "Zoo Animals")
        every { kbService.resolve("Nothing") } returns KbResolution.NotFound("Nothing", available, demonstrations)

        // When
        val outcome = OpenKnowledgeBase("Nothing").doIt(kbService)

        // Then
        outcome.text() shouldBe kbNotFoundMessage("Nothing", available, demonstrations)
        coVerify(exactly = 0) { kbService.open(any()) }
    }

    @Test
    fun `opening a demonstration asks for a copy name`() = runTest {
        // Given
        every { kbService.resolve("Zoo Animals") } returns KbResolution.Demonstration(ZOO)

        // When
        val outcome = OpenKnowledgeBase("Zoo Animals").doIt(kbService)

        // Then
        val ask = outcome.shouldBeInstanceOf<KbManagementOutcome.AskForName>()
        ask.question shouldBe nameForDemonstrationCopyMessage("Zoo Animals")
        ask.actionForName("Zoo2") shouldBe CopyDemonstrationKnowledgeBase(ZOO, "Zoo2")
        coVerify(exactly = 0) { kbService.open(any()) }
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
    }

    @Test
    fun `an exact match is opened at once`() = runTest {
        // Given
        every { kbService.resolve("thyroids") } returns KbResolution.Exact(thyroids)
        coEvery { kbService.open(thyroids) } just Runs

        // When
        val outcome = OpenKnowledgeBase("thyroids").doIt(kbService)

        // Then
        outcome.text() shouldBe kbOpenedMessage("Thyroids")
        coVerify(exactly = 1) { kbService.open(thyroids) }
    }

    @Test
    fun `a partial match asks first and opens on acceptance`() = runTest {
        // Given
        every { kbService.resolve("thyroid") } returns KbResolution.Partial(thyroids)
        coEvery { kbService.open(thyroids) } just Runs

        // When
        val ask = OpenKnowledgeBase("thyroid").doIt(kbService).asAsk()

        // Then
        ask.question shouldBe confirmKbOpenMessage("Thyroids")
        coVerify(exactly = 0) { kbService.open(any()) }

        // When the user accepts
        val response = ask.accept()

        // Then
        response shouldBe ChatResponse(kbOpenedMessage("Thyroids"))
        coVerify(exactly = 1) { kbService.open(thyroids) }
    }

    @Test
    fun `an ambiguous name is put back to the user`() = runTest {
        // Given
        every { kbService.resolve("thyroid") } returns
                KbResolution.Ambiguous("thyroid", listOf("Thyroids", "Thyroids (old)"))

        // When
        val outcome = OpenKnowledgeBase("thyroid").doIt(kbService)

        // Then
        outcome.text() shouldBe kbAmbiguousMessage("thyroid", listOf("Thyroids", "Thyroids (old)"))
    }

    @Test
    fun `an unknown name lists what there is`() = runTest {
        // Given
        every { kbService.resolve("Lipids") } returns KbResolution.NotFound("Lipids", listOf("Glucose", "Thyroids"))

        // When
        val outcome = OpenKnowledgeBase("Lipids").doIt(kbService)

        // Then
        outcome.text() shouldBe kbNotFoundMessage("Lipids", listOf("Glucose", "Thyroids"))
    }

    @Test
    fun `opening changes the context`() {
        OpenKnowledgeBase("x").changesContext shouldBe true
    }
}
