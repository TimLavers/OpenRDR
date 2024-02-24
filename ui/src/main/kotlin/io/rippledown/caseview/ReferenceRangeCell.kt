package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.constants.caseview.REFERENCE_RANGE_CELL_DESCRIPTION_PREFIX
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult

@Composable
fun RowScope.ReferenceRangeCell(attribute: Attribute, result: TestResult, widthWeight: Float) {
    Text(
        text = rangeText(result.referenceRange),
        modifier = Modifier.weight(widthWeight)
            .semantics{
                contentDescription = referenceRangeCellContentDescription(attribute.name)
            },
        textAlign = TextAlign.Start
    )
}
fun referenceRangeCellContentDescription(attributeName: String) = "$REFERENCE_RANGE_CELL_DESCRIPTION_PREFIX $attributeName"

fun rangeText(referenceRange: ReferenceRange?) =
    with(referenceRange) {
        when {
            this == null -> ""
            lowerString == null && upperString == null -> ""
            lowerString == null -> "< $upperString"
            upperString == null -> "> $lowerString"
            else -> "$lowerString - $upperString"
        }
    }