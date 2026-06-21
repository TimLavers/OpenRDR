package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE
import io.rippledown.constants.interpretation.UNRESOLVED_VARIABLE_TOOLTIP
import io.rippledown.model.Conclusion
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

interface ReadonlyInterpretationViewHandler {
    fun onTextLayoutResult(layoutResult: TextLayoutResult) {}
}

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun ReadonlyInterpretationView(
    interpretation: ViewableInterpretation,
    diff: Diff? = null,
    ruleConditions: List<String> = emptyList(),
    contentDescription: String = INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE,
    modifier: Modifier,
    handler: ReadonlyInterpretationViewHandler
) {
    val conclusionList = interpretation.conclusions().toList()
    var comments by remember {
        mutableStateOf(interpretation.renderedComments.map { it.text })
    }
    var unresolvedRanges by remember {
        mutableStateOf(interpretation.renderedComments.map { it.unresolvedRanges })
    }
    var unstyledText by remember { mutableStateOf(comments.unhighlighted(diff, unresolvedRanges)) }
    var styledText by remember { mutableStateOf(unstyledText) }
    var commentIndex by remember { mutableStateOf(-1) }
    var isOverDiffText by remember { mutableStateOf(false) }
    var isOverUnresolved by remember { mutableStateOf(false) }

    LaunchedEffect(interpretation, diff) {
        comments = interpretation.renderedComments.map { it.text }
        unresolvedRanges = interpretation.renderedComments.map { it.unresolvedRanges }
        unstyledText = comments.unhighlighted(diff, unresolvedRanges)
        styledText = unstyledText
    }

    TooltipArea(
        modifier = modifier.fillMaxWidth(),
        tooltip = {
            if (isOverUnresolved) {
                UnresolvedVariableTooltip()
            } else if (isOverDiffText && ruleConditions.isNotEmpty()) {
                ConditionTooltip(ruleConditions)
            } else {
                ToolTipForNonEmptyInterpretation(commentIndex, conclusionList, interpretation)
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
                        isOverUnresolved = unstyledText.spanStyles.any { span ->
                            characterOffset in span.start until span.end &&
                                    span.item.background == UNRESOLVED_COLOR
                        }
                        isOverDiffText = unstyledText.spanStyles.any { span ->
                            characterOffset in span.start until span.end &&
                                    (span.item.background == DIFF_ADDITION_COLOR || span.item.background == DIFF_REMOVAL_COLOR)
                        }
                        if (!isOverDiffText) {
                            commentIndex = comments.commentIndexForOffset(characterOffset)
                            styledText = comments.highlightItem(commentIndex, diff)
                        }
                    }

                    override fun onPointerExit() {
                        commentIndex = -1
                        isOverDiffText = false
                        isOverUnresolved = false
                    }
                }
            )
        }
    )
}

@Composable
fun UnresolvedVariableTooltip() {
    androidx.compose.material3.Surface(
        color = UNRESOLVED_COLOR,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        shadowElevation = 4.dp
    ) {
        androidx.compose.material.Text(
            text = UNRESOLVED_VARIABLE_TOOLTIP,
            modifier = Modifier
                .padding(8.dp)
                .semantics { contentDescription = UNRESOLVED_VARIABLE_TOOLTIP }
        )
    }
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