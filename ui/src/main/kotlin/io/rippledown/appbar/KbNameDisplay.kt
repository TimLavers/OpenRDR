package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign.Companion.Start
import androidx.compose.ui.unit.dp
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_NAME_ID
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.model.KBInfo

@Composable
@Preview
fun KbNameDisplay(kbInfo: KBInfo?) {
    Text(
        style = MaterialTheme.typography.h6,
        text = kbInfo?.name ?: NO_KB_SELECTED,
        color = colors.secondary,
        textAlign = Start,
        modifier = Modifier
            .testTag(KB_NAME_ID)
            .padding(10.dp)
            .semantics {
                contentDescription = KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
            }
    )
}
