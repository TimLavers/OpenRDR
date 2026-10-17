package io.rippledown.integration.pageobjects

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.rippledown.integration.waitUntilAsserted
import io.rippledown.interpretation.CommentTooltipTestHook
import io.rippledown.model.condition.ConditionText

/** Reads the live tooltip content of this window without opening a popup. */
class CommentTooltipPO(private val hook: CommentTooltipTestHook) {
    fun conditionsFor(comment: String): List<ConditionText> {
        val snapshot = hook.snapshot()
        val matches = snapshot.filter { it.text == comment }
        withClue("Expected one tooltip for '$comment'; current comment rows: ${snapshot.map { it.name to it.text }}") {
            matches.size shouldBe 1
        }
        return matches.single().conditions
    }

    fun phrasesFor(comment: String): List<String> =
        conditionsFor(comment).filter { it.hasPhrase() }.map { it.phrase }

    fun waitForConditions(comment: String, conditions: List<String>) {
        waitUntilAsserted {
            withClue("Tooltip conditions for '$comment'") {
                conditionsFor(comment).map { it.formal } shouldContainExactlyInAnyOrder conditions
            }
        }
    }

    fun waitForPhrases(comment: String, phrases: List<String>) {
        waitUntilAsserted {
            withClue("Tooltip phrases for '$comment'") {
                phrasesFor(comment) shouldBe phrases
            }
        }
    }
}
