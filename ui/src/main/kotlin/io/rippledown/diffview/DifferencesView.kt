@file:OptIn(ExperimentalComposeUiApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CHANGES
import io.rippledown.decoration.LightGreen
import io.rippledown.decoration.LightRed
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged

interface DifferencesViewHandler {
    fun onStartRule(selectedDiff: Diff)
}

val DIFF_VIEW = "DIFF_VIEW"
val DIFF_ROW_PREFIX = "DIFF_ROW_"
val ORIGINAL_PREFIX = "ORIGINAL_"
val CHANGED_PREFIX = "CHANGED_"
val ICON_PREFIX = "ICON_PREFIX_"

@Composable
fun DifferencesView(diffList: DiffList, handler: DifferencesViewHandler) {
    var cursorOnRow: Int by remember { mutableStateOf(diffList.indexOfFirstChange()) }

    Column(modifier = Modifier
        .width(400.dp)
        .semantics { contentDescription = INTERPRETATION_PANEL_CHANGES }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Original comment",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Current comment",
                fontWeight = FontWeight.Bold
            )
            Text("")
        }
        LazyColumn(modifier = Modifier.semantics { contentDescription = DIFF_VIEW }) {
            itemsIndexed(diffList.diffs) { index, diff ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp)
                        .height(40.dp)
                        .onPointerEvent(PointerEventType.Enter) { cursorOnRow = index }
                        .semantics { contentDescription = "$DIFF_ROW_PREFIX$index" }
                ) {
                    Text(
                        text = diff.left(),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .background(if (diff !is Unchanged) LightRed else Transparent)
                            .semantics { contentDescription = "$ORIGINAL_PREFIX$index" }
                    )
                    Text(
                        text = diff.right(),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .background(if (diff !is Unchanged) LightGreen else Transparent)
                            .semantics { contentDescription = "$CHANGED_PREFIX$index" }
                    )
                    if (diff !is Unchanged && cursorOnRow == index) {
                        ToolTipForIconAndLabel(
                            toolTipText = "Build a rule for this change",
                            isSelected = false,
                            icon = painterResource("wrench_24.png"),
                            onClick = {
                                handler.onStartRule(diff)
                            },
                            iconContentDescription = "$ICON_PREFIX$index"
                        )
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }
            }
        }
    }
}