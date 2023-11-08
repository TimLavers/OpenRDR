package io.rippledown.interpretation

import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import main.Handler
import mui.material.TextField
import mui.system.sx
import npm.debounce
import react.FC
import react.dom.events.FormEvent
import react.dom.onChange
import react.useState
import web.cssom.FontFamily.Companion.monospace
import web.cssom.FontWeight.Companion.normal
import web.html.HTMLDivElement

external interface InterpretationViewHandler : Handler {
    var text: String
    var onEdited: (text: String) -> Unit
    var isCornerstone: Boolean
}

val InterpretationView = FC<InterpretationViewHandler> { handler ->
    var currentText by useState(handler.text)

    fun handleFormEvent(event: FormEvent<HTMLDivElement>) {
        val changed = event.target.asDynamic().value
        currentText = changed
        handler.onEdited(changed)
    }

    TextField {
        id = INTERPRETATION_TEXT_AREA
        fullWidth = true
        multiline = true
        sx {
            fontWeight = normal
            fontFamily = monospace
        }
        rows = 10
        autoFocus = true
        onChange = debounce(func = ::handleFormEvent, wait = DEBOUNCE_WAIT_PERIOD_MILLIS)
        defaultValue = currentText
        onFocus = {
            val length = it.currentTarget.asDynamic().value.length
            it.currentTarget.asDynamic().setSelectionRange(length, length)
        }
    }
}