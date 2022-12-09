import csstype.px
import dom.html.HTMLTextAreaElement
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import mui.icons.material.QuestionMark
import mui.material.*
import mui.system.Breakpoint
import mui.system.responsive
import mui.system.sx
import react.FC
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
        rowSpacing = responsive(Pair(Breakpoint.xs, 1))

        Grid {
            container = true
            direction = responsive(GridDirection.row)
            sx {
                marginTop = 5.px
            }
            Grid {
                item = true
                xs = 11
                key = handler.interpretation.caseId.name
                TextField {
                    id = INTERPRETATION_TEXT_AREA_ID
                    minRows = 5
                    multiline = true
                    onInput = { event ->
                        interpretationText = event.target.unsafeCast<HTMLTextAreaElement>().value
                    }
                    value = interpretationText
                }
            }
            Grid {
                item = true
                xs = 1
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


/*fun main() {
    val container = document.createElement("div")

    document.body!!.appendChild(container)

    val example = InterpretationView.create {
        interpretation = Interpretation(CaseId("1"), "")
    }

    createRoot(container.unsafeCast<dom.Element>()).render(example)
}*/




