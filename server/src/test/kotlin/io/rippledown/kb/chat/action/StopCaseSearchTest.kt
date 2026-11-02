package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class StopCaseSearchTest : ActionTestBase() {

    @Test
    fun `stops the current search`() = runTest {
        // Given a search that is running
        every { ruleService.stopCaseSearch() } returns "The search has been terminated."

        // When the action is performed
        val response = StopCaseSearch().doIt(ruleService, null, modelResponder)

        // Then the search is stopped, and the termination is the response
        verify(exactly = 1) { ruleService.stopCaseSearch() }
        response.text shouldBe "The search has been terminated."
    }

    @Test
    fun `returns the service's refusal when there is no search to stop verbatim`() = runTest {
        // Given the service refuses to stop a search, as none is running
        val refusal = "There is no search in progress for this knowledge base."
        every { ruleService.stopCaseSearch() } throws IllegalStateException(refusal)

        // When the action is performed
        val response = StopCaseSearch().doIt(ruleService, null, modelResponder)

        // Then the refusal is the chat response
        response.text shouldBe refusal
    }
}
