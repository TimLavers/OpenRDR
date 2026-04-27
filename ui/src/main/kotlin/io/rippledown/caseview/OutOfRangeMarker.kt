package io.rippledown.caseview

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.rippledown.constants.caseview.OUT_OF_RANGE_MARKER_DESCRIPTION_PREFIX
import io.rippledown.constants.caseview.OUT_OF_RANGE_MARKER_TEXT
import io.rippledown.model.Attribute
import io.rippledown.model.Result

/**
 * A purely visual indicator shown next to an out-of-range numeric value.
 * Rendered as a separate composable rather than appended to the value text
 * so that existing tests that read the value (e.g. `hasText("12.8")`) keep
 * working unchanged.
 */
@Composable
fun OutOfRangeMarker(caseName: String, attribute: Attribute) {
    Text(
        text = OUT_OF_RANGE_MARKER_TEXT,
        color = Color.Red,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics {
            contentDescription = outOfRangeMarkerContentDescription(caseName, attribute.name)
        }
    )
}

fun outOfRangeMarkerContentDescription(caseName: String, attributeName: String) =
    "$OUT_OF_RANGE_MARKER_DESCRIPTION_PREFIX $caseName $attributeName"

fun Result.isHigh() = referenceRange?.isHigh(value) == true
fun Result.isLow() = referenceRange?.isLow(value) == true
fun Result.isOutOfRange() = isHigh() || isLow()
