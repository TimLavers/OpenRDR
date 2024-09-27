package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.ADDING
import io.rippledown.constants.interpretation.BY
import io.rippledown.constants.interpretation.REMOVING
import io.rippledown.constants.interpretation.REPLACING
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import org.junit.Test

class DiffToAnnotatedStringTest {
    @Test
    fun `should convert an Addition to an annotated string`() {
        // given
        val addedText = "Going to Bondi"
        val diff = Addition(addedText)

        // when
        val annotatedString = diff.toAnnotatedString()

        // then
        annotatedString.text shouldBe "$ADDING$addedText"
        annotatedString.spanStyles.size shouldBe 1
        annotatedString.spanStyles[0].start shouldBe ADDING.length
        annotatedString.spanStyles[0].end shouldBe annotatedString.spanStyles[0].start + addedText.length
    }

    @Test
    fun `should convert a Replacement to an annotated string`() {
        // given
        val originalText = "Going to Bondi"
        val replacementText = "Going to Malabar"
        val diff = Replacement(originalText, replacementText)

        // when
        val annotatedString = diff.toAnnotatedString()

        // then
        annotatedString.text shouldBe "$REPLACING$originalText$BY$replacementText"
        annotatedString.spanStyles.size shouldBe 2
        annotatedString.spanStyles[0].start shouldBe REPLACING.length
        annotatedString.spanStyles[0].end shouldBe annotatedString.spanStyles[0].start + originalText.length
        annotatedString.spanStyles[1].start shouldBe annotatedString.spanStyles[0].end + BY.length
        annotatedString.spanStyles[1].end shouldBe annotatedString.spanStyles[1].start + replacementText.length
    }

    @Test
    fun `should convert a Removal to an annotated string`() {
        // given
        val removedText = "Going to Bondi"
        val diff = Removal(removedText)

        // when
        val annotatedString = diff.toAnnotatedString()

        // then
        annotatedString.text shouldBe "$REMOVING$removedText"
        annotatedString.spanStyles.size shouldBe 1
        annotatedString.spanStyles[0].start shouldBe REMOVING.length
        annotatedString.spanStyles[0].end shouldBe annotatedString.spanStyles[0].start + removedText.length
    }
}