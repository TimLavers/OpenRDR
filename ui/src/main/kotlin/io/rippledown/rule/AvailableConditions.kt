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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.rule.AVAILABLE_CONDITIONS
import io.rippledown.constants.rule.AVAILABLE_CONDITION_PREFIX
import io.rippledown.model.condition.Condition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

interface AvailableConditionsHandler {
    fun onAddCondition(condition: Condition)
    fun onEditThenAdd(condition: Condition)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AvailableConditions(conditions: List<Condition>, handler: AvailableConditionsHandler) {
    var cursorOnRow: Int by remember { mutableStateOf(-1) }
    val scrollState = rememberScrollState()
    val hoverOverScroll = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .height(200.dp)
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
                            .clickable {
                                handler.onAddCondition(condition)
                            }
                            .onPointerEvent(Enter) {
                                cursorOnRow = index
                            }
                            .pointerInput(Unit) {
                                coroutineScope {
                                    launch {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                                    println("Right click!!!!!!")
                                                    handler.onEditThenAdd(condition)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .background(
                                if (cursorOnRow == index) Color.LightGray else Color.Transparent
                            )
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
