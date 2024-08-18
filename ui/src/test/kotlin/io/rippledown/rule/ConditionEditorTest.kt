package io.rippledown.rule

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.main.EDIT_CONDITION_FIELD_DESCRIPTION
import io.rippledown.model.Attribute
import io.rippledown.model.condition.edit.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConditionEditorTest {

    @get:Rule
    var composeTestRule = createComposeRule()
    private val notes = Attribute(99, "Notes")
    private lateinit var conditionBeingEdited: EditableCondition
    private lateinit var handler: ConditionEditHandler

    @Before
    fun setUp() {
        conditionBeingEdited = EditableContainsCondition(notes, "whatever")
        handler = mockk(relaxed = true)
        every {
            handler.editableCondition()
        }.returns(conditionBeingEdited)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `initial value`() {
        with(composeTestRule) {
            setContent {
                ConditionEditor(handler)
            }
            waitUntilExactlyOneExists(hasContentDescription(EDIT_CONDITION_FIELD_DESCRIPTION))
            requireConditionConstantTextFirstPartToBe(conditionBeingEdited.fixedTextPart1())
        }
    }
}