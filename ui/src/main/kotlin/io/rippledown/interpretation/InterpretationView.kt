@file:OptIn(
    ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class, ExperimentalFoundationApi::class
)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.CONDITION_PREFIX
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.model.interpretationview.ViewableInterpretation

interface InterpretationViewHandler : ReadonlyInterpretationViewHandler, InterpretationActionsHandler {
    fun allComments(): Set<String>
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InterpretationView(
    interpretation: ViewableInterpretation,
    showChangeIcon: Boolean,
    handler: InterpretationViewHandler
) {
    var comments by remember {
        mutableStateOf(interpretation.conclusions().map { it.text })
    }

    LaunchedEffect(interpretation) {
        comments = interpretation.conclusions().map { it.text }
    }

    OutlinedCard(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ReadonlyInterpretationView(
                interpretation = interpretation,
                contentDescription = INTERPRETATION_TEXT_FIELD,
                modifier = Modifier.weight(1f), // Fill the available space, but leaving space for the icon
                handler = handler
            )
            if (showChangeIcon) {
                InterpretationActions(
                    commentsGivenForCase = comments,
                    allComments = handler.allComments(),
                    handler = object : InterpretationActionsHandler {
                        override fun startRuleToAddComment(comment: String) {
                            handler.startRuleToAddComment(comment)
                        }

                        override fun startRuleToReplaceComment(toBeReplaced: String, replacement: String) {
                            handler.startRuleToReplaceComment(toBeReplaced, replacement)
                        }

                        override fun startRuleToRemoveComment(comment: String) {
                            handler.startRuleToRemoveComment(comment)
                        }
                    }
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

fun List<String>.unhighlighted() = highlightItem(-1)

fun List<String>.highlightItem(index: Int) = buildAnnotatedString {
    forEachIndexed { i, text ->
        if (i == index) {
            addStyle(
                style = SpanStyle(
                    background = io.rippledown.decoration.BACKGROUND_COLOR
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