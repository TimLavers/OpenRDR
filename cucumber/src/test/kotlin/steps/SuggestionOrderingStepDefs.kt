package steps

import io.cucumber.java.en.Then
import io.kotest.matchers.collections.shouldNotContain
import io.rippledown.chat.ChatTestHook
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

private fun snapshotDiagnostic(): String {
    val s = ChatTestHook.snapshot()
    return "ChatTestHook{messageCount=${s.messageCount}, suggestionRowCount=${s.suggestionRowCount}, " +
            "mostRecentBotText=${s.mostRecentBotText?.take(120)}, " +
            "mostRecentSuggestionText=${s.mostRecentSuggestionText?.take(200)}, " +
            "sendIsEnabled=${s.sendIsEnabled}}"
}

/**
 * Step definitions for the Phase 1 targeted-suggested-conditions feature.
 *
 * Verifies the *order* of items in the most recent suggestion list, plus
 * the cap on list size. Relies on `chatPO().suggestionsInMostRecentMessage()`
 * which already returns the displayed items in display order.
 *
 * Matching semantics:
 *  - "the suggested condition X should appear before Y" matches the FIRST
 *    suggestion whose text contains X (case-insensitive) and the FIRST
 *    whose text contains Y. Both must be present; X's index must be lower
 *    than Y's.
 *  - "the first suggested condition is X" requires substring match of X
 *    against the first list entry.
 *
 * The substring-contains semantics keeps assertions robust against minor
 * `Condition.asText()` formatting (e.g. signature suffixes like "(current)").
 */
class SuggestionOrderingStepDefs {

    @Then("the suggested condition {string} should appear before {string}")
    fun suggestedConditionShouldAppearBefore(earlier: String, later: String) {
        awaitSuggestedConditionAppearsBeforeAll(earlier, listOf(later))
    }

    /**
     * Asserts the FIRST suggestion containing [earlier] precedes the FIRST
     * suggestion containing each of [laters], polling for up to 20 seconds.
     *
     * Note: failures throw AssertionError (not IllegalStateException via
     * `check`) so Awaitility's `untilAsserted` keeps polling for the full
     * timeout window. `check` produces IllegalStateException, which
     * awaitility lets escape immediately — ending the await on the first
     * poll, before the bot has had time to produce suggestions.
     */
    private fun awaitSuggestedConditionAppearsBeforeAll(earlier: String, laters: List<String>) {
        await().atMost(ofSeconds(20)).untilAsserted {
            val list = currentSuggestions()
            val earlierIdx = list.indexOfFirst { it.contains(earlier, ignoreCase = true) }
            if (earlierIdx < 0) {
                throw AssertionError(
                    "No suggestion containing '$earlier'. Suggestions: $list. ${snapshotDiagnostic()}"
                )
            }
            laters.forEach { later ->
                val laterIdx = list.indexOfFirst { it.contains(later, ignoreCase = true) }
                if (laterIdx < 0) {
                    throw AssertionError(
                        "No suggestion containing '$later'. Suggestions: $list. ${snapshotDiagnostic()}"
                    )
                }
                if (earlierIdx >= laterIdx) {
                    throw AssertionError(
                        "Expected '$earlier' (idx $earlierIdx) to appear before '$later' (idx $laterIdx). " +
                                "Suggestions: $list. ${snapshotDiagnostic()}"
                    )
                }
            }
        }
    }

    @Then("the first suggested condition is {string}")
    fun firstSuggestedConditionIs(text: String) {
        await().atMost(ofSeconds(20)).until {
            currentSuggestions().firstOrNull()?.contains(text, ignoreCase = true) == true
        }
    }

    @Then("the number of suggested conditions should be at most {int}")
    fun numberOfSuggestedConditionsAtMost(max: Int) {
        await().atMost(ofSeconds(20)).until {
            currentSuggestions().isNotEmpty()
        }
        val count = currentSuggestions().size
        if (count > max) throw AssertionError("Expected at most $max suggestions, got $count")
    }

    @Then("the suggested condition {string} should appear before (all of )the following suggestion(s):")
    fun suggestedConditionShouldAppearBeforeAll(earlier: String, table: io.cucumber.datatable.DataTable) {
        awaitSuggestedConditionAppearsBeforeAll(earlier, table.asList())
    }

    @Then("the suggested conditions, in order, should start with:")
    fun suggestedConditionsInOrderShouldStartWith(table: io.cucumber.datatable.DataTable) {
        val expectedPrefix = table.asList()
        await().atMost(ofSeconds(20)).untilAsserted {
            val list = currentSuggestions()
            if (list.size < expectedPrefix.size) {
                throw AssertionError(
                    "Expected at least ${expectedPrefix.size} suggestions, got ${list.size}: $list"
                )
            }
            expectedPrefix.forEachIndexed { i, expected ->
                if (!list[i].contains(expected, ignoreCase = true)) {
                    throw AssertionError(
                        "Suggestion at index $i was '${list[i]}', expected to contain '$expected'. Full list: $list"
                    )
                }
            }
        }
    }

    @Then("the suggested condition {string} should NOT be the first suggestion")
    fun suggestedConditionShouldNotBeFirst(text: String) {
        // Used to assert that a signal which would normally promote a
        // condition (e.g. historical) is NOT firing — without it, the
        // condition has no reason to be at the top.
        await().atMost(ofSeconds(20)).until { currentSuggestions().isNotEmpty() }
        val list = currentSuggestions()
        val first = list.firstOrNull() ?: throw AssertionError("No suggestions present")
        if (first.contains(text, ignoreCase = true)) {
            throw AssertionError(
                "Expected '$text' NOT to be the first suggestion, but first was '$first'. " +
                        "Full list: $list"
            )
        }
    }

    @Then("the condition containing {string} should NOT appear")
    fun suggestedConditionContainingShouldNotAppear(text: String) {
        await().atMost(ofSeconds(20)).until { currentSuggestions().isNotEmpty() }
        val list = currentSuggestions()
        val contains = list.firstOrNull {
            it.contains(text, ignoreCase = true)
        }
        if (contains != null) {
            throw AssertionError(
                "Expected '$text' NOT to be in the suggestions.\n" +
                        "Full list:\n $list"
            )
        }
    }

    @Then("the suggestion {string} should NOT appear")
    fun suggestedConditionShouldNotAppear(text: String) {
        await().atMost(ofSeconds(20)).until { currentSuggestions().isNotEmpty() }
        currentSuggestions() shouldNotContain text
    }

    private fun currentSuggestions(): List<String> = chatPO().suggestionsInMostRecentMessage()
}
