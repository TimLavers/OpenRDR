@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.caseview.ColumnWidths
import io.rippledown.constants.interpretation.*
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

val DIFF_ADDITION_COLOR = androidx.compose.ui.graphics.Color(0xFFC8E6C9)
val DIFF_REMOVAL_COLOR = androidx.compose.ui.graphics.Color(0xFFFFCDD2)
val UNRESOLVED_COLOR = androidx.compose.ui.graphics.Color(0xFFFFF9C4)

interface InterpretationViewHandler : ReadonlyInterpretationViewHandler

@Composable
fun InterpretationView(
    interpretation: ViewableInterpretation,
    diff: Diff? = null,
    ruleConditions: List<String> = emptyList(),
    columnWidths: ColumnWidths = ColumnWidths(1),
    handler: InterpretationViewHandler
) {
    var commentsExpanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clickable { commentsExpanded = !commentsExpanded }
                .semantics { contentDescription = COMMENTS_TOGGLE }
        ) {
            Icon(
                imageVector = if (commentsExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Comments",
                color = Color.DarkGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            TooltipArea(
                tooltip = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = COMMENTS_HELP_TEXT,
                            style = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.widthIn(max = 400.dp).padding(8.dp)
                                .semantics { contentDescription = COMMENTS_HELP }
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.DarkGray.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 6.dp)
                        .size(14.dp)
                        .semantics { contentDescription = COMMENTS_INFO_ICON }
                )
            }
        }
        if (commentsExpanded) {
            // Match the 4.dp gap the case-list section headers leave between
            // the header and their list, for a consistent look.
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 8.dp)) {
                    if (commentRowsToDisplay(interpretation.renderedComments, diff).isEmpty()) {
                        Text(
                            text = COMMENTS_NONE_TEXT,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray,
                            modifier = Modifier.padding(2.dp)
                                .semantics { contentDescription = COMMENTS_NONE }
                        )
                    } else {
                        ReadonlyInterpretationView(
                            interpretation = interpretation,
                            diff = diff,
                            ruleConditions = ruleConditions,
                            contentDescription = INTERPRETATION_TEXT_FIELD,
                            columnWidths = columnWidths,
                            modifier = Modifier.fillMaxWidth(),
                            handler = handler
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConditionTooltip(
    conditions: List<String>,
) {
    Column {
        conditions.forEach { condition ->
            Text(
                text = condition,
                modifier = Modifier.padding(4.dp)
                    .semantics {
                        contentDescription = "$CONDITION_PREFIX$condition"
                    }
            )
        }
    }
}
