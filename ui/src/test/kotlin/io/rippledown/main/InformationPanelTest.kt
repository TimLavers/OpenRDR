package io.rippledown.main

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.AnnotatedString
import io.rippledown.interpretation.toAnnotatedString
import io.rippledown.model.diff.Addition
import io.rippledown.utils.applicationFor
import org.junit.Rule
import org.junit.Test

class InformationPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should display message to the right`() {
        with(composeTestRule) {
            // given
            val leftMessage = AnnotatedString("This is a message for the left")
            val rightMessage = AnnotatedString("This is a message for the right")

            // when
            setContent {
                InformationPanel(leftMessage, rightMessage)
            }

            // then
            onNodeWithContentDescription(LEFT_INFO_MESSAGE_ID).assertTextEquals(leftMessage.text)
            onNodeWithContentDescription(RIGHT_INFO_MESSAGE_ID).assertTextEquals(rightMessage.text)

        }
    }
}

fun main() {
    applicationFor {
        InformationPanel(
            Addition("Going to Bondi").toAnnotatedString(),
            AnnotatedString("This is a message for the right")
        )
    }
}