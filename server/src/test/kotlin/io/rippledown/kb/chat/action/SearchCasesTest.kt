package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.chat.SEARCH_UNDER_WAY_MESSAGE
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SearchCasesTest : ActionTestBase() {

    @Test
    fun `starts a search with the given parameters, with no case selected`() = runTest {
        // Given the parameters of a search, and no current case
        val conditions = listOf("TSH is greater than 4", "Age is greater than 50")
        val lists = listOf("processed", "cornerstones")
        every { ruleService.searchCases(conditions, "and", 5, lists, "highTSH") } returns SEARCH_UNDER_WAY_MESSAGE

        // When the action is performed
        val response = SearchCases(conditions, "and", 5, lists, "highTSH").doIt(ruleService, null, modelResponder)

        // Then the search is started with those parameters, and its confirmation is the response
        verify(exactly = 1) { ruleService.searchCases(conditions, "and", 5, lists, "highTSH") }
        response.text shouldBe SEARCH_UNDER_WAY_MESSAGE
    }

    @Test
    fun `missing optional parameters become no connector, no limit and empty lists`() = runTest {
        // Given a search action whose optional parameters were not given
        every { ruleService.searchCases(emptyList(), "", null, emptyList(), "") } returns SEARCH_UNDER_WAY_MESSAGE

        // When the action is performed
        SearchCases(null, null, null, null, null).doIt(ruleService, null, modelResponder)

        // Then the service is called with those defaults
        verify(exactly = 1) { ruleService.searchCases(emptyList(), "", null, emptyList(), "") }
    }

    @Test
    fun `returns the service's refusal of an invalid parameter verbatim`() = runTest {
        // Given the service refuses the search
        val refusal =
            "Cannot search: the condition \"Blah is greater than 5\" does not correspond to a condition in this knowledge base."
        every { ruleService.searchCases(any(), any(), any(), any(), any()) } throws IllegalArgumentException(refusal)

        // When the action is performed
        val response = SearchCases(
            listOf("Blah is greater than 5"), "and", null, listOf("processed"), "nowhere"
        ).doIt(ruleService, null, modelResponder)

        // Then the refusal is the chat response
        response.text shouldBe refusal
    }

    @Test
    fun `returns the service's refusal when a search is already running verbatim`() = runTest {
        // Given the service refuses the search because one is already running
        val refusal = "A search is already in progress for this knowledge base."
        every { ruleService.searchCases(any(), any(), any(), any(), any()) } throws IllegalStateException(refusal)

        // When the action is performed
        val response = SearchCases(
            listOf("TSH is greater than 4"), "and", null, listOf("processed"), "highTSH"
        ).doIt(ruleService, null, modelResponder)

        // Then the refusal is the chat response
        response.text shouldBe refusal
    }
}
