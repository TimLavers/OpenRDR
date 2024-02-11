package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.model.TestResult

@Composable
fun RowScope.ValueCell(result: TestResult, widthWeight: Float) {
    Text(
        text = resultText(result),
        modifier = Modifier.weight(widthWeight),
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Start
    )
}
fun resultText(result: TestResult): String {
    val value = result.value.text
    return if (result.units == null) {
        value
    } else {
        "$value ${result.units}"
    }
}