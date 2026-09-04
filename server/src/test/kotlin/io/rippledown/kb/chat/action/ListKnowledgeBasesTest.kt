package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.rippledown.constants.chat.NO_KNOWLEDGE_BASES
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ListKnowledgeBasesTest : KbActionTestBase() {

    @Test
    fun `lists the names one per line with the open one marked`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns listOf(glucose, scratch, thyroids)
        every { kbService.openKnowledgeBase() } returns scratch

        // When
        val outcome = ListKnowledgeBases().doIt(kbService)

        // Then
        outcome.text() shouldBe "Glucose\nScratch (open)\nThyroids"
    }

    @Test
    fun `nothing is marked when no knowledge base is open`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns listOf(glucose, thyroids)
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = ListKnowledgeBases().doIt(kbService)

        // Then
        outcome.text() shouldBe "Glucose\nThyroids"
    }

    @Test
    fun `no knowledge bases`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.openKnowledgeBase() } returns null

        // When
        val outcome = ListKnowledgeBases().doIt(kbService)

        // Then
        outcome.text() shouldBe NO_KNOWLEDGE_BASES
    }

    @Test
    fun `listing does not change the context`() {
        ListKnowledgeBases().changesContext shouldBe false
    }
}
