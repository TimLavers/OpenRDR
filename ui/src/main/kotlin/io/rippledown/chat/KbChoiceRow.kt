package io.rippledown.chat

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
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
            Text(
                "Your knowledge bases", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
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
            Spacer(Modifier.height(16.dp))
            Text(
                "Demonstration knowledge bases", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            Text(
                "Opening a demonstration creates your own named copy", fontSize = 12.sp, color = Color.DarkGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            listing.demonstrationNames.forEach { name ->
                KnowledgeBaseItem(name, false, enabled, onChosen)
            }
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
            .heightIn(min = 40.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (isOpen) {
            Text(
                "Open", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
