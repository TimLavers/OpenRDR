package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.rippledown.constants.main.APPLICATION_BAR_DESCRIPTION
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.model.KBInfo

interface AppBarHandler : KBControlHandler, KbEditControlHandler {
    var isRuleSessionInProgress: Boolean
}

@Composable
@Preview
fun ApplicationBar(kbInfo: KBInfo?, handler: AppBarHandler) {

    TopAppBar(modifier = Modifier.semantics {
        contentDescription = APPLICATION_BAR_DESCRIPTION
    }.testTag(APPLICATION_BAR_ID)) {
        Row {
//            ApplicationNameDisplay()
            KbNameDisplay(kbInfo)
            // Don't show menus if a rule session is in progress
            if (!handler.isRuleSessionInProgress) {
                KBControl(kbInfo, handler)
                EditCurrentKbControl(handler)
            }
        }
    }
}