@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.CONDITION_PREFIX
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE
import io.rippledown.model.interpretationview.ViewableInterpretation

val BACKGROUND_COLOR = Color.LightGray

interface InterpretationViewHandler {
    fun onTextLayoutResult(layoutResult: TextLayoutResult)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InterpretationView(
    interpretation: ViewableInterpretation,
    isCornerstone: Boolean,
    handler: InterpretationViewHandler? = null
) {
    val conclusionList = interpretation.conclusions().toList()
    var comments by remember {
        mutableStateOf(interpretation.conclusions().map { it.text })
    }
    var unstyledText by remember { mutableStateOf(comments.unhighlighted()) }
    var styledText by remember { mutableStateOf(unstyledText) }
    var commentIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(interpretation) {
        comments = interpretation.conclusions().map { it.text }
        unstyledText = comments.unhighlighted()
        styledText = unstyledText
    }

    OutlinedCard(modifier = Modifier.padding(vertical = 10.dp)) {
        TooltipArea(
            tooltip = {
                val showToolTip = commentIndex != -1
                if (showToolTip) {
                    ConditionTooltip(interpretation.conditionsForConclusion(conclusionList[commentIndex]))
                }
            },
            content = {
                AnnotatedTextView(
                    text = if (commentIndex == -1) unstyledText else styledText,
                    description = if (isCornerstone) INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE else INTERPRETATION_TEXT_FIELD,
                    handler = object : AnnotatedTextViewHandler {
                        override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                            handler?.onTextLayoutResult(layoutResult)
                        }

                        override fun onPointerEnter(characterOffset: Int) {
                            commentIndex = comments.commentIndexForOffset(characterOffset)
                            styledText = comments.highlightItem(commentIndex)
                        }

                        override fun onPointerExit() {
                            commentIndex = -1
                        }
                    }
                )
            }
        )
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

fun List<String>.unhighlighted() = highlightItem(-1)

fun List<String>.highlightItem(index: Int) = buildAnnotatedString {
    forEachIndexed { i, text ->
        if (i == index) {
            addStyle(
                style = SpanStyle(
                    background = BACKGROUND_COLOR
                ),
                start = length,
                end = length + text.length
            )
        }
        append(text)
        if (i < size - 1) {
            // Add space before the next comment
            append(" ")
        }
    }
}