import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.textarea
import react.key

external interface InterpretationViewHandler : Props {
    var case: RDRCase
    var onInterpretationSubmitted: (Interpretation) -> Unit
}

const val SEND_INTERPRETATION_BUTTON_ID = "send_interpretation_button"

val InterpretationView = FC<InterpretationViewHandler> { props ->
    var interpretationText = props.case.interpretation.textGivenByRules()

    div {
        key = props.case.name
        textarea {
            id = "interpretation_text_area"
            rows = 10
            cols = 72
            defaultValue = interpretationText
            onChange = {
                interpretationText = it.target.value
            }
        }
        div {
            ReactHTML.button {
                +"Send interpretation"
                id = SEND_INTERPRETATION_BUTTON_ID
                css {
                    padding = px4
                }

                onClick = {
                    val caseId = CaseId(props.case.name, props.case.name)
                    val interpretation = Interpretation(caseId, interpretationText)
                    props.onInterpretationSubmitted(interpretation)
                    interpretationText = ""
                }
            }
        }
    }
}