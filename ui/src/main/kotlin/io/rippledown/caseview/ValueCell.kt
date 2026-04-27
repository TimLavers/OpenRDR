package io.rippledown.caseview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.model.Attribute
import io.rippledown.model.Result

@Composable
fun RowScope.ValueCell(
    caseName: String,
    attribute: Attribute,
    index: Int,
    result: Result,
    columnWidths: ColumnWidths,
    widthWeight: Float = columnWidths.valueColumnWeight(),
    textAlign: TextAlign = TextAlign.Start
) {
    val outOfRange = result.isOutOfRange()
    val color = if (outOfRange) Color.Red else Color.Unspecified
    val weight = if (outOfRange) FontWeight.Bold else FontWeight.Normal
    val arrangement = if (textAlign == TextAlign.End) Arrangement.End else Arrangement.Start
    Row(
        // A small end-padding keeps right-aligned values from butting up
        // against the reference-range column.
        modifier = Modifier.weight(widthWeight)
            .padding(end = if (textAlign == TextAlign.End) 8.dp else 0.dp),
        horizontalArrangement = arrangement
    ) {
        // Only the numeric value is rendered here. Units live in their own
        // dedicated column on the far right of the row (see UnitsCell).
        Text(
            text = result.value.text,
            modifier = Modifier.semantics {
                contentDescription = valueCellContentDescription(caseName, attribute.name, index)
            },
            color = color,
            fontWeight = weight,
            textAlign = textAlign
        )
        if (outOfRange) {
            OutOfRangeMarker(caseName, attribute)
        }
    }
}

fun resultText(result: Result): String {
    val value = result.value.text
    return if (result.units == null) {
        value
    } else {
        "$value ${result.units!!.trim()}"
    }
}
fun valueCellContentDescriptionPrefix(caseName: String, attributeName: String) = "$caseName $attributeName value"
fun valueCellContentDescription(caseName: String, attributeName: String, index: Int) =
    "${valueCellContentDescriptionPrefix(caseName, attributeName)} $index"