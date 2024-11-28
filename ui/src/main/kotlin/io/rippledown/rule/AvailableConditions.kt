@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package io.rippledown.rule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.components.Scrollbar
import io.rippledown.constants.rule.AVAILABLE_CONDITIONS
import io.rippledown.constants.rule.AVAILABLE_CONDITION_PREFIX
import io.rippledown.model.condition.edit.SuggestedCondition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

interface AvailableConditionsHandler {
    fun onAddCondition(suggestedCondition: SuggestedCondition)
    fun onEditThenAdd(suggestedCondition: SuggestedCondition)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AvailableConditions(conditions: List<SuggestedCondition>, handler: AvailableConditionsHandler) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .height(200.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .verticalScroll(scrollState)
                .semantics { contentDescription = AVAILABLE_CONDITIONS }
        ) {
            conditions
                .sortedWith(compareBy { it.asText() })
                .forEachIndexed { index, suggestedCondition ->
                    val condition = suggestedCondition.initialSuggestion()
                    ConditionToolTip(condition) {
                        SuggestionChip(
                            onClick = {
                                if (suggestedCondition.isEditable()) {
                                    handler.onEditThenAdd(suggestedCondition)
                                } else {
                                    handler.onAddCondition(suggestedCondition)
                                }
                            },
                            modifier = Modifier.height(30.dp).width(300.dp),
                            label = {
                                Text(
                                    text = condition.display(),
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            coroutineScope {
                                                launch {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                                                println("Right click!!!!!!")
//                                                    if (suggestedCondition.isEditable()) {
//                                                        handler.onEditThenAdd(suggestedCondition.editableCondition()!!)
//                                                    }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .semantics { contentDescription = "$AVAILABLE_CONDITION_PREFIX$index" }
                                )
                            }
                        )
                    }
                }
        }
        Scrollbar(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
