package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.rippledown.model.chat.ChatResponse
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ListKnowledgeBasesTest : KbActionTestBase() {
    private val demoTitles =
        listOf("Contact Lense Prescription", "Pathology", "Thyroid Stimulating Hormone", "Zoo Animals")
    private val demonstrationSection = """
        Demonstration knowledge bases (open one to get your own copy):
        Contact Lense Prescription
        Pathology
        Thyroid Stimulating Hormone
        Zoo Animals
    """.trimIndent()

    @BeforeTest
    fun stubDemonstrations() {
        every { kbService.demonstrations() } returns SampleKB.demonstrations()
    }

    @Test
    fun `lists the names one per line with the open one marked`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns listOf(thyroids, glucose)
        every { kbService.openKnowledgeBase() } returns thyroids

        // When
        val outcome = ListKnowledgeBases().doIt(kbService)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response shouldBe ChatResponse(
            "Your knowledge bases:\nThyroids (open)\nGlucose\n\n$demonstrationSection",
            kbChoices = listOf("Glucose") + demoTitles
        )
    }

    @Test
    fun `nothing is marked when no knowledge base is open`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns listOf(glucose, thyroids)
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = ListKnowledgeBases().doIt(kbService)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response shouldBe ChatResponse(
            "Your knowledge bases:\nGlucose\nThyroids\n\n$demonstrationSection",
            kbChoices = listOf("Glucose", "Thyroids") + demoTitles
        )
    }

    @Test
    fun `no knowledge bases`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = ListKnowledgeBases().doIt(kbService)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response shouldBe ChatResponse(
            "You have no knowledge bases of your own.\n\n$demonstrationSection",
            kbChoices = demoTitles
        )
    }

    @Test
    fun `listing does not change the context`() {
        ListKnowledgeBases().changesContext shouldBe false
    }
}
