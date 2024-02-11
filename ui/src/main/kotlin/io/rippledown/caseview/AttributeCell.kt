package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.model.Attribute

@Composable
fun RowScope.AttributeCell(attribute: Attribute, widthWeight: Float) {
    Text(
        text = attribute.name,
        modifier = Modifier.weight(widthWeight),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
    )
}