package io.rippledown.interpretation

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.TITLE
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show initial non-blank interpretation`() = runTest {
        val initialText = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                InterpretationView("", object : Handler by handlerImpl, InterpretationViewHandler {
                    //                    override var text = initialText
                    override var onEdited = { _: String -> }
                    override var isCornertone = false
                })
            }
            requireInterpretation(initialText)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationView("", object : Handler by handlerImpl, InterpretationViewHandler {
                    //                    override var text = ""
                    override var onEdited = { _: String -> }
                    override var isCornertone = false
                })
            }
            requireInterpretation("")
        }
    }

    @Test
    fun `should call OnEdited once the text is changed`() = runTest {
        val enteredText = "And bring your flippers"
        var updatedText = ""

        with(composeTestRule) {
            setContent {
                InterpretationView("", object : Handler by handlerImpl, InterpretationViewHandler {
                    override var onEdited = { changed: String -> updatedText = changed }
                    override var isCornertone = false
                })
            }
            //Given
            updatedText shouldBe ""

            //When
            enterInterpretationAndWaitForUpdate(enteredText)

            //Then
            updatedText shouldBe enteredText
        }
    }

    @Test
    fun `should no call OnEdited if the text has not changed`() = runTest {
        var onEditedCalled = false

        with(composeTestRule) {
            setContent {
                InterpretationView("", object : Handler by handlerImpl, InterpretationViewHandler {
                    override var onEdited = { changed: String -> onEditedCalled = true }
                    override var isCornertone = false
                })
            }
            //Given

            //When

            //Then
            onEditedCalled shouldBe false
        }
    }


}


fun main() {

    application {
        Window(
            onCloseRequest = ::exitApplication,
            icon = painterResource("water-wave-icon.png"),
            title = TITLE
        ) {
            InterpretationView("", object : Handler by handlerImpl, InterpretationViewHandler {
                override var onEdited = { entered: String -> println("onEdited $entered") }
                override var isCornertone = false
            })
        }
    }
}