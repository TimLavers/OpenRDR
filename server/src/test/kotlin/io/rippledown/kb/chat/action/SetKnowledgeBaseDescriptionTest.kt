package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetKnowledgeBaseDescriptionTest : KbActionTestBase() {

    @Test
    fun `replaces the stored description verbatim`() = runTest {
        // given
        val description = "# Thyroids\n\nClinical guidance."
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.setDescription(thyroids, description) } returns Unit

        // when
        val outcome = SetKnowledgeBaseDescription(description).doIt(kbService)

        // then
        outcome.text() shouldBe "Description of \"Thyroids\" updated."
        verify(exactly = 1) { kbService.setDescription(thyroids, description) }
    }

    @Test
    fun `a named knowledge base that is not open gets the description, not the open one`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.resolve("zinc") } returns KbResolution.Exact(glucose)
        every { kbService.setDescription(glucose, "metallic kb") } returns Unit

        // when
        val outcome = SetKnowledgeBaseDescription("metallic kb", "zinc").doIt(kbService)

        // then
        outcome.text() shouldBe "Description of \"Glucose\" updated."
        verify(exactly = 1) { kbService.setDescription(glucose, "metallic kb") }
        verify(exactly = 0) { kbService.setDescription(thyroids, any()) }
    }

    @Test
    fun `a partial match is confirmed before writing`() = runTest {
        // given
        every { kbService.resolve("Gluc") } returns KbResolution.Partial(glucose)
        every { kbService.setDescription(glucose, "Sugar.") } returns Unit

        // when
        val outcome = SetKnowledgeBaseDescription("Sugar.", "Gluc").doIt(kbService)

        // then
        val ask = outcome.shouldBeInstanceOf<KbManagementOutcome.Ask>()
        ask.question shouldBe confirmKbOpenMessage("Glucose")
        verify(exactly = 0) { kbService.setDescription(any(), any()) }
        ask.thenDo(kbService).text shouldBe kbDescriptionUpdatedMessage("Glucose")
        verify(exactly = 1) { kbService.setDescription(glucose, "Sugar.") }
    }

    @Test
    fun `a demonstration cannot be described`() = runTest {
        // given
        every { kbService.resolve("Zoo Animals") } returns KbResolution.Demonstration(SampleKB.ZOO)

        // when
        val outcome = SetKnowledgeBaseDescription("Animals.", "Zoo Animals").doIt(kbService)

        // then
        outcome.text() shouldBe cannotDescribeDemonstrationMessage("Zoo Animals")
        verify(exactly = 0) { kbService.setDescription(any(), any()) }
    }

    @Test
    fun `an ambiguous name is reported`() = runTest {
        // given
        every { kbService.resolve("o") } returns KbResolution.Ambiguous("o", listOf("Glucose", "Zoo Animals"))

        // when
        val outcome = SetKnowledgeBaseDescription("Text", "o").doIt(kbService)

        // then
        outcome.text() shouldBe kbAmbiguousMessage("o", listOf("Glucose", "Zoo Animals"))
    }

    @Test
    fun `an unknown name is reported`() = runTest {
        // given
        every { kbService.resolve("Lipids") } returns KbResolution.NotFound(
            "Lipids",
            listOf("Thyroids"),
            listOf("Pathology")
        )

        // when
        val outcome = SetKnowledgeBaseDescription("Text", "Lipids").doIt(kbService)

        // then
        outcome.text() shouldBe kbNotFoundMessage("Lipids", listOf("Thyroids"), listOf("Pathology"))
        verify(exactly = 0) { kbService.setDescription(any(), any()) }
    }

    @Test
    fun `requires an open knowledge base`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns null

        // when
        val outcome = SetKnowledgeBaseDescription("Text").doIt(kbService)

        // then
        outcome.text() shouldBe NO_KB_OPEN_MESSAGE
        verify(exactly = 0) { kbService.setDescription(any(), any()) }
    }

    @Test
    fun `setting the description does not change the chat context`() {
        // given / when / then
        SetKnowledgeBaseDescription("Text").changesContext shouldBe false
    }
}
