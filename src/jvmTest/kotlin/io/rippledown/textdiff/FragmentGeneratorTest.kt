package io.rippledown.textdiff

import io.kotest.matchers.shouldBe
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.diff.Unchanged
import org.junit.Test

class FragmentGeneratorTest {

    @Test
    fun `should convert a list of strings to alphabetic characters`() {
        val list = listOf("Sentence 1", "Sentence 2", "Sentence 1", "Sentence 3")
        list.toAlphabetString(TextToAlphabetMapper()) shouldBe "ABAC"
    }

    @Test
    fun `should find the different fragments between two lists of strings`() {
        val originalStrings = listOf("Sentence 1", "Sentence 2", "Sentence 3", "Sentence 4", "Sentence 5")
        val changedStrings = listOf("Sentence 1", "Sentence 3", "Sentence 4", "Sentence 6", "Sentence 7")

        val fragments = generateDifferences(originalStrings, changedStrings)

        fragments shouldBe listOf(
            UnchangedFragment("Sentence 1"),
            RemovedFragment("Sentence 2"),
            UnchangedFragment("Sentence 3"),
            UnchangedFragment("Sentence 4"),
            ReplacedFragment("Sentence 5", "Sentence 6"),
            AddedFragment("Sentence 7")
        )
    }

    @Test
    fun `should create an unchanged fragment from a Diff`() {
        val mapper = TextToAlphabetMapper()
        val text = "Sentence 1"
        val alpha = mapper.toAlpha(text)
        val diff = Unchanged(alpha)
        diff.toFragment(mapper) shouldBe UnchangedFragment(text)
    }

    @Test
    fun `should create an added fragment from a Diff`() {
        val mapper = TextToAlphabetMapper()
        val text = "Sentence 1"
        val alpha = mapper.toAlpha(text)
        val diff = Addition(alpha)
        diff.toFragment(mapper) shouldBe AddedFragment(text)
    }

    @Test
    fun `should create a removed fragment from a Diff`() {
        val mapper = TextToAlphabetMapper()
        val text = "Sentence 1"
        val alpha = mapper.toAlpha(text)
        val diff = Removal(alpha)
        diff.toFragment(mapper) shouldBe RemovedFragment(text)
    }

    @Test
    fun `should create a replacement fragment from a Diff`() {
        val mapper = TextToAlphabetMapper()
        val text1 = "Sentence 1"
        val text2 = "Sentence 2"
        val alpha1 = mapper.toAlpha(text1)
        val alpha2 = mapper.toAlpha(text2)
        val diff = Replacement(alpha1, alpha2)
        diff.toFragment(mapper) shouldBe ReplacedFragment(text1, text2)
    }

    /*@Test
    fun `should restore a list of strings from a character array`() {
        val strings = listOf("Sentence 1", "Sentence 2", "Sentence 1", "Sentence 3")
        val mapper = TextToAlphabetMapper()
        val alphas = strings.toAlphabetString(mapper) // A B A C
        alphas.toFragments(mapper) shouldBe strings
    }
*/
    @Test
    fun `should generate an unchanged fragment if the original text is the same as the changed text`() {
        val sentence = "Go to Bondi Beach."
        fragmentList(sentence, sentence).fragments shouldBe listOf(
            UnchangedFragment(sentence)
        )
    }

    @Test
    fun `should generate no fragments if the original text and changed text are blank`() {
        fragmentList("", "").fragments shouldBe emptyList()
    }

    @Test
    fun `should split a paragraph into sentences delimited by a period and a space`() {
        val paragraph = "Sun is up. Go to Bondi Beach. Surf is great."
        paragraph.splitIntoSentences() shouldBe listOf("Sun is up.", "Go to Bondi Beach.", "Surf is great.")
    }

    @Test
    fun `should split a paragraph into sentences delimited by a ! and a space`() {
        val paragraph = "Sun is up! Go to Bondi Beach. Surf is great."
        paragraph.splitIntoSentences() shouldBe listOf("Sun is up!", "Go to Bondi Beach.", "Surf is great.")
    }

    @Test
    fun `should split a paragraph into sentences delimited by a question mark and a space`() {
        val paragraph = "Is the sun up? Go to Bondi Beach! Surf is great."
        paragraph.splitIntoSentences() shouldBe listOf("Is the sun up?", "Go to Bondi Beach!", "Surf is great.")
    }

    @Test
    fun `should not split an empty paragraph into sentences`() {
        "".splitIntoSentences() shouldBe emptyList()
    }

    @Test
    fun `should not split a paragraph that just has spaces into sentences`() {
        "    ".splitIntoSentences() shouldBe emptyList()
    }

    @Test
    fun `should generate an added fragment if the changed text has added a comment`() {
        val bondiText = "Go to Bondi Beach."
        val surfText = "Surf is great."
        fragmentList(bondiText, "$bondiText $surfText").fragments shouldBe listOf(
            UnchangedFragment(bondiText),
            AddedFragment(surfText)
        )
    }

    @Test
    fun `should generate a removed fragment if the changed text has removed a comment`() {
        val bondiText = "Go to Bondi Beach."
        val surfText = "Surf is great."
        fragmentList("$bondiText $surfText", bondiText).fragments shouldBe listOf(
            UnchangedFragment(bondiText),
            RemovedFragment(surfText)
        )
    }

    @Test
    fun `should generate a replaced fragment if the changed text has replaced a comment`() {
        val bondiText = "Go to Bondi Beach."
        val surfText = "Surf is great."
        fragmentList(bondiText, surfText).fragments shouldBe listOf(
            ReplacedFragment(bondiText, surfText)
        )
    }
}