package io.rippledown.interpretation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.constants.interpretation.INTERPRETATION_VIEW_LABEL
import io.rippledown.main.Handler
import io.rippledown.model.interpretationview.ViewableInterpretation
import kotlinx.coroutines.delay

interface InterpretationViewHandler {
    var text: String
    var onEdited: (text: String) -> Unit
    var isCornertone: Boolean
}

@Composable
fun InterpretationView(handler: InterpretationViewHandler) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var entered : String by remember { mutableStateOf(handler.text) }

    //Only call the handler to update the server's version of the text after the debounce period
    LaunchedEffect(entered) {
        delay(DEBOUNCE_WAIT_PERIOD_MILLIS)
        handler.onEdited(entered)
        println("interpretation view handler called with text: '${entered}'")
    }

    OutlinedTextField(
        label = { Text(INTERPRETATION_VIEW_LABEL) },
        value = entered,
        onValueChange = {
            entered = it
        },
        modifier = Modifier
            .semantics {
                contentDescription = INTERPRETATION_TEXT_FIELD
            }
            .focusRequester(focusRequester)
            .padding(10.dp)
            .fillMaxWidth()
    )

}