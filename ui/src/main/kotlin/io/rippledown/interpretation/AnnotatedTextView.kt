@file:OptIn(
    ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class,
    ExperimentalComposeUiApi::class
)

package io.rippledown.interpretation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD

interface AnnotatedTextViewHandler {
    fun onTextLayoutResult(layoutResult: TextLayoutResult)
    fun onPointerEnter(characterOffset: Int)
    fun onPointerExit()
}

@Composable
fun AnnotatedTextView(
    text: AnnotatedString,
    description: String = INTERPRETATION_TEXT_FIELD,
    handler: AnnotatedTextViewHandler
) {
    var pointerEnter by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        modifier = Modifier.padding(10.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        textLayoutResult.let { layoutResult ->
                            if (pointerEnter) {
                                val characterOffset = layoutResult!!.getOffsetForPosition(position)
                                handler.onPointerEnter(characterOffset)
                            } else {
                                handler.onPointerExit()
                            }
                        }
                    }
                }
            }
            .onPointerEvent(PointerEventType.Enter) {
                if (!pointerEnter) {
                    pointerEnter = true
                }
            }.onPointerEvent(PointerEventType.Exit) {
                if (pointerEnter) {
                    pointerEnter = false
                }
            }
            .semantics {
                contentDescription = description
            },
        onTextLayout = { layoutResult ->
            textLayoutResult = layoutResult
            handler.onTextLayoutResult(layoutResult)
        }
    )

}