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
import androidx.compose.ui.semantics.selected
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
    handler: CaseSelectorHandler,
    favouriteCaseIds: List<CaseId> = emptyList(),
    selectedCaseId: Long? = null
) {
    val allCaseIds = caseIds + cornerstoneCaseIds + favouriteCaseIds
    val selectedCaseIndex = allCaseIds.indexOfFirst { it.id == selectedCaseId }
    val focusRequestors = remember(allCaseIds) { List(allCaseIds.size) { FocusRequester() } }
    var processedExpanded by remember { mutableStateOf(true) }
    var cornerstoneExpanded by remember { mutableStateOf(true) }
    var favouritesExpanded by remember { mutableStateOf(true) }

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
        val isComposed = when {
            index < caseIds.size -> processedExpanded
            index < caseIds.size + cornerstoneCaseIds.size -> cornerstoneCaseIds.isNotEmpty() && cornerstoneExpanded
            else -> favouriteCaseIds.isNotEmpty() && favouritesExpanded
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
        if (allCaseIds.isEmpty()) return
        val clampedIndex = index.coerceIn(0, allCaseIds.size - 1)
        allCaseIds[clampedIndex].id?.let { handler.selectCase(it) }
        requestFocusOnCase(clampedIndex)
    }

    // The sections are stacked, each taking only the height its cases need, so
    // that a section header always sits directly below the section above it.
    // (Giving each section a weight instead would share the panel's height
    // between the expanded sections, so collapsing one would push the headers
    // below it down the panel.) One scroll state covers the lot, so a long case
    // list scrolls the whole panel rather than each section separately.
    val scrollState = rememberScrollState()
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
                .verticalScroll(scrollState)
        ) {
            CollapsibleSectionHeader(
                title = "Processed (${caseIds.size})",
                expanded = processedExpanded,
                onToggle = { processedExpanded = !processedExpanded },
                semanticId = PROCESSED_SECTION_HEADER_ID
            )
            if (processedExpanded) {
                CaseSectionList(
                    caseIds = caseIds,
                    firstIndex = 0,
                    semanticId = PROCESSED_SECTION_ID,
                    selectedCaseIndex = selectedCaseIndex,
                    focusRequestors = focusRequestors,
                    onSelect = ::indexSelected
                )
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
                CaseSectionList(
                    caseIds = cornerstoneCaseIds,
                    firstIndex = caseIds.size,
                    semanticId = CORNERSTONE_SECTION_ID,
                    selectedCaseIndex = selectedCaseIndex,
                    focusRequestors = focusRequestors,
                    onSelect = ::indexSelected
                )
            }
            if (favouriteCaseIds.isNotEmpty()) {
                CollapsibleSectionHeader(
                    title = "Favourites (${favouriteCaseIds.size})",
                    expanded = favouritesExpanded,
                    onToggle = { favouritesExpanded = !favouritesExpanded },
                    semanticId = FAVOURITES_SECTION_HEADER_ID
                )
            }
            if (favouriteCaseIds.isNotEmpty() && favouritesExpanded) {
                CaseSectionList(
                    caseIds = favouriteCaseIds,
                    firstIndex = caseIds.size + cornerstoneCaseIds.size,
                    semanticId = FAVOURITES_SECTION_ID,
                    selectedCaseIndex = selectedCaseIndex,
                    focusRequestors = focusRequestors,
                    onSelect = ::indexSelected
                )
            }
        }
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(8.dp),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

/**
 * The cases of one section, indented under its header.
 *
 * @param firstIndex the index of this section's first case in the combined list
 *   of all cases, which is what selection and arrow-key navigation work in.
 */
@Composable
private fun CaseSectionList(
    caseIds: List<CaseId>,
    firstIndex: Int,
    semanticId: String,
    selectedCaseIndex: Int,
    focusRequestors: List<FocusRequester>,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .semantics {
                contentDescription = semanticId
            }
            .padding(start = 14.dp, end = 20.dp)
            .fillMaxWidth()
    ) {
        caseIds.forEachIndexed { index, caseId ->
            val globalIndex = firstIndex + index
            CaseNameItem(
                caseId = caseId,
                isSelected = globalIndex == selectedCaseIndex,
                focusRequester = focusRequestors[globalIndex],
                onClick = { onSelect(globalIndex) },
                onDownArrow = { onSelect(globalIndex + 1) },
                onUpArrow = { onSelect(globalIndex - 1) }
            )
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
            .semantics {
                contentDescription = "$CASE_NAME_PREFIX${caseId.name}"
                selected = isSelected
            }
    )
}

private fun downArrowKeyWasPressed(keyEvent: KeyEvent) =
    (keyEvent.type == KeyDown) && (keyEvent.key == Key.DirectionDown)

private fun upArrowKeyWasPressed(keyEvent: KeyEvent) =
    (keyEvent.type == KeyDown) && (keyEvent.key == Key.DirectionUp)

