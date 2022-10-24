import csstype.*
import emotion.react.css
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.textarea
import react.useState

external interface InterpretationViewHandler : Handler {
    var interpretation: Interpretation
    var onInterpretationSubmitted: () -> Unit
}

const val INTERPRETATION_TEXT_AREA_ID = "interpretation_text_area"
const val SEND_INTERPRETATION_BUTTON_ID = "send_interpretation_button"

val InterpretationView = FC<InterpretationViewHandler> { handler ->
    var interpretationText by useState(handler.interpretation.textGivenByRules())

    div {
        key = handler.interpretation.caseId.name
        textarea {
            id = INTERPRETATION_TEXT_AREA_ID
            css {
                fontWeight = FontWeight.normal
                fontFamily = FontFamily.monospace
            }
            rows = 10
            cols = 72
            onChange = {
                interpretationText = it.target.value
            }
            value = interpretationText
        }
        div {
            ReactHTML.button {
                +"Send interpretation"
                id = SEND_INTERPRETATION_BUTTON_ID
                css {
                    padding = px4
                }

                onClick = {
                    val caseId = handler.interpretation.caseId
                    val interpretation = Interpretation(caseId, interpretationText)
                    handler.scope.launch {
                        handler.api.saveInterpretation(interpretation)
                        handler.onInterpretationSubmitted()
                    }

                    interpretationText = ""
                }
            }
        }
    }
}