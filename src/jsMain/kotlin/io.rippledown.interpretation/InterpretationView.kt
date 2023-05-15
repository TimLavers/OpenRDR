package io.rippledown.interpretation

import ConclusionsDialog
import Handler
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import mui.material.Grid
import mui.material.GridDirection
import mui.material.TextField
import mui.system.responsive
import mui.system.sx
import npm.debounce
import react.FC
import react.dom.onChange
import react.useState
import web.cssom.FontFamily
import web.cssom.FontWeight
import web.html.HTMLDivElement
import xs

external interface InterpretationViewHandler : Handler {
    var interpretation: Interpretation
    var onInterpretationEdited: (interp: Interpretation) -> Unit
}

typealias FormEventAlias = (react.dom.events.FormEvent<HTMLDivElement>) -> Unit

val InterpretationView = FC<InterpretationViewHandler> { handler ->
    val interp = handler.interpretation
    var latestText by useState(interp.latestText())

    fun handleFormEvent(): FormEventAlias {
        return {
            handler.scope.launch {
                val changed = it.target.asDynamic().value
                latestText = changed
                interp.verifiedText = changed
                val updatedInterpretation = handler.api.saveVerifiedInterpretation(interp)
                handler.onInterpretationEdited(updatedInterpretation)
            }
        }
    }

    fun debounceFunction(): FormEventAlias {
        return debounce(handleFormEvent(), DEBOUNCE_WAIT_PERIOD_MILLIS)
    }

    Grid {
        container = true
        direction = responsive(GridDirection.column)
        Grid {
            item = true
            xs = 8
            key = interp.caseId.name
            TextField {
                id = INTERPRETATION_TEXT_AREA
                fullWidth = true
                multiline = true
                sx {
                    fontWeight = FontWeight.normal
                    fontFamily = FontFamily.monospace
                }
                rows = 10
                onChange = debounceFunction()
                defaultValue = latestText
            }

            ConclusionsDialog {
                this.interpretation = handler.interpretation
            }

        }
    }
}




