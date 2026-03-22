package io.rippledown.appbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
fun ApplicationBar(
    kbInfo: KBInfo?,
    handler: AppBarHandler,
) {
    TopAppBar(
        title = {
            Row {
                KbNameDisplay(kbInfo)
                Spacer(modifier = Modifier.width(16.dp))
                if (!handler.isRuleSessionInProgress) {
                    KBControl(kbInfo, handler)
                    Spacer(modifier = Modifier.width(8.dp))
                    EditCurrentKbControl(handler)
                }
            }
        },
        modifier = Modifier.semantics {
            contentDescription = APPLICATION_BAR_DESCRIPTION
        }.testTag(APPLICATION_BAR_ID)
    )
}
