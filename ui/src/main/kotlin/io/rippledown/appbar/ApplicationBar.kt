package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import io.rippledown.constants.main.APPLICATION_BAR_DESCRIPTION
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.constants.main.MAIN_HEADING_ID
import io.rippledown.model.KBInfo

interface AppBarHandler : KBControlHandler {
    var isRuleSessionInProgress: Boolean
}

@Composable
@Preview
fun ApplicationBar(kbInfo: KBInfo?, handler: AppBarHandler) {

    TopAppBar(modifier = Modifier.semantics {
        contentDescription = APPLICATION_BAR_DESCRIPTION
    }.testTag(APPLICATION_BAR_ID)) {

        Row {
            Text(
                style = MaterialTheme.typography.h6,
                text = MAIN_HEADING,
                color = colors.onPrimary,
                fontWeight = Bold,
                modifier = Modifier
                    .padding(10.dp)
                    .testTag(MAIN_HEADING_ID)
                    .semantics {
                        contentDescription = MAIN_HEADING
                    }
            )

            //Simplify the ApplicationBar by removing the KBControl if a rule session is in progress
            if (!handler.isRuleSessionInProgress) {
                KBControl(kbInfo, handler)
            }
        }
    }
}