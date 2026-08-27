package io.rippledown.suggestions

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.utils.defaultDate
import kotlin.test.Test

/**
 * Conditions suggested for comment attributes. A comment attribute's value is
 * its own definition, so the only thing worth asking about it is whether the
 * case was given it.
 */
class ConditionSuggesterCommentAttributeTest {
    private val glucose = Attribute(1, "Glucose")
    private val c1 = Attribute(5001, "C1", AttributeKind.COMMENT)
    private val c2 = Attribute(5002, "C2", AttributeKind.COMMENT)

    /**
     * A case as it is after materialisation: the comment attributes the rules
     * gave it carry their rendered text as their value.
     */
    private fun materialisedCase() = with(RDRCaseBuilder()) {
        addValue(glucose, defaultDate, "5.0")
        addValue(c1, defaultDate, "Let's surf.")
        build("Bondi")
    }

    private fun suggestionTexts(case: RDRCase = materialisedCase()) =
        ConditionSuggester(
            SuggestionContext(sessionCase = case, attributes = setOf(glucose, c1, c2))
        ).allSuggestions().map { it.asText() }

    @Test
    fun `presence is suggested for a comment given for the case`() {
        // Given a case that was given the comment C1
        // When suggestions are generated
        val suggestions = suggestionTexts()

        // Then the case can be asked whether it was given that comment
        suggestions shouldContain "C1 is in case"
    }

    @Test
    fun `the value of a comment is not suggested as a condition`() {
        // Given a case that was given the comment C1
        // When suggestions are generated
        val suggestions = suggestionTexts()

        // Then no condition is offered on the comment's text: the text is the
        // comment's definition, so such a condition only restates its presence
        suggestions shouldNotContain "C1 is \"Let's surf.\""
        suggestions.none { it.startsWith("C1 ") && it != "C1 is in case" } shouldBe true
    }

    @Test
    fun `no condition is suggested for a comment the case was not given`() {
        // Given a case that was not given the comment C2
        // When suggestions are generated
        val suggestions = suggestionTexts()

        // Then C2 is not mentioned at all: a knowledge base has many comments,
        // and all but a few are absent from any one case
        suggestions.none { "C2" in it } shouldBe true
    }

    @Test
    fun `conditions on the case data are unaffected`() {
        // Given a case with a glucose value
        // When suggestions are generated
        val suggestions = suggestionTexts()

        // Then the conditions on the external attribute are still offered
        suggestions shouldContain "Glucose ≥ 5.0"
    }
}
