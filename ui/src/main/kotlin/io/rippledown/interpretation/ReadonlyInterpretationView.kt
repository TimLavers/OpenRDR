package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE
import io.rippledown.model.interpretationview.ViewableInterpretation

interface ReadonlyInterpretationViewHandler {
    fun onTextLayoutResult(layoutResult: TextLayoutResult) {}
}

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun ReadonlyInterpretationView(
    interpretation: ViewableInterpretation,
    contentDescription: String = INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE,
    modifier: Modifier,
    handler: ReadonlyInterpretationViewHandler
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

    TooltipArea(
        modifier = modifier.fillMaxWidth(),
        tooltip = {
            val showToolTip = commentIndex != -1
            if (showToolTip) {
                println("------------Showing tooltip for comment index $commentIndex")
                ConditionTooltip(interpretation.conditionsForConclusion(conclusionList[commentIndex]))
            }
        },
        content = {
            AnnotatedTextView(
                text = if (commentIndex == -1) unstyledText else styledText,
                description = contentDescription,
                handler = object : AnnotatedTextViewHandler {
                    override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                        handler.onTextLayoutResult(layoutResult)
                    }

                    override fun onPointerEnter(characterOffset: Int) {
                        commentIndex = comments.commentIndexForOffset(characterOffset)
                        styledText = comments.highlightItem(commentIndex)
                        println("------------Pointer entered at offset $characterOffset, comment index is now $commentIndex")
                    }

                    override fun onPointerExit() {
                        println("------------Pointer exited, resetting comment index")
                        commentIndex = -1
                    }
                }
            )
        }
    )
}