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
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.constants.main.MAIN_HEADING_ID

@Composable
@Preview
fun ApplicationNameDisplay() {
    Text(
        style = MaterialTheme.typography.h6,
        text = MAIN_HEADING,
        color = colors.onPrimary,
        fontWeight = Bold,
        modifier = Modifier
            .padding(10.dp)
            .testTag(MAIN_HEADING_ID)
            .semantics {
                contentDescription = MAIN_HEADING
            }
    )
}
