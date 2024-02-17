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
