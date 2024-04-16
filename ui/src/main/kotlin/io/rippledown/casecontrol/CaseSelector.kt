package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.model.CaseId
import java.awt.event.KeyEvent.VK_DOWN
import java.awt.event.KeyEvent.VK_UP
import kotlin.math.max
import kotlin.math.min

interface CaseSelectorHandler {
    var selectCase: (id: Long) -> Unit
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun CaseSelector(caseIds: List<CaseId>, handler: CaseSelectorHandler) {
    val count = caseIds.size
    val scrollState = rememberScrollState()
    var selectedCaseIndex by remember { mutableStateOf(0) }
    val focusRequestors = mutableListOf<FocusRequester>()
    Box(
        modifier = Modifier
            .height(800.dp)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .size(150.dp, 800.dp)
                .testTag(CASELIST_ID)
                .semantics {
                    contentDescription = CASELIST_ID
                }
                .verticalScroll(scrollState)
        ) {
            caseIds.forEachIndexed { index, caseId ->
                focusRequestors.add(FocusRequester())
                Text(
                    text = caseId.name,
                    modifier = Modifier
                        .focusRequester(focusRequestors[index])
                        .clickable {
                            selectedCaseIndex = index
                            handler.selectCase(caseId.id!!)
                        }
                        .background(if (index == selectedCaseIndex) Color.LightGray else Color.White)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                val nextIndex = if (downArrowKeyWasPressed(keyEvent)) {
                                    min(count - 1, selectedCaseIndex + 1)
                                } else {
                                    if (upArrowKeyWasPressed(keyEvent)) {
                                        max(0, selectedCaseIndex - 1)
                                    } else {
                                        selectedCaseIndex
                                    }
                                }
                                focusRequestors[nextIndex].requestFocus()
                                selectedCaseIndex = nextIndex
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
                .requiredWidth(5.dp),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

private fun downArrowKeyWasPressed(keyEvent: KeyEvent) =
    keyEvent == KeyEvent(VK_DOWN) //detect event generated from keyboard
            || keyEvent.key == Key.DirectionDown //detect event generated from ComposeTestRule

private fun upArrowKeyWasPressed(keyEvent: KeyEvent) =
    keyEvent == KeyEvent(VK_UP) //detect event generated from keyboard
            || keyEvent.key == Key.DirectionUp //detect event generated from ComposeTestRule

