package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextLayoutResult
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE
import io.rippledown.model.interpretationview.ViewableInterpretation

interface ReadonlyInterpretationViewHandler {
    fun onTextLayoutResult(layoutResult: TextLayoutResult) {}
}

@ExperimentalFoundationApi
@Composable
fun ReadonlyInterpretationView(
    interpretation: ViewableInterpretation,
    contentDescription: String = INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE,
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
        tooltip = {
            val showToolTip = commentIndex != -1
            if (showToolTip) {
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
                    }

                    override fun onPointerExit() {
                        commentIndex = -1
                    }
                }
            )
        }
    )
}