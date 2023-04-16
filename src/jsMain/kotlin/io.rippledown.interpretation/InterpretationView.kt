package io.rippledown.interpretation

import ConclusionsDialog
import Handler
import csstype.FontFamily
import csstype.FontWeight
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import mui.material.*
import mui.system.responsive
import mui.system.sx
import react.FC
import react.dom.onChange
import react.useState
import xs

external interface InterpretationViewHandler : Handler {
    var interpretation: Interpretation
}

const val INTERPRETATION_TEXT_AREA_ID = "interpretation_text_area"
const val SEND_INTERPRETATION_BUTTON_ID = "send_interpretation_button"

val InterpretationView = FC<InterpretationViewHandler> { handler ->
    var interpretationText by useState(handler.interpretation.textGivenByRules())

    Grid {
        container = true
        direction = responsive(GridDirection.column)
        Grid {
            item = true
            xs = 8
            key = handler.interpretation.caseId.name
            TextField {
                id = INTERPRETATION_TEXT_AREA_ID
                fullWidth = true
                multiline = true
                sx {
                    fontWeight = FontWeight.normal
                    fontFamily = FontFamily.monospace
                }
                rows = 10
                onChange = {
                    interpretationText = it.target.asDynamic().value
                }
                defaultValue = interpretationText
            }

            ConclusionsDialog {
                interpretation = handler.interpretation
            }

        }
        Grid {
            item = true
            Button {
                +"Send interpretation"
                id = SEND_INTERPRETATION_BUTTON_ID
                variant = ButtonVariant.contained
                color = ButtonColor.primary
                size = Size.small
                onClick = {
                    val caseId = handler.interpretation.caseId
                    val interpretation = Interpretation(caseId = caseId, text = interpretationText)
                    handler.scope.launch {
                        val result = handler.api.saveInterpretation(interpretation)
                    }
                    interpretationText = ""
                }
            }
        }
    }
}




