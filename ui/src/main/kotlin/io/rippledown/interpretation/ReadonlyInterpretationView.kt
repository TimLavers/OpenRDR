package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE
import io.rippledown.model.Conclusion
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
            ToolTipForNonEmptyInterpretation(commentIndex, conclusionList, interpretation)
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

@Composable
fun ToolTipForNonEmptyInterpretation(
    commentIndex: Int,
    conclusionList: List<Conclusion>,
    interpretation: ViewableInterpretation
) {
    val showToolTip = commentIndex != -1 && commentIndex < conclusionList.size
    if (showToolTip) {
        ConditionTooltip(interpretation.conditionsForConclusion(conclusionList[commentIndex]))
    }
}