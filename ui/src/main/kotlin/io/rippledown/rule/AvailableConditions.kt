@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package io.rippledown.rule

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.rule.AVAILABLE_CONDITIONS
import io.rippledown.constants.rule.AVAILABLE_CONDITION_PREFIX
import io.rippledown.model.condition.Condition

interface AvailableConditionsHandler {
    var onAddCondition: (condition: Condition) -> Unit
}

@Composable
fun AvailableConditions(conditions: List<Condition>, handler: AvailableConditionsHandler) {
    var cursorOnRow: Int by remember { mutableStateOf(-1) }
    val scrollState = rememberScrollState()
    val hoverOverScroll = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .semantics { contentDescription = AVAILABLE_CONDITIONS }
        ) {
            conditions
                .sortedWith(compareBy { it.asText() })
                .forEachIndexed { index, condition ->
                Text(
                    text = condition.asText(),
                    modifier = Modifier
                        .onPointerEvent(Enter) {
                            cursorOnRow = index
                        }
                        .background(
                            if (cursorOnRow == index) Color.LightGray else Color.Transparent
                        )
                        .clickable {
                            println("Adding condition: $condition")
                            handler.onAddCondition(condition)
                        }
                        .padding(start = 10.dp)
                        .semantics { contentDescription = "$AVAILABLE_CONDITION_PREFIX$index" }

                )
            }
        }
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .onPointerEvent(Enter) {
                    hoverOverScroll.value = true
                }
                .onPointerEvent(Exit) {
                    hoverOverScroll.value = false
                }
                .requiredWidth(if (hoverOverScroll.value) 10.dp else 5.dp),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}
