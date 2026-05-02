package steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

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
        await().atMost(ofSeconds(20)).until {
            val list = currentSuggestions()
            val earlierIdx = list.indexOfFirst { it.contains(earlier, ignoreCase = true) }
            val laterIdx = list.indexOfFirst { it.contains(later, ignoreCase = true) }
            earlierIdx in 0 until (if (laterIdx == -1) Int.MAX_VALUE else laterIdx)
        }
        val list = currentSuggestions()
        val earlierIdx = list.indexOfFirst { it.contains(earlier, ignoreCase = true) }
        val laterIdx = list.indexOfFirst { it.contains(later, ignoreCase = true) }
        check(earlierIdx >= 0) { "No suggestion containing '$earlier'. Suggestions: $list" }
        check(laterIdx >= 0) { "No suggestion containing '$later'. Suggestions: $list" }
        (earlierIdx < laterIdx) shouldBe true
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
        check(count <= max) { "Expected at most $max suggestions, got $count" }
    }

    @Then("the suggested conditions, in order, should start with:")
    fun suggestedConditionsInOrderShouldStartWith(table: io.cucumber.datatable.DataTable) {
        val expectedPrefix = table.asList()
        await().atMost(ofSeconds(20)).until {
            val list = currentSuggestions()
            list.size >= expectedPrefix.size && expectedPrefix.withIndex().all { (i, expected) ->
                list[i].contains(expected, ignoreCase = true)
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
        val first = list.firstOrNull() ?: error("No suggestions present")
        check(!first.contains(text, ignoreCase = true)) {
            "Expected '$text' NOT to be the first suggestion, but first was '$first'. " +
                    "Full list: $list"
        }
    }

    @And("I work through any cornerstone cases")
    fun workThroughAnyCornerstoneCases() {
        // Phase 1 ranking is computed once cornerstones have been resolved
        // and the bot is asking for a reason. If a cornerstone is showing,
        // confirm it; otherwise no-op. This step exists so historical-signal
        // scenarios can ignore the cornerstone-handling preamble.
        // Implementation will follow the existing cornerstone-confirmation
        // chat flow; left as TODO for the human implementing Phase 1.
        TODO("Wire up to existing cornerstone-confirmation chat steps once Phase 1 wires the historical scorer.")
    }

    private fun currentSuggestions(): List<String> = chatPO().suggestionsInMostRecentMessage()
}
