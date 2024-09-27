package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.model.CaseId
import java.awt.event.KeyEvent.VK_DOWN
import java.awt.event.KeyEvent.VK_UP

interface CaseSelectorHandler {
    var selectCase: (id: Long) -> Unit
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun CaseSelector(caseIds: List<CaseId>, handler: CaseSelectorHandler) {
    val scrollState = rememberScrollState()
    val hoverOverScroll = remember { mutableStateOf(false) }
    var selectedCaseIndex by remember { mutableStateOf(0) }
    val focusRequestors = mutableListOf<FocusRequester>()

    fun indexSelected(index: Int) {
        selectedCaseIndex = if (index < 1) { // Arrow up at top.
            0
        } else if (index >= caseIds.size) { // Arrow down at bottom.
            caseIds.size - 1
        } else {
            index
        }
        val caseId = caseIds[selectedCaseIndex]
        handler.selectCase(caseId.id!!)
        focusRequestors[selectedCaseIndex].requestFocus()
    }
    Box(
        modifier = Modifier
            .height(800.dp)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .size(100.dp, 800.dp)
                .padding(start = 5.dp)
                .verticalScroll(scrollState)
                .semantics {
                    contentDescription = CASELIST_ID
                }
        ) {
            caseIds.forEachIndexed { index, caseId ->
                    focusRequestors.add(FocusRequester())
                    Text(
                        text = caseId.name,
                        modifier = Modifier
                            .focusRequester(focusRequestors[index])
                            .clickable {
                                indexSelected(index)
                            }
                            .background(if (index == selectedCaseIndex) Color.LightGray else Color.White)
                            .onKeyEvent { keyEvent ->
                                if (downArrowKeyWasPressed(keyEvent)) {
                                    indexSelected(index + 1)
                                } else if (upArrowKeyWasPressed(keyEvent)) {
                                    indexSelected(index - 1)
                                }
                                false
                            }
                            .semantics { contentDescription = "$CASE_NAME_PREFIX${caseId.name}" }
                    )
                }
        }
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .onPointerEvent(PointerEventType.Enter) {
                    hoverOverScroll.value = true
                }
                .onPointerEvent(PointerEventType.Exit) {
                    hoverOverScroll.value = false
                }
                .requiredWidth(if (hoverOverScroll.value) 10.dp else 5.dp),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

private fun downArrowKeyWasPressed(keyEvent: KeyEvent) =
    ((keyEvent.type == KeyDown) && (keyEvent.key == Key.DirectionDown))//detect event generated from ComposeTestRule
            || keyEvent == KeyEvent(VK_DOWN) //detect event generated from keyboard

private fun upArrowKeyWasPressed(keyEvent: KeyEvent) =
    ((keyEvent.type == KeyDown) && (keyEvent.key == Key.DirectionUp)) //detect event generated from ComposeTestRule
            || keyEvent == KeyEvent(VK_UP) //detect event generated from keyboard

