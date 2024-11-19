package io.rippledown.rule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.components.Scrollbar
import io.rippledown.constants.rule.SELECTED_CONDITIONS
import io.rippledown.constants.rule.SELECTED_CONDITION_PREFIX
import io.rippledown.model.condition.Condition

interface SelectedConditionsHandler {
    var onRemoveCondition: (condition: Condition) -> Unit
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun SelectedConditions(conditions: List<Condition>, handler: SelectedConditionsHandler) {
    val scrollState = rememberScrollState()
    var cursorOnRow: Int by remember { mutableStateOf(-1) }
    Text(
        text = "Selected conditions",
        style = TextStyle(
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic
        ),
        modifier = Modifier
            .fillMaxWidth()
    )
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
                .semantics { contentDescription = SELECTED_CONDITIONS }
        ) {
            conditions
                .sortedWith(compareBy { it.asText() })
                .forEachIndexed { index, condition ->
                    ConditionToolTip(condition) {
                        InputChip(
                            selected = false,
                            trailingIcon = {
                                if (cursorOnRow == index) Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Icon to remove the condition",
                                    modifier = Modifier.size(15.dp)
                                ) else Spacer(modifier = Modifier.width(15.dp))
                            },
                            onClick = {
                                handler.onRemoveCondition(condition)
                            },
                            label = {
                                Text(
                                    text = condition.display(),
                                )
                            },
                            modifier = Modifier
                                .height(30.dp).width(300.dp)
                                .onPointerEvent(Enter) { cursorOnRow = index }
                                .semantics {
                                    contentDescription = "$SELECTED_CONDITION_PREFIX$index"
                                }

                        )
                    }
                }
        }
        Scrollbar(scrollState = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}


