@file:OptIn(ExperimentalFoundationApi::class)

package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    var onToggleChat: () -> Unit
}

const val CHAT_ICON_TOGGLE = "CHAT_ICON"

@Composable
@Preview
fun ApplicationBar(
    kbInfo: KBInfo?,
    isChatVisible: Boolean = true,
    isChatEnabled: Boolean = true,
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
        actions = {
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isChatVisible) "Hide Chat Panel" else "Show Chat Panel",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            ) {
                IconButton(
                    onClick = {
                        if (isChatEnabled) handler.onToggleChat()
                    },
                    enabled = isChatEnabled
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Chat,
                        contentDescription = CHAT_ICON_TOGGLE,
                        tint = if (isChatVisible) Color.White else Color.Gray,
                    )
                }
            }
        },
        modifier = Modifier.semantics {
            contentDescription = APPLICATION_BAR_DESCRIPTION
        }.testTag(APPLICATION_BAR_ID)
    )
}