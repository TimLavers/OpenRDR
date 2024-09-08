package io.rippledown.interpretation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.model.Conclusion

val BACKGROUND_COLOR = Color.LightGray

interface InterpretationViewHandler {
    fun onTextLayoutResult(layoutResult: TextLayoutResult)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InterpretationView(conclusions: List<Conclusion>, handler: InterpretationViewHandler? = null) {
    val commentList = conclusions.map { it.text }
    val unstyledText = commentList.unhighlighted()

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var styledText by remember { mutableStateOf(unstyledText) }
    var pointerEnter by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(
            text = styledText,
            modifier = Modifier.padding(10.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position
                            textLayoutResult?.let { layoutResult ->
                                if (pointerEnter) {
                                    val characterOffset = layoutResult.getOffsetForPosition(position)
                                    val commentIndex = commentList.commentIndexForOffset(characterOffset)
                                    styledText = commentList.highlightItem(commentIndex)
                                } else {
                                    styledText = unstyledText
                                }
                            }
                        }
                    }
                }
                .onPointerEvent(Enter) {
                    pointerEnter = true
                }.onPointerEvent(Exit) {
                    pointerEnter = false
                }
                .semantics {
                    contentDescription = INTERPRETATION_TEXT_FIELD
                },
            onTextLayout = { layoutResult ->
                textLayoutResult = layoutResult
                handler?.onTextLayoutResult(layoutResult)
            }
        )
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