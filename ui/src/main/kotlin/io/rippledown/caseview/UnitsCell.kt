package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import io.rippledown.constants.caseview.UNITS_CELL_DESCRIPTION_PREFIX
import io.rippledown.model.Attribute
import io.rippledown.model.Result

@Composable
fun RowScope.UnitsCell(attribute: Attribute, result: Result, widthWeight: Float) {
    val raw = result.units?.trim().orEmpty()
    Text(
        text = formatUnits(raw),
        modifier = Modifier.weight(widthWeight)
            .semantics {
                contentDescription = unitsCellContentDescription(attribute.name)
            },
        textAlign = TextAlign.Start
    )
}

fun unitsCellContentDescription(attributeName: String) = "$UNITS_CELL_DESCRIPTION_PREFIX $attributeName"

private val microPrefixRegex = Regex("""\bu(?=[a-zA-Z])""")
private val powerOfTenRegex = Regex("""10\^(-?\d+)""")

/**
 * Pretty-print a units string for display:
 *  - lowercase 'u' used as the SI micro prefix is shown as the Greek
 *    letter mu (e.g. `umol/L` -> `μmol/L`).
 *  - `10^N` exponents are rendered with N as a superscript
 *    (e.g. `10^9/L` -> `10` followed by a superscript `9`, then `/L`).
 *
 * The transformation is purely visual; the underlying `Result.units`
 * string is unchanged.
 */
fun formatUnits(units: String): AnnotatedString {
    val withMu = units.replace(microPrefixRegex, "μ")
    return buildAnnotatedString {
        var cursor = 0
        for (match in powerOfTenRegex.findAll(withMu)) {
            append(withMu.substring(cursor, match.range.first))
            append("10")
            withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em)) {
                append(match.groupValues[1])
            }
            cursor = match.range.last + 1
        }
        append(withMu.substring(cursor))
    }
}
