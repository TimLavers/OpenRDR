package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RowScope.AttributesHeaderCell(text: String, widthWeight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(widthWeight),
        fontWeight = FontWeight.Bold
    )
}
