package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbAmbiguousMessage
import io.rippledown.constants.chat.kbDescriptionOfMessage
import io.rippledown.constants.chat.kbNotFoundMessage
import io.rippledown.kb.KbResolution
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ShowKnowledgeBaseDescriptionTest : KbActionTestBase() {

    @Test
    fun `returns the stored description verbatim`() = runTest {
        // given
        val description = "# Thyroids\n\nClinical guidance."
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.description(thyroids) } returns description

        // when
        val outcome = ShowKnowledgeBaseDescription().doIt(kbService)

        // then
        outcome.text() shouldBe description
    }

    @Test
    fun `a named knowledge base that is not open is described with its name`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.resolve("Glucose") } returns KbResolution.Exact(glucose)
        every { kbService.description(glucose) } returns "Glucose rules."

        // when
        val outcome = ShowKnowledgeBaseDescription("Glucose").doIt(kbService)

        // then
        outcome.text() shouldBe kbDescriptionOfMessage("Glucose", "Glucose rules.")
    }

    @Test
    fun `a partial name is described without asking, since reading changes nothing`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.resolve("Gluc") } returns KbResolution.Partial(glucose)
        every { kbService.description(glucose) } returns "Glucose rules."

        // when
        val outcome = ShowKnowledgeBaseDescription("Gluc").doIt(kbService)

        // then
        outcome.text() shouldBe kbDescriptionOfMessage("Glucose", "Glucose rules.")
    }

    @Test
    fun `naming the open knowledge base gives its description verbatim`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.resolve("Thyroids") } returns KbResolution.Exact(thyroids)
        every { kbService.description(thyroids) } returns "# Thyroids"

        // when
        val outcome = ShowKnowledgeBaseDescription("Thyroids").doIt(kbService)

        // then
        outcome.text() shouldBe "# Thyroids"
    }

    @Test
    fun `a named knowledge base with no description`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.resolve("Glucose") } returns KbResolution.Exact(glucose)
        every { kbService.description(glucose) } returns "  "

        // when
        val outcome = ShowKnowledgeBaseDescription("Glucose").doIt(kbService)

        // then
        outcome.text() shouldBe "\"Glucose\" has no description."
    }

    @Test
    fun `a demonstration is described from the sample`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns null
        every { kbService.resolve("Zoo Animals") } returns KbResolution.Demonstration(SampleKB.ZOO)

        // when
        val outcome = ShowKnowledgeBaseDescription("Zoo Animals").doIt(kbService)

        // then
        outcome.text() shouldBe kbDescriptionOfMessage("Zoo Animals", SampleKB.ZOO.description())
    }

    @Test
    fun `an ambiguous name is reported`() = runTest {
        // given
        every { kbService.resolve("o") } returns KbResolution.Ambiguous("o", listOf("Glucose", "Zoo Animals"))

        // when
        val outcome = ShowKnowledgeBaseDescription("o").doIt(kbService)

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
        val outcome = ShowKnowledgeBaseDescription("Lipids").doIt(kbService)

        // then
        outcome.text() shouldBe kbNotFoundMessage("Lipids", listOf("Thyroids"), listOf("Pathology"))
    }

    @Test
    fun `reports an empty description`() = runTest {
        // given
        every { kbService.openKnowledgeBase() } returns thyroids
        every { kbService.description(thyroids) } returns ""

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
