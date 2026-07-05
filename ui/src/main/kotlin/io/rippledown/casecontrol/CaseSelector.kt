package io.rippledown.casecontrol

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
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
    var selectedCaseIndex by remember { mutableStateOf(0) }
    val focusRequestors = remember(allCaseIds) { List(allCaseIds.size) { FocusRequester() } }
    var processedExpanded by remember { mutableStateOf(true) }
    var cornerstoneExpanded by remember { mutableStateOf(true) }

    // A FocusRequester is only attached once its CaseNameItem has been
    // composed, which only happens when the item's section is expanded (the
    // scrolling Column composes all of its children regardless of scroll
    // offset). Requesting focus on an unattached requester logs a
    // "FocusRequester is not initialized" warning and forces the accessibility
    // bridge to re-sync focus state — needless churn that aggravates the
    // Compose Desktop a11y sync race. Only request focus when the target item
    // is actually composed, and guard defensively against a not-yet-attached
    // requester on the current frame.
    fun requestFocusOnCase(index: Int) {
        if (index !in focusRequestors.indices) return
        val isComposed = if (index < caseIds.size) {
            processedExpanded
        } else {
            cornerstoneCaseIds.isNotEmpty() && cornerstoneExpanded
        }
        if (!isComposed) return
        try {
            focusRequestors[index].requestFocus()
        } catch (_: IllegalStateException) {
            // Requester not attached yet on this frame; safe to ignore.
        }
    }

    // Implement the callback to request focus on the selected case
    handler.requestFocusOnSelectedCase = {
        requestFocusOnCase(selectedCaseIndex)
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
        requestFocusOnCase(selectedCaseIndex)
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
                Box(modifier = Modifier.weight(1f)) {
                    val processedScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .semantics {
                                contentDescription = PROCESSED_SECTION_ID
                            }
                            .padding(start = 14.dp, end = 20.dp)
                            .fillMaxSize()
                            .verticalScroll(processedScrollState)
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
                    // Show scrollbar 
                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(8.dp),
                        adapter = rememberScrollbarAdapter(processedScrollState)
                    )
                }
            }
            if (cornerstoneCaseIds.isNotEmpty()) {
                CollapsibleSectionHeader(
                    title = "Cornerstones (${cornerstoneCaseIds.size})",
                    expanded = cornerstoneExpanded,
                    onToggle = { cornerstoneExpanded = !cornerstoneExpanded },
                    semanticId = CORNERSTONE_SECTION_HEADER_ID
                )
            }
            if (cornerstoneCaseIds.isNotEmpty() && cornerstoneExpanded) {
                Box(modifier = Modifier.weight(1f)) {
                    val cornerstoneScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .semantics {
                                contentDescription = CORNERSTONE_SECTION_ID
                            }
                            .padding(start = 14.dp, end = 20.dp)
                            .fillMaxSize()
                            .verticalScroll(cornerstoneScrollState)
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
                    // Show scrollbar 
                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(8.dp),
                        adapter = rememberScrollbarAdapter(cornerstoneScrollState)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    semanticId: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onToggle() }
            .padding(vertical = 4.dp)
            .semantics { contentDescription = semanticId }
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(18.dp)
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

