package io.rippledown.interpretation

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA

interface InterpretationViewHandler {
    var text: String
    var onEdited: (text: String) -> Unit
    var isCornertone: Boolean
}

@Composable
fun InterpretationView(handler: InterpretationViewHandler) {

    Text(
        text = handler.text,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .semantics {
                contentDescription = INTERPRETATION_TEXT_AREA
            }
    )

}
/*

package io.rippledown.interpretation

import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_AREA
import io.rippledown.main.Handler
import mui.material.TextField
import mui.system.sx
import npm.debounce
import react.FC
import react.dom.events.FormEvent
import react.dom.onChange
import react.useState
import web.cssom.Border
import web.cssom.FontFamily.Companion.monospace
import web.cssom.FontWeight.Companion.normal
import web.cssom.LineStyle
import web.cssom.ch
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
            border = Border(0.ch, LineStyle.hidden)
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
 */