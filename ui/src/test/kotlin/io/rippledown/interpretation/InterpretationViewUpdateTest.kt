package io.rippledown.interpretation

import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.kotest.assertions.withClue
import io.mockk.mockk
import io.rippledown.model.Interpretation
import io.rippledown.model.RenderedComment
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import io.rippledown.utils.commentAssignment
import io.rippledown.utils.commentAttributeName
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

/**
 * Tests of the key function
 */
class InterpretationViewUpdateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: InterpretationViewHandler

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
    }

    val textA = "text for case A"
    val textB = "text for case B"
    val buttonTag = "buttonTag"


    @Test
    fun `should update interpretation when the interpretation text is changed`() = runTest {
        val nameA = commentAttributeName(1)
        val nameB = commentAttributeName(2)
        val i1 = Interpretation().apply { add(RuleSummary(assignment = commentAssignment(1, textA))) }
        val i2 = Interpretation().apply { add(RuleSummary(assignment = commentAssignment(2, textB))) }
        val original = ViewableInterpretation(
            i1,
            textGivenByRules = textA,
            renderedComments = listOf(RenderedComment(textA, name = nameA))
        )
        val changed = ViewableInterpretation(
            i2,
            textGivenByRules = textB,
            renderedComments = listOf(RenderedComment(textB, name = nameB))
        )

        with(composeTestRule) {
            setContent {
                InterpretationViewWithButton(original, changed, handler)
            }
            //Given
            requireInterpretation(textA)

            //When
            onNodeWithTag(buttonTag).performClick()

            //Then
            requireInterpretation(textB)
            requireCommentNames(nameB)
            withClue("check the interpretation is still being displayed when the pointer is moved over the comment") {
                movePointerOverCommentRow(nameB)
                requireInterpretation(textB)
            }
        }
    }

    @Composable
    fun InterpretationViewWithButton(
        original: ViewableInterpretation,
        changed: ViewableInterpretation,
        handler: InterpretationViewHandler
    ) {
        var viewableInterpretation: ViewableInterpretation by remember { mutableStateOf(original) }

        InterpretationView(viewableInterpretation, handler = handler)

        Button(
            onClick = {
                viewableInterpretation = changed
            },
            modifier = Modifier.testTag(buttonTag)
        ) {
            // Button text
        }
    }
}