package io.rippledown.interpretation

import Handler
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.model.Interpretation
import kotlinx.coroutines.launch
import mui.material.MuiInputBase.Companion.readOnly
import mui.material.TextField
import mui.system.sx
import npm.debounce
import react.FC
import react.dom.aria.ariaReadOnly
import react.dom.onChange
import react.useState
import web.cssom.FontFamily
import web.cssom.FontWeight
import web.html.HTMLDivElement

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

    TextField {
        id = INTERPRETATION_TEXT_AREA
        inputProps = js("{readonly:true}")
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
}




