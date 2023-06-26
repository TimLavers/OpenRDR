package io.rippledown.interpretation

import Handler
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import mui.material.Button
import mui.material.TextField
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.VFC
import react.dom.createRootFor
import react.dom.test.act
import react.useState
import kotlin.test.Test

class InterpretationViewUpdateTest {

    @Test
    fun shouldUpdateTextFieldWhenInterpretationIsChanged() = runTest {
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"
        val buttonId = "button_id"

        val interpA = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(0, caseAConclusion)))
        }
        val interpB = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(1, caseBConclusion)))
        }

        val vfc = VFC {
            var interp by useState(interpA)

            Button {
                id = buttonId
                onClick = {
                    interp = interpB
                }
            }

//            IV{
//                interpretation = interp
//            }
            /*
                        TextField {
                            id = INTERPRETATION_TEXT_AREA
                            multiline = true
                            defaultValue = interp.latestText()
                        }
            */

            InterpretationView {
                interpretation = interp
            }
        }
        with(createRootFor(vfc)) {
            requireInterpretation(caseAConclusion)

            //switch interpretations
            act { findById(buttonId).click() }
            waitForEvents()

            requireInterpretation(caseBConclusion)
        }
    }

    val IV = FC<IVHandler> { props ->
        val text = props.interpretation.latestText()
//        val text by useState(props.interpretation.latestText())
        TextField {
            id = INTERPRETATION_TEXT_AREA
            multiline = true
            defaultValue = text
        }

    }
}

external interface IVHandler : Handler {
    var interpretation: Interpretation
}
