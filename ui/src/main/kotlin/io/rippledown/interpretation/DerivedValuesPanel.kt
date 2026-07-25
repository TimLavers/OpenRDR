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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.caseview.ColumnWidths
import io.rippledown.constants.interpretation.*
import io.rippledown.model.caseview.DerivedValueInfo

/**
 * A collapsible panel that lists non-comment derived attribute values as
 * name/value pairs. Hovering over any part of a derived attribute row
 * (name or value) shows a tooltip with the formula and the conditions that
 * assigned the value. The value itself is not repeated in the tooltip.
 *
 * The heading is always shown, even when [derivedValues] is empty, so that
 * users discover the feature; the empty state shows a subdued
 * "None for this case" line. Hovering over the heading reveals an info icon;
 * clicking it toggles a short explainer of what derived attributes are and
 * how to create one via the chat.
 */
@Composable
fun DerivedValuesPanel(
    derivedValues: List<DerivedValueInfo>,
    columnWidths: ColumnWidths = ColumnWidths(1)
) {
    var expanded by remember { mutableStateOf(true) }
    var showHelp by remember { mutableStateOf(false) }
    val headingInteractionSource = remember { MutableInteractionSource() }
    val headingHovered by headingInteractionSource.collectIsHoveredAsState()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .hoverable(headingInteractionSource)
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
                text = "Derived attributes",
                color = Color.DarkGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            if (headingHovered || showHelp) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.padding(start = 6.dp)
                        .size(14.dp)
                        .clickable { showHelp = !showHelp }
                        .semantics { contentDescription = DERIVED_ATTRIBUTES_INFO_ICON }
                )
            }
        }
        if (showHelp) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = DERIVED_ATTRIBUTES_HELP_TEXT,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(8.dp)
                        .semantics { contentDescription = DERIVED_ATTRIBUTES_HELP }
                )
            }
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
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 8.dp)) {
                    if (derivedValues.isEmpty()) {
                        Text(
                            text = DERIVED_ATTRIBUTES_NONE_TEXT,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray,
                            modifier = Modifier.padding(2.dp)
                                .semantics { contentDescription = DERIVED_ATTRIBUTES_NONE }
                        )
                    } else {
                        derivedValues.forEach { info ->
                            DerivedValueRow(info, columnWidths)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DerivedValueRow(
    info: DerivedValueInfo,
    columnWidths: ColumnWidths = ColumnWidths(1)
) {
    TooltipArea(
        tooltip = { DerivedValueTooltip(info) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .semantics { contentDescription = "$DERIVED_VALUE_ROW_PREFIX${info.name}" }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = info.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier
                    .weight(columnWidths.attributeColumnWeight)
                    .padding(end = 12.dp)
                    .semantics { contentDescription = "$DERIVED_VALUE_NAME_PREFIX${info.name}" }
            )
            Text(
                text = info.value,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.weight(columnWidths.scrollableAreaWeight())
            )
            Spacer(
                modifier = Modifier.weight(
                    1f - columnWidths.attributeColumnWeight - columnWidths.scrollableAreaWeight()
                )
            )
        }
    }
}
