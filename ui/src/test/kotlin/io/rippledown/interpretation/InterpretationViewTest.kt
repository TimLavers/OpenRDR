package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: InterpretationViewHandler

    @Before
    fun setUp() {
        handler = mockk<InterpretationViewHandler>(relaxed = true)
    }

    @Test
    fun `should show initial non-blank interpretation`() = runTest {
        val initialText = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                InterpretationView(initialText, handler)
            }
            requireInterpretation(initialText)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationView("", handler)
            }
            requireInterpretation("")
        }
    }

    @Test
    fun `should call OnEdited once the text is changed`() = runTest {
        //Given
        with(composeTestRule) {
            setContent {
                InterpretationView("", handler)
            }

            //When
            val enteredText = "And bring your flippers"
            enterInterpretation(enteredText)

            //Then
            verify { handler.onEdited(enteredText) }
        }
    }

    @Test
    fun `should not call OnEdited if the text has not changed`() = runTest {

        with(composeTestRule) {
            //Given
            setContent {
                InterpretationView("Go to Bondi", handler)
            }

            //When

            //Then
            verify { handler.onEdited wasNot Called }
        }
    }
}


fun main() {

    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            InterpretationView("", object : InterpretationViewHandler {
                override var onEdited = { entered: String -> println("onEdited $entered") }
                override var isCornertone = false
            })
        }
    }
}