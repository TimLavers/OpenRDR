package io.rippledown.interpretation

import Handler
import debug
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
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
    var text: String
    var onEdited: (text: String) -> Unit
    var isCornerstone: Boolean
}

typealias FormEventAlias = (react.dom.events.FormEvent<HTMLDivElement>) -> Unit

val InterpretationView = FC<InterpretationViewHandler> { handler ->
    var text by useState(handler.text)

    fun handleFormEvent(): FormEventAlias {
        return {
            val changed = it.target.asDynamic().value
            text = changed
            handler.onEdited(changed)
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
        key = handler.text
        value = text
        debug("InterpretationView: text=$text")
    }
}




