package io.rippledown.interpretation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.constants.interpretation.INTERPRETATION_VIEW_LABEL

interface InterpretationViewHandler {
    var onEdited: (text: String) -> Unit
    var isCornertone: Boolean
}

@Composable
fun InterpretationView(text: String, handler: InterpretationViewHandler) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    OutlinedTextField(
        label = {
            Text(
                text = INTERPRETATION_VIEW_LABEL,
                modifier = Modifier.semantics { contentDescription = "Interpretation label" })
        },
        value = TextFieldValue(text, selection = TextRange(text.length)),
        onValueChange = {
            handler.onEdited(it.text)
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