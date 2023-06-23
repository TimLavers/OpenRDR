package io.rippledown.interpretation

import Handler
import debug
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import mui.material.TextField
import mui.system.sx
import npm.debounce
import react.FC
import react.dom.onChange
import react.useState
import web.cssom.FontFamily
import web.cssom.FontWeight
import web.html.HTMLDivElement

external interface InterpretationViewHandler : Handler {
    var interpretation: Interpretation
    var onInterpretationEdited: (interp: Interpretation) -> Unit
    var isCornerstone: Boolean
}

typealias FormEventAlias = (react.dom.events.FormEvent<HTMLDivElement>) -> Unit

val InterpretationView = FC<InterpretationViewHandler> { handler ->

    val interp = handler.interpretation
    debug("InterpretationView called with '${interp.latestText()}'")

    fun handleFormEvent(): FormEventAlias {
        return {
            handler.scope.launch {
                val changed = it.target.asDynamic().value
                debug("handleFormEvent called with $changed")
                handler.interpretation.verifiedText = changed
                val updatedInterpretation = handler.api.saveVerifiedInterpretation(handler.interpretation)
                handler.onInterpretationEdited(updatedInterpretation)
            }
        }
    }

    fun debounceFunction(): FormEventAlias {
        return debounce(handleFormEvent(), DEBOUNCE_WAIT_PERIOD_MILLIS)
    }

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
        debug("set default value in text area with '${handler.interpretation.latestText()}' for interpretation ${handler.interpretation.caseId}")
        defaultValue = handler.interpretation.latestText()

    }
}




