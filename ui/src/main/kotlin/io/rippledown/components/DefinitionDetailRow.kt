package io.rippledown.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A definition detail with a fixed label column and a wrapping value column. */
@Composable
internal fun DefinitionDetailRow(heading: String, value: String, valueModifier: Modifier = Modifier) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            heading,
            color = Color.DarkGray,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.width(72.dp)
        )
        Text(
            value,
            color = Color.Black,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f).then(valueModifier)
        )
    }
}
