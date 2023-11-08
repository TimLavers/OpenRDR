package io.rippledown.interpretation

import kotlinx.coroutines.test.TestResult
import mui.material.Button
import proxy.findById
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.dom.test.runReactTest
import react.useState
import kotlin.test.Test

class InterpretationViewUpdateTest {

    @Test
    fun shouldUpdateTextFieldWhenInterpretationIsChanged(): TestResult {
        val textA = "text for case A"
        val textB = "text for case B"
        val buttonId = "button_id"

        val fc = FC {
            var currentText by useState(textA)

            Button {
                id = buttonId
                onClick = {
                    currentText = textB
                }
            }
            div {
                key = interpretationViewKey(currentText) //Re-render when the current text changes
                InterpretationView {
                    text = currentText
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                requireInterpretation(textA)

                //When switch interpretations
                act { findById(buttonId).click() }

                //Then
                requireInterpretation(textB)
            }
        }
    }
}