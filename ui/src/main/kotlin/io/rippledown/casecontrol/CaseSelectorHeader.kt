package io.rippledown.casecontrol

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.decoration.ItalicGrey

@Composable
fun CaseSelectorHeader(numberOfCases: Int) {
    val label = "$numberOfCases cases"
    Text(
        text = label,
        style = ItalicGrey,
        modifier = Modifier
            .width(100.dp)
            .padding(start = 5.dp)
            .semantics { contentDescription = NUMBER_OF_CASES_ID }
    )
}
