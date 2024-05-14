package io.rippledown.rule

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.rule.REMOVE_CONDITION_ICON_PREFIX
import io.rippledown.constants.rule.SELECTED_CONDITIONS
import io.rippledown.constants.rule.SELECTED_CONDITION_PREFIX
import io.rippledown.model.condition.Condition

interface SelectedConditionsHandler {
    var onRemoveCondition: (condition: Condition) -> Unit
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun SelectedConditions(conditions: List<Condition>, handler: SelectedConditionsHandler) {
    val scrollState = rememberLazyListState()
    var cursorOnRow: Int by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .height(200.dp)
            .width(300.dp)
    ) {
        Text(
            text = "Selected conditions",
            style = TextStyle(
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic
            ),
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth()
        )
        LazyColumn(state = scrollState,
            modifier = Modifier
                .semantics { contentDescription = SELECTED_CONDITIONS }
        ) {
            itemsIndexed(conditions) { index, condition ->
                Row(
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .fillMaxWidth()
                        .background(
                            if (cursorOnRow == index) Color.LightGray else Color.Transparent
                        )
                        .onClick { handler.onRemoveCondition(condition) }
                ) {
                    Text(
                        text = condition.asText(),
                        modifier = Modifier
                            .onPointerEvent(Enter) { cursorOnRow = index }
                            .semantics {
                                contentDescription = "$SELECTED_CONDITION_PREFIX$index"
                            }

                    )
                    if (cursorOnRow == index)
                        Image(
                            painter = painterResource("line_24.png"),
                            contentDescription = "$REMOVE_CONDITION_ICON_PREFIX$index",
                        )
                    else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.End),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}
