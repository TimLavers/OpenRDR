package io.rippledown.rule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.main.EDIT_CONDITION_FIELD_DESCRIPTION
import io.rippledown.model.Attribute
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.condition.edit.EditableContainsCondition
import io.rippledown.model.condition.edit.EditableExtendedLowNormalRangeCondition
import io.rippledown.model.condition.episodic.signature.All
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConditionEditorTest {

    @get:Rule
    var composeTestRule = createComposeRule()
    private val glucose = Attribute(99, "Glucose")
    private lateinit var conditionBeingEdited: EditableCondition
    private lateinit var handler: ConditionEditHandler

    @Before
    fun setUp() {
        conditionBeingEdited = EditableExtendedLowNormalRangeCondition(glucose, All)
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
            requireConditionEditableTextToBe(conditionBeingEdited.editableValue().value)
            requireConditionConstantTextSecondPartToBe(conditionBeingEdited.fixedTextPart2())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `input validation`() {
        with(composeTestRule) {
            setContent {
                ConditionEditor(handler)
            }
            waitUntilExactlyOneExists(hasContentDescription(EDIT_CONDITION_FIELD_DESCRIPTION))
            requireConditionEditorOkButtonEnabled()
            enterNewVariableValueInConditionEditor("five")
            requireConditionEditorOkButtonDisabled()
            enterNewVariableValueInConditionEditor("5")
            requireConditionEditorOkButtonEnabled()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `handle ok`() {
        with(composeTestRule) {
            setContent {
                ConditionEditor(handler)
            }
            waitUntilExactlyOneExists(hasContentDescription(EDIT_CONDITION_FIELD_DESCRIPTION))
            enterNewVariableValueInConditionEditor("5")
            clickConditionEditorOkButton()
            verify {
                handler.editingFinished(conditionBeingEdited.condition("5"))
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `handle cancel`() {
        with(composeTestRule) {
            setContent {
                ConditionEditor(handler)
            }
            waitUntilExactlyOneExists(hasContentDescription(EDIT_CONDITION_FIELD_DESCRIPTION))
            enterNewVariableValueInConditionEditor("5")
            clickConditionEditorCancelButton()
            verify {
//                handler.editingFinished(conditionBeingEdited.condition("5")) wasNot Called
                handler.cancel()
            }
        }
    }
}

fun main() {
    application {
        val DEFAULT_WINDOW_SIZE = DpSize(520.dp, 180.dp)
        val windowSize by remember { mutableStateOf(DEFAULT_WINDOW_SIZE) }

        Window(
            onCloseRequest = ::exitApplication,
            state = WindowState(size = windowSize)//allow for resizing

        ) {
            val notes = Attribute(99, "Clinical Notes")
            val tsh = Attribute(100, "TSH")
            val conditionBeingEdited = EditableContainsCondition(notes, "whatever")
//            val conditionBeingEdited = EditableGreaterThanEqualsCondition(tsh, EditableValue("2.50", Type.Real), AtLeast(3))
//            val conditionBeingEdited = EditableExtendedLowNormalRangeCondition(tsh, AtLeast(3))
            val handler = mockk<ConditionEditHandler>()
            every {
                handler.editableCondition()
            }.returns(conditionBeingEdited)

        }
    }
}
