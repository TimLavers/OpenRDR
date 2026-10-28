package io.rippledown.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.chat.AiDefinitionDraftView
import io.rippledown.model.rule.AiBooleanExample
import org.junit.Rule
import org.junit.Test

class AiDraftCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val draft = AiDefinitionDraftView(
        attributeName = "hasDiabetes",
        source = "clinical_notes",
        criteria = "Personal history only",
        examples = listOf(AiBooleanExample("FH diab", false), AiBooleanExample("T2 Diab", true)),
        previewValue = "true",
        evidence = "T2 Diab",
        prompt = "Accept this definition?"
    )

    @Test
    fun `the card shows each value in a column to the right of its heading`() {
        // Given a draft offer message
        compose.setContent {
            ChatPanel(messages = listOf(AiDefinitionDraftMessage("the draft", draft)))
        }

        // Then the card's merged description is only the bot text, not the row identifiers
        compose.onNodeWithTag(AI_DRAFT_CARD).assertIsDisplayed()
            .assertContentDescriptionEquals("${BOT}0:the draft")

        // And each heading is shown with its value aligned alongside it
        listOf(
            "Draft" to "hasDiabetes",
            "Source" to "clinical_notes",
            "Criteria" to "Personal history only",
            "Examples" to "FH diab → false\nT2 Diab → true",
            "Preview" to "true",
            "Evidence" to "T2 Diab"
        ).forEach { (heading, value) ->
            val headingNode = compose.onNodeWithText(heading, useUnmergedTree = true)
            headingNode.assertIsDisplayed()
            val valueNode = compose.onNodeWithTag("$AI_DRAFT_VALUE_PREFIX$heading", useUnmergedTree = true)
            valueNode.assertIsDisplayed()
            compose.onNodeWithText(value, useUnmergedTree = true).assertIsDisplayed()

            val headingBounds = headingNode.fetchSemanticsNode().boundsInRoot
            val valueBounds = valueNode.fetchSemanticsNode().boundsInRoot
            valueBounds.top shouldBe headingBounds.top
            valueBounds.left shouldBe headingBounds.right
        }
        compose.onNodeWithText("Accept this definition?", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `a long value wraps inside its own column at a narrow width`() {
        // Given a narrow card and a criteria value too long for one line
        val longCriteria = "Personal history of type 2 diabetes mellitus documented in the clinical notes."
        compose.setContent {
            Box(Modifier.width(300.dp)) {
                ChatPanel(
                    messages = listOf(AiDefinitionDraftMessage("the draft", draft.copy(criteria = longCriteria)))
                )
            }
        }

        // Then the value still sits right of its heading and has wrapped to extra lines
        val heading = compose.onNodeWithText("Criteria", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val value = compose.onNodeWithTag("${AI_DRAFT_VALUE_PREFIX}Criteria", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        value.left shouldBe heading.right
        value.height shouldBeGreaterThan heading.height
    }

    @Test
    fun `a draft with no evaluation shows the reason and no evidence row`() {
        // Given a draft that could not be previewed
        val message = AiDefinitionDraftMessage(
            "the draft",
            draft.copy(
                previewValue = null, previewMessage = "No clinical_notes value; not evaluated.", evidence = null
            )
        )
        compose.setContent {
            ChatPanel(messages = listOf(message))
        }

        // Then the Preview row explains the absence and no Evidence row exists
        compose.onNodeWithText("No clinical_notes value; not evaluated.", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("${AI_DRAFT_VALUE_PREFIX}Evidence", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
