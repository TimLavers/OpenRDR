@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.CONDITION_PREFIX
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.interpretationview.ViewableInterpretation

val DIFF_ADDITION_COLOR = androidx.compose.ui.graphics.Color(0xFFC8E6C9)
val DIFF_REMOVAL_COLOR = androidx.compose.ui.graphics.Color(0xFFFFCDD2)

interface InterpretationViewHandler : ReadonlyInterpretationViewHandler

@Composable
fun InterpretationView(
    interpretation: ViewableInterpretation,
    diff: Diff? = null,
    handler: InterpretationViewHandler
) {
    OutlinedCard(
        modifier = Modifier.padding(vertical = 10.dp),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ReadonlyInterpretationView(
                    interpretation = interpretation,
                    diff = diff,
                    contentDescription = INTERPRETATION_TEXT_FIELD,
                    modifier = Modifier.weight(1f),
                    handler = handler
                )
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

fun List<String>.unhighlighted(diff: Diff? = null) = highlightItem(-1, diff)

fun List<String>.highlightItem(index: Int, diff: Diff? = null) = buildAnnotatedString {
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