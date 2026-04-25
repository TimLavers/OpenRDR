package io.rippledown.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
        }
    }
}
