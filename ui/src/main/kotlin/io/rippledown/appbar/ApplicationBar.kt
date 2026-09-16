package io.rippledown.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_NAME_ID
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.constants.main.APPLICATION_BAR_DESCRIPTION
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.model.KBInfo
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Text as Material3Text

private val AppBarBackground = Color(0xFF4F4A8C)
private val AppBarHeight = 44.dp

@Composable
fun ApplicationBar(
    kbInfo: KBInfo?,
    description: String? = null,
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
