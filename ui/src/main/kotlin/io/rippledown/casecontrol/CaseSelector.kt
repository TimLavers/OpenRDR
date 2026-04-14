package io.rippledown.casecontrol

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.caseview.*
import io.rippledown.model.CaseId

interface CaseSelectorHandler {
    var selectCase: (id: Long) -> Unit
    var requestFocusOnSelectedCase: () -> Unit
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CaseSelector(
    caseIds: List<CaseId>,
    cornerstoneCaseIds: List<CaseId> = emptyList(),
    handler: CaseSelectorHandler
) {
    val allCaseIds = caseIds + cornerstoneCaseIds
    val scrollState = rememberScrollState()
    val hoverOverScroll = remember { mutableStateOf(false) }
    var selectedCaseIndex by remember { mutableStateOf(0) }
    val focusRequestors = remember(allCaseIds) { List(allCaseIds.size) { FocusRequester() } }
    var processedExpanded by remember { mutableStateOf(true) }
    var cornerstoneExpanded by remember { mutableStateOf(true) }

    // Implement the callback to request focus on the selected case
    handler.requestFocusOnSelectedCase = {
        if (selectedCaseIndex < focusRequestors.size) {
            focusRequestors[selectedCaseIndex].requestFocus()
        }
    }

    fun indexSelected(index: Int) {
        selectedCaseIndex = if (index < 1) { // Arrow up at top.
            0
        } else if (index >= allCaseIds.size) { // Arrow down at bottom.
            allCaseIds.size - 1
        } else {
            index
        }
        val caseId = allCaseIds[selectedCaseIndex]
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
                .size(150.dp, 800.dp)
                .padding(start = 5.dp)
                .verticalScroll(scrollState)
                .semantics {
                    contentDescription = CASELIST_ID
                }
        ) {
            CollapsibleSectionHeader(
                title = "Processed (${caseIds.size})",
                expanded = processedExpanded,
                onToggle = { processedExpanded = !processedExpanded },
                semanticId = PROCESSED_SECTION_HEADER_ID
            )
            if (processedExpanded) {
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = PROCESSED_SECTION_ID
                    }
                        .padding(start = 14.dp)
                ) {
                    caseIds.forEachIndexed { index, caseId ->
                        CaseNameItem(
                            caseId = caseId,
                            isSelected = index == selectedCaseIndex,
                            focusRequester = focusRequestors[index],
                            onClick = { indexSelected(index) },
                            onDownArrow = { indexSelected(index + 1) },
                            onUpArrow = { indexSelected(index - 1) }
                        )
                    }
                }
            }
            CollapsibleSectionHeader(
                title = "Cornerstones (${cornerstoneCaseIds.size})",
                expanded = cornerstoneExpanded,
                onToggle = { cornerstoneExpanded = !cornerstoneExpanded },
                semanticId = CORNERSTONE_SECTION_HEADER_ID
            )
            if (cornerstoneExpanded) {
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = CORNERSTONE_SECTION_ID
                    }
                        .padding(start = 14.dp)
                ) {
                    cornerstoneCaseIds.forEachIndexed { csIndex, caseId ->
                        val globalIndex = caseIds.size + csIndex
                        CaseNameItem(
                            caseId = caseId,
                            isSelected = globalIndex == selectedCaseIndex,
                            focusRequester = focusRequestors[globalIndex],
                            onClick = { indexSelected(globalIndex) },
                            onDownArrow = { indexSelected(globalIndex + 1) },
                            onUpArrow = { indexSelected(globalIndex - 1) }
                        )
                    }
                }
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

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    semanticId: String
) {
    val arrow = if (expanded) "▾" else "▸"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onToggle() }
            .padding(vertical = 4.dp)
            .semantics { contentDescription = semanticId }
    ) {
        Text(
            text = arrow,
            style = TextStyle(
                color = Color.DarkGray,
                fontSize = 20.sp
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = TextStyle(
                color = Color.DarkGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CaseNameItem(
    caseId: CaseId,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onDownArrow: () -> Unit,
    onUpArrow: () -> Unit
) {
    Text(
        text = caseId.name,
        fontSize = 12.sp,
        modifier = Modifier
            .focusRequester(focusRequester)
            .clickable { onClick() }
            .background(if (isSelected) Color.LightGray else Color.White)
            .onKeyEvent { keyEvent ->
                if (downArrowKeyWasPressed(keyEvent)) {
                    onDownArrow()
                    true
                } else if (upArrowKeyWasPressed(keyEvent)) {
                    onUpArrow()
                    true
                } else {
                    false
                }
            }
            .semantics { contentDescription = "$CASE_NAME_PREFIX${caseId.name}" }
    )
}

private fun downArrowKeyWasPressed(keyEvent: KeyEvent) =
    (keyEvent.type == KeyDown) && (keyEvent.key == Key.DirectionDown)

private fun upArrowKeyWasPressed(keyEvent: KeyEvent) =
    (keyEvent.type == KeyDown) && (keyEvent.key == Key.DirectionUp)

