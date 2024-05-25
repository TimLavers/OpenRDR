package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.constants.caseview.NUMBER_OF_CASES_LABEL

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CaseSelectorHeader(numberOfCases: Int) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.width(100.dp)
    )
    {
        Text(
            text = CASES,
            textAlign = TextAlign.Left,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.weight(1f)
                .semantics { contentDescription = NUMBER_OF_CASES_LABEL }

        )
        Text(
            text = numberOfCases.toString(),
            textAlign = TextAlign.Right,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f).padding(end = 20.dp)
                .semantics { contentDescription = NUMBER_OF_CASES_ID }
        )
    }
}
