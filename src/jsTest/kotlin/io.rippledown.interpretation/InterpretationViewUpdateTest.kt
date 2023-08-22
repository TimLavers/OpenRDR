package io.rippledown.interpretation

import kotlinx.coroutines.test.runTest
import mui.material.Button
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.useState
import kotlin.test.Test

class InterpretationViewUpdateTest {

    @Test
    fun shouldUpdateTextFieldWhenInterpretationIsChanged() = runTest {
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
                key = currentText //This is important! Force re-render when the current text changes
                InterpretationView {
                    text = currentText
                }
            }
        }
        with(createRootFor(fc)) {
            requireInterpretation(textA)

            //switch interpretations
            act { findById(buttonId).click() }
            waitForEvents()

            requireInterpretation(textB)
        }
    }
}