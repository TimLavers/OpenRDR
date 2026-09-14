package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.chat.NO_KB_OPEN
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KbFileDialogRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ExportKnowledgeBaseTest : KbActionTestBase() {
    @Test
    fun `export captures the open KB before it can change`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids

        // When
        val response = ExportKnowledgeBase().doIt(kbService).shouldBeInstanceOf<KbManagementOutcome.Done>().response
        every { kbService.openKnowledgeBase() } returns glucose

        // Then
        response.text shouldBe "Choose where to save \"Thyroids\" as a ZIP file."
        val request = response.kbFileDialogRequest.shouldBeInstanceOf<KbFileDialogRequest.Export>()
        request.requestId.isNotBlank() shouldBe true
        request.kbInfo.id shouldBe thyroids.id
        request.kbInfo.name shouldBe thyroids.name
        verify(exactly = 1) { kbService.openKnowledgeBase() }
    }

    @Test
    fun `export without an open KB refuses without a dialog`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns null

        // When
        val response = ExportKnowledgeBase().doIt(kbService).shouldBeInstanceOf<KbManagementOutcome.Done>().response

        // Then
        response shouldBe ChatResponse(NO_KB_OPEN)
        response.kbFileDialogRequest shouldBe null
    }

    @Test
    fun `exporting the same KB again has a new request id`() = runTest {
        // Given
        every { kbService.openKnowledgeBase() } returns thyroids
        val action = ExportKnowledgeBase()

        // When
        val first = action.doIt(kbService).shouldBeInstanceOf<KbManagementOutcome.Done>().response
        val second = action.doIt(kbService).shouldBeInstanceOf<KbManagementOutcome.Done>().response

        // Then
        second.kbFileDialogRequest?.requestId shouldNotBe first.kbFileDialogRequest?.requestId
    }
}
