package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Called
import io.mockk.verify
import io.rippledown.model.chat.KbFileDialogRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ImportKnowledgeBaseTest : KbActionTestBase() {
    @Test
    fun `import requests a chooser without requiring a knowledge base`() = runTest {
        // Given
        val action = ImportKnowledgeBase()

        // When
        val response = action.doIt(kbService).shouldBeInstanceOf<KbManagementOutcome.Done>().response

        // Then
        response.text shouldBe "Choose a knowledge base ZIP file to import."
        val request = response.kbFileDialogRequest.shouldBeInstanceOf<KbFileDialogRequest.Import>()
        request.requestId.isNotBlank() shouldBe true
        verify { kbService wasNot Called }
    }

    @Test
    fun `each import request has a new id`() = runTest {
        // Given
        val action = ImportKnowledgeBase()

        // When
        val first = action.doIt(kbService).shouldBeInstanceOf<KbManagementOutcome.Done>().response
        val second = action.doIt(kbService).shouldBeInstanceOf<KbManagementOutcome.Done>().response

        // Then
        second.kbFileDialogRequest?.requestId shouldNotBe first.kbFileDialogRequest?.requestId
    }
}
