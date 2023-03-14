package io.rippledown.textdiff

import io.kotest.matchers.shouldBe
import org.junit.Test

class TextDiffUtilsTest {

    @Test
    fun `should identify an added text fragment`() {
        val originalFragments = listOf("A and B.")
        val revisedText = "A and B. X and Y."
        val revisions = revisions(originalFragments, revisedText)
        revisions shouldBe listOf(AddedFragment("X and Y."))
    }

    @Test
    fun `should identify an added text fragment containing a *`() {
        val originalFragments = listOf("A and B*.")
        val revisedText = "A and B*. X and Y*."
        val revisions = revisions(originalFragments, revisedText)
        revisions shouldBe listOf(AddedFragment("X and Y*."))
    }

    @Test
    fun `should identify two non-consecutive added text fragments`() {
        val originalFragments = listOf("A and B.")
        val revisedText = "P and Q. A and B. X and Y."
        val revisions = revisions(originalFragments, revisedText)
        revisions shouldBe listOf(
            AddedFragment("P and Q."),
            AddedFragment("X and Y.")
        )
    }

    @Test
    fun `should identify a removed text fragment`() {
        val originalFragments = listOf("X and Y.", "A and B.")
        val revisedText = "A and B."
        val revisions = revisions(originalFragments, revisedText)
        revisions shouldBe listOf(RemovedFragment("X and Y."))
    }

    @Test
    fun `should identify two non-consecutive removed text fragments`() {
        val originalFragments = listOf("P and Q.", "A and B.", "X and Y.")
        val revisedText = "A and B."
        val revisions = revisions(originalFragments, revisedText)
        revisions shouldBe listOf(
            RemovedFragment("P and Q."),
            RemovedFragment("X and Y.")
        )
    }

    @Test
    fun `should not identify fragments if there is no change`() {
        val originalFragments = listOf("P and Q.")
        val revisedText = "P and Q."
        val revisions = revisions(originalFragments, revisedText)
        revisions shouldBe emptyList()
    }

}