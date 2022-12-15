import csstype.FontFamily
import csstype.FontWeight
import emotion.react.css
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import mui.icons.material.QuestionMark
import mui.material.*
import mui.system.responsive
import react.FC
import react.dom.html.ReactHTML
import react.useState

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
            ReactHTML.textarea {
                id = INTERPRETATION_TEXT_AREA_ID
                css {
                    fontWeight = FontWeight.normal
                    fontFamily = FontFamily.monospace
                }
                rows = 10
                cols = 70
                onChange = {
                    interpretationText = it.target.value
                }
                value = interpretationText
            }
            IconButton {
                QuestionMark {
                }
                onClick = {
                    println("clicked")
                }
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
                    val interpretation = Interpretation(caseId, interpretationText)
                    handler.scope.launch {
                        handler.api.saveInterpretation(interpretation)
                    }
                    interpretationText = ""
                }
            }
        }
    }
}




