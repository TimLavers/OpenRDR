package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.rippledown.constants.interpretation.COMMENT_TEXT_PREFIX
import io.rippledown.constants.interpretation.CONDITION_PHRASE_PREFIX
import io.rippledown.constants.interpretation.CONDITION_PREFIX
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleText

class InterpretationPOTest {
    @Test
    fun `condition phrases use rendered text and exclude formal condition nodes`() {
        // Given
        val phrase = textNode("${CONDITION_PHRASE_PREFIX}elevated glucose", "elevated glucose")
        val formal = textNode("${CONDITION_PREFIX}Glucose is high", "Glucose is high")
        val root = rootWith(phrase, formal)
        val page = InterpretationPO { root }

        // When
        val phrases = page.conditionPhrasesShown()

        // Then
        phrases shouldBe listOf("elevated glucose")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `no phrase nodes gives an empty list`(formalOnly: Boolean) {
        // Given
        val root = if (formalOnly) {
            rootWith(textNode("${CONDITION_PREFIX}Glucose is high", "Glucose is high"))
        } else rootWith()
        val page = InterpretationPO { root }

        // When
        val phrases = page.conditionPhrasesShown()

        // Then
        phrases shouldBe emptyList()
    }

    @Test
    fun `all displayed phrases are returned in tooltip order including duplicates`() {
        // Given
        val root = rootWith(
            textNode("${CONDITION_PHRASE_PREFIX}elevated glucose", "elevated glucose"),
            textNode("${CONDITION_PHRASE_PREFIX}raised glucose", "raised glucose"),
            textNode("${CONDITION_PHRASE_PREFIX}raised glucose", "raised glucose")
        )
        val page = InterpretationPO { root }

        // When
        val phrases = page.conditionPhrasesShown()

        // Then
        phrases shouldBe listOf("elevated glucose", "raised glucose", "raised glucose")
    }

    @Test
    fun `phrase check waits until the previous phrase has disappeared`() {
        // Given
        val oldPhrase = textNode("${CONDITION_PHRASE_PREFIX}elevated glucose", "elevated glucose")
        val newPhrase = textNode("${CONDITION_PHRASE_PREFIX}raised glucose", "raised glucose")
        val contextProvider = mockk<() -> AccessibleContext>()
        every { contextProvider() } returnsMany listOf(rootWith(oldPhrase, newPhrase), rootWith(newPhrase))
        val page = InterpretationPO(contextProvider)

        // When
        page.waitForConditionPhrasesToBeShowing(listOf("raised glucose"))

        // Then
        verify(atLeast = 2) { contextProvider() }
    }

    private fun rootWith(vararg children: AccessibleContext): AccessibleContext = mockk {
        every { accessibleDescription } returns null
        every { accessibleChildrenCount } returns children.size
        children.forEachIndexed { index, child ->
            every { getAccessibleChild(index) } returns mockk<Accessible> {
                every { accessibleContext } returns child
            }
        }
    }

    private fun textNode(description: String, text: String): AccessibleContext = mockk {
        every { accessibleDescription } returns description
        every { accessibleName } returns description
        every { accessibleChildrenCount } returns 0
        every { accessibleText } returns mockk<AccessibleText> {
            every { charCount } returns text.length
            text.forEachIndexed { index, character ->
                every { getAtIndex(AccessibleText.CHARACTER, index) } returns character.toString()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `hover waits for the comment row even if it disappears after its text is observed`(temporarilyMissing: Boolean) {
        // Given
        val comment = "Go to Bondi."
        val empty = mockk<AccessibleContext>()
        every { empty.accessibleDescription } returns null
        every { empty.accessibleChildrenCount } returns 0
        val cell = mockk<AccessibleContext>()
        every { cell.accessibleDescription } returns "${COMMENT_TEXT_PREFIX}C1"
        every { cell.accessibleChildrenCount } returns 0
        every { cell.accessibleText } returns null
        every { cell.accessibleName } returns comment
        val contextProvider = mockk<() -> AccessibleContext>()
        every { contextProvider() } returnsMany if (temporarilyMissing) listOf(empty, empty, cell) else listOf(cell)
        val page = spyk(InterpretationPO(contextProvider), recordPrivateCalls = true)
        every { page.commentsShown() } returns listOf(comment)
        every { page["movePointerOverCentreOf"](cell) } returns Unit

        // When
        page.movePointerToComment(comment)

        // Then
        verify(exactly = 1) { page["movePointerOverCentreOf"](cell) }
    }
}
