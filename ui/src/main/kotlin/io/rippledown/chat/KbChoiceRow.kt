package io.rippledown.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.chat.NO_KNOWLEDGE_BASES_OF_YOUR_OWN
import io.rippledown.model.chat.KnowledgeBaseListing

@Composable
fun KbChoiceRow(
    listing: KnowledgeBaseListing,
    index: Int,
    enabled: Boolean = true,
    onChosen: (String) -> Unit = {}
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .semantics { contentDescription = "$BOT$index" }
            .padding(12.dp)
    ) {
        Column(Modifier.semantics { contentDescription = "$KB_CHOICE_LIST$index" }) {
            KnowledgeBaseHeading("Your knowledge bases", "Click a knowledge base to open it.")
            Spacer(Modifier.height(4.dp))
            if (listing.storedNames.isEmpty()) {
                Text(
                    NO_KNOWLEDGE_BASES_OF_YOUR_OWN, fontSize = 13.sp, color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            listing.storedNames.forEach { name ->
                KnowledgeBaseItem(name, name == listing.openName, enabled, onChosen)
            }
            Spacer(Modifier.height(12.dp))
            KnowledgeBaseHeading(
                "Demonstration knowledge bases",
                "Click a demonstration to open it. You will get your own copy and be asked to give it a name."
            )
            Spacer(Modifier.height(4.dp))
            listing.demonstrationNames.forEach { name ->
                KnowledgeBaseItem(name, false, enabled, onChosen)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KnowledgeBaseHeading(title: String, help: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray,
            modifier = Modifier.semantics { heading() })
        TooltipArea(tooltip = {
            Surface(color = Color(0xFF333333), contentColor = Color.White, shape = RoundedCornerShape(4.dp)) {
                Text(help, fontSize = 12.sp, modifier = Modifier.widthIn(max = 280.dp).padding(8.dp))
            }
        }) {
            Icon(
                Icons.Outlined.Info, contentDescription = "$title help", tint = Color.DarkGray,
                modifier = Modifier.padding(start = 6.dp).size(14.dp).semantics { stateDescription = help })
        }
    }
}

@Composable
private fun KnowledgeBaseItem(name: String, isOpen: Boolean, enabled: Boolean, onChosen: (String) -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val active = enabled && !isOpen
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (active && (hovered || focused)) Color(0xFFEEEAF7) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .pointerHoverIcon(if (active) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactions, indication = LocalIndication.current, enabled = active,
                role = Role.Button, onClick = { onChosen(name) })
            .semantics {
                contentDescription = "$KB_CHOICE_ITEM$name"
                if (isOpen) stateDescription = "Open"
            }
            .heightIn(min = 24.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false),
            color = if (active) Color(0xFF3949AB) else Color.DarkGray,
            textDecoration = if (active) TextDecoration.Underline else TextDecoration.None
        )
        if (isOpen) {
            Text(
                "(current)", fontSize = 12.sp, color = Color.DarkGray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
