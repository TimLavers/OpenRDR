package io.rippledown.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_NAME_ID
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.constants.main.APPLICATION_BAR_DESCRIPTION
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.model.KBInfo

interface AppBarHandler : KBControlHandler, KbEditControlHandler {
    var isRuleSessionInProgress: Boolean
}

private val AppBarBackground = Color(0xFF4F4A8C)
private val AppBarHeight = 44.dp

@Composable
fun ApplicationBar(
    kbInfo: KBInfo?,
    handler: AppBarHandler,
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
        if (!handler.isRuleSessionInProgress) {
            KbAnchorMenu(kbInfo, handler)
        } else {
            // During rule building the user must not be able to switch or edit
            // the knowledge base, but we still show the current KB name so the
            // context is not lost. No dropdown icon is shown.
            ReadOnlyKbName(kbInfo)
        }
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
