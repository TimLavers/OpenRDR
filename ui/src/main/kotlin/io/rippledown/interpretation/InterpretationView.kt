@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.interpretation.COMMENTS_TOGGLE
import io.rippledown.constants.interpretation.CONDITION_PREFIX
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.model.IntRangeData
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ReadonlyInterpretationView(
                            interpretation = interpretation,
                            diff = diff,
                            ruleConditions = ruleConditions,
                            contentDescription = INTERPRETATION_TEXT_FIELD,
                            modifier = Modifier.weight(1f),
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

fun List<String>.commentIndexForOffset(offset: Int): Int {
    var currentOffset = 0
    forEachIndexed { i, comment ->
        if (offset in currentOffset until currentOffset + comment.length) {
            return i
        }
        currentOffset += comment.length
    }
    return -1
}

fun List<String>.unhighlighted(diff: Diff? = null, unresolvedRanges: List<List<IntRangeData>> = emptyList()) =
    highlightItem(-1, diff, unresolvedRanges)

fun List<String>.highlightItem(
    index: Int,
    diff: Diff? = null,
    unresolvedRanges: List<List<IntRangeData>> = emptyList()
) = buildAnnotatedString {
    forEachIndexed { i, text ->
        val isDiffTarget = when (diff) {
            is Removal -> text == diff.removedText
            is Replacement -> text == diff.originalText
            else -> false
        }

        if (i == index && !isDiffTarget) {
            addStyle(
                style = SpanStyle(
                    background = io.rippledown.decoration.BACKGROUND_COLOR
                ),
                start = length,
                end = length + text.length
            )
        }
        if (isDiffTarget) {
            val start = length
            append(text)
            addStyle(SpanStyle(background = DIFF_REMOVAL_COLOR), start, length)
        } else {
            append(text)
        }

        // Apply unresolved range styling
        if (i < unresolvedRanges.size) {
            unresolvedRanges[i].forEach { range ->
                val rangeStart = length - text.length + range.start
                val rangeEnd = length - text.length + range.endInclusive + 1
                addStyle(SpanStyle(background = UNRESOLVED_COLOR), rangeStart, rangeEnd)
            }
        }

        if (diff is Replacement && text == diff.originalText) {
            append(" ")
            val start = length
            append(diff.replacementText)
            addStyle(SpanStyle(background = DIFF_ADDITION_COLOR), start, length)
        }

        if (i < size - 1) {
            // Add space before the next comment
            append(" ")
        }
    }
    if (diff is Addition) {
        if (isNotEmpty()) append(" ")
        val start = length
        append(diff.addedText)
        addStyle(SpanStyle(background = DIFF_ADDITION_COLOR), start, length)
    }
}