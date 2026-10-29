package io.rippledown.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.APPLICATION_BAR_DESCRIPTION
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.constants.main.RECONNECTING_DESCRIPTION
import io.rippledown.model.KBInfo
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Text as Material3Text

private val AppBarBackground = Color(0xFF4F4A8C)
private val AppBarHeight = 44.dp

@Composable
fun ApplicationBar(
    kbInfo: KBInfo?,
    description: String? = null,
    othersPresent: List<String> = emptyList(),
    connected: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(AppBarHeight)
            .background(AppBarBackground)
            .padding(horizontal = 8.dp)
            .semantics {
                contentDescription = APPLICATION_BAR_DESCRIPTION
            }
            .testTag(APPLICATION_BAR_ID)
    ) {
        ReadOnlyKbName(kbInfo)
        if (kbInfo != null) {
            key(kbInfo.id) { KnowledgeBaseDescription(description) }
        }
        if (!connected) {
            Text(
                text = "Reconnecting…",
                style = MaterialTheme.typography.body2,
                color = colors.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp)
                    .semantics { contentDescription = RECONNECTING_DESCRIPTION }
            )
        }
        Spacer(Modifier.weight(1f))
        if (kbInfo != null && othersPresent.isNotEmpty()) {
            key(kbInfo.id) { OthersPresent(kbInfo.name, othersPresent) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OthersPresent(kbName: String, othersPresent: List<String>) {
    val text = "Others with $kbName open:\n" + othersPresent.joinToString("\n") { "• $it" }
    val tooltip = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    var openedByClick by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused) tooltip.show() else tooltip.dismiss()
    }
    LaunchedEffect(tooltip.isVisible) {
        if (!tooltip.isVisible) openedByClick = false
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        state = tooltip,
        focusable = false,
        tooltip = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Material3Theme.colorScheme.inverseSurface,
                contentColor = Material3Theme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp
            ) {
                Material3Text(
                    text,
                    fontSize = 12.sp,
                    modifier = Modifier.widthIn(max = 320.dp).heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()).padding(12.dp)
                        .semantics { contentDescription = PRESENCE_DETAILS_DESCRIPTION }
                )
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(4.dp))
                .semantics(mergeDescendants = true) {
                    contentDescription = PRESENCE_DESCRIPTION
                    stateDescription = text
                }
                .onFocusChanged { focused = it.isFocused }
                .clickable(role = Role.Button) {
                    // Hover can reveal the names before the first click arrives.
                    // Only a second click should toggle that list closed.
                    val dismiss = openedByClick && tooltip.isVisible
                    openedByClick = !dismiss
                    scope.launch { if (dismiss) tooltip.dismiss() else tooltip.show() }
                }
                .padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Outlined.People, contentDescription = null,
                tint = colors.onPrimary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Text(othersPresent.size.toString(), style = MaterialTheme.typography.body2,
                color = colors.onPrimary.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnowledgeBaseDescription(description: String?) {
    val text = when {
        description == null -> "Loading description..."
        description.isBlank() -> "No description set. You can add one through chat."
        else -> description
    }
    val tooltip = rememberTooltipState(isPersistent = true)
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused) tooltip.show() else tooltip.dismiss()
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        state = tooltip,
        focusable = false,
        tooltip = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Material3Theme.colorScheme.inverseSurface,
                contentColor = Material3Theme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp
            ) {
                Material3Text(text, fontSize = 12.sp, modifier = Modifier.widthIn(max = 400.dp).padding(8.dp))
            }
        }
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = "Knowledge base description",
            tint = colors.onPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp).padding(2.dp)
                .semantics { stateDescription = text }
                .onFocusChanged { focused = it.isFocused }
                .focusable()
        )
    }
}

@Composable
private fun ReadOnlyKbName(kbInfo: KBInfo?) {
    Text(
        text = kbInfo?.name ?: NO_KB_SELECTED,
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.SemiBold,
        color = colors.onPrimary,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .testTag(KB_NAME_ID)
            .semantics(mergeDescendants = true) {
                contentDescription = KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
            }
    )
}
