package io.rippledown.interpretation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.DERIVED_VALUE_CONDITIONS_PREFIX
import io.rippledown.constants.interpretation.DERIVED_VALUE_FORMULA_PREFIX
import io.rippledown.model.caseview.DerivedValueInfo

@Composable
internal fun DerivedValueTooltip(info: DerivedValueInfo) {
    Column {
        if (!info.formula.isLiteralValue(info.value)) {
            Text(
                text = info.formula,
                modifier = Modifier.padding(4.dp)
                    .semantics { contentDescription = "$DERIVED_VALUE_FORMULA_PREFIX${info.formula}" }
            )
        }
        info.conditions.forEach { condition ->
            Text(
                text = condition,
                modifier = Modifier.padding(4.dp)
                    .semantics { contentDescription = "$DERIVED_VALUE_CONDITIONS_PREFIX$condition" }
            )
        }
    }
}

private fun String.isLiteralValue(value: String) = this == "\"$value\""
