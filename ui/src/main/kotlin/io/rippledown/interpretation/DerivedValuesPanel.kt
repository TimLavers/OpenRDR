@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.interpretation.*
import io.rippledown.model.caseview.DerivedValueInfo

/**
 * A collapsible panel that lists non-comment derived attribute values as
 * name/value pairs. Hovering over a derived attribute name shows a tooltip
 * with the formula and the conditions that assigned the value.
 *
 * The panel is hidden entirely when [derivedValues] is empty.
 */
@Composable
fun DerivedValuesPanel(
    derivedValues: List<DerivedValueInfo>
) {
    if (derivedValues.isEmpty()) return

    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { contentDescription = DERIVED_VALUES_TOGGLE }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Derived values",
                color = Color.DarkGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = DERIVED_VALUES_PANEL },
                colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                    containerColor = Color.White
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    derivedValues.forEach { info ->
                        DerivedValueRow(info)
                    }
                }
            }
        }
    }
}

@Composable
private fun DerivedValueRow(info: DerivedValueInfo) {
    val nameInteractionSource = remember { MutableInteractionSource() }
    val isNameHovered by nameInteractionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .semantics { contentDescription = "$DERIVED_VALUE_ROW_PREFIX${info.name}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        TooltipArea(
            modifier = Modifier
                .hoverable(nameInteractionSource)
                .semantics { contentDescription = "$DERIVED_VALUE_NAME_PREFIX${info.name}" },
            tooltip = {
                DerivedValueTooltip(info)
            }
        ) {
            Text(
                text = info.name,
                fontWeight = FontWeight.Bold,
                color = if (isNameHovered) MaterialTheme.colorScheme.primary else Color.DarkGray,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            text = info.value,
            fontSize = 13.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun DerivedValueTooltip(info: DerivedValueInfo) {
    Column {
        Text(
            text = info.formula,
            modifier = Modifier.padding(4.dp)
                .semantics { contentDescription = "$DERIVED_VALUE_FORMULA_PREFIX${info.formula}" }
        )
        info.conditions.forEach { condition ->
            Text(
                text = condition,
                modifier = Modifier.padding(4.dp)
                    .semantics { contentDescription = "$DERIVED_VALUE_CONDITIONS_PREFIX$condition" }
            )
        }
    }
}
