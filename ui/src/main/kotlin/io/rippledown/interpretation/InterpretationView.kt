package io.rippledown.interpretation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.constants.interpretation.INTERPRETATION_VIEW_LABEL

@Composable
fun InterpretationView(text: String) {
    OutlinedTextField(
        readOnly = true,
        label = {
            Text(
                text = INTERPRETATION_VIEW_LABEL,
                modifier = Modifier.semantics { contentDescription = "Interpretation label" })
        },
        value = TextFieldValue(text, selection = TextRange(text.length)),
        onValueChange = {},
        modifier = Modifier
            .semantics {
                contentDescription = INTERPRETATION_TEXT_FIELD
            }
            .padding(10.dp)
            .fillMaxWidth()
    )
}