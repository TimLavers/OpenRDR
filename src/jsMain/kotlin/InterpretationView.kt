import dom.html.HTMLTextAreaElement
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import mui.icons.material.QuestionMark
import mui.material.*
import mui.system.Breakpoint
import mui.system.responsive
import react.FC
import react.ReactNode
import react.useState

inline var GridProps.xs: Any?
    get() = asDynamic().xs
    set(value) {
        asDynamic().xs = value
    }

external interface InterpretationViewHandler : Handler {
    var interpretation: Interpretation
}

const val INTERPRETATION_TEXT_AREA_ID = "interpretation_text_area"
const val SEND_INTERPRETATION_BUTTON_ID = "send_interpretation_button"

val InterpretationView = FC<InterpretationViewHandler> { handler ->
    var interpretationText by useState(handler.interpretation.textGivenByRules())

    Grid {
        container = true
        direction = responsive(GridDirection.row)
        rowSpacing = responsive(Pair(Breakpoint.xs, 2))
        Grid {
            container = true
            Grid {
                item = true
                xs = 4
                key = handler.interpretation.caseId.name
                TextField {
                    id = INTERPRETATION_TEXT_AREA_ID
                    label = "Interpretation".unsafeCast<ReactNode>()
                    variant = FormControlVariant.filled
                    minRows = 5
                    multiline = true
                    fullWidth = true
                    onInput = { event ->
                        interpretationText = event.target.unsafeCast<HTMLTextAreaElement>().value
                    }
                    value = interpretationText
                }
            }
            Grid {
                item = true
                IconButton {
                    QuestionMark {
                    }
                    onClick = {
                        println("clicked")
                    }
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


/*
fun main() {
    val container = document.createElement("div")
    document.body!!.appendChild(container)

    val example = InterpretationView.create {
        interpretation = Interpretation(CaseId("1"), "")
    }

    createRoot(container.unsafeCast<dom.Element>()).render(example)
}
*/


