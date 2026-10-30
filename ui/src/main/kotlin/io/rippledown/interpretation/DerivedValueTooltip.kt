package io.rippledown.interpretation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.rippledown.components.DefinitionDetailRow
import io.rippledown.constants.interpretation.DERIVED_VALUE_CONDITIONS_PREFIX
import io.rippledown.constants.interpretation.DERIVED_VALUE_FORMULA_PREFIX
import io.rippledown.model.caseview.DerivedValueInfo

@Composable
internal fun DerivedValueTooltip(info: DerivedValueInfo, showFormula: Boolean = true) {
    val saved = info.provenance
    if (saved != null) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            contentColor = Color.Black,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DefinitionDetailRow("Name", info.name)
                DefinitionDetailRow("Refers to", saved.sourceName)
                DefinitionDetailRow("Criteria", saved.instructions)
                DefinitionDetailRow("Evidence", saved.evidence ?: "No supporting excerpt returned")
                ConditionTooltip(info.conditions, formalDescriptionPrefix = DERIVED_VALUE_CONDITIONS_PREFIX)
            }
        }
        return
    }
    Column {
        if (showFormula && info.formula.isNotBlank() && !info.formula.isLiteralValue(info.value)) {
            Text(
                text = formulaAnnotatedString(info.formula),
                modifier = Modifier.padding(4.dp)
                    .semantics { contentDescription = "$DERIVED_VALUE_FORMULA_PREFIX${info.formula}" }
            )
        }
        ConditionTooltip(info.conditions, formalDescriptionPrefix = DERIVED_VALUE_CONDITIONS_PREFIX)
    }
}

private val powerRegex = """(?:[*][*]|\^)[ \t]*([0-9]+(?:[.][0-9]+)?)""".toRegex()

internal fun formulaAnnotatedString(formula: String) = buildAnnotatedString {
    var cursor = 0
    for (match in powerRegex.findAll(formula)) {
        append(formula.substring(cursor, match.range.first))
        val exponent = match.groupValues[1]
        withStyle(
            SpanStyle(
                baselineShift = BaselineShift.Superscript,
                fontSize = 0.85.em,
                fontStyle = FontStyle.Italic
            )
        ) {
            append(exponent)
        }
        cursor = match.range.last + 1
    }
    append(formula.substring(cursor))
    addStyle(SpanStyle(fontStyle = FontStyle.Italic), 0, length)
}

private fun String.isLiteralValue(value: String) = this == "\"$value\""
