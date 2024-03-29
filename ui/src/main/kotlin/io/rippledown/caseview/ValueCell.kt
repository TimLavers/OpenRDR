package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.model.Attribute
import io.rippledown.model.TestResult

@Composable
fun RowScope.ValueCell(
    caseName: String,
    attribute: Attribute,
    index: Int,
    result: TestResult,
    columnWidths: ColumnWidths
) {
    Text(
        text = resultText(result),
        modifier = Modifier.weight(columnWidths.valueColumnWeight())
            .semantics {
                contentDescription = valueCellContentDescription(caseName, attribute.name, index)
            },
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}
fun resultText(result: TestResult): String {
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