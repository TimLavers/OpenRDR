package io.rippledown.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.components.DefinitionDetailRow
import io.rippledown.model.chat.AiDefinitionDraftView

const val AI_DRAFT_CARD = "AI_DRAFT_CARD"
const val AI_DRAFT_VALUE_PREFIX = "AI_DRAFT_VALUE_"

/**
 * An AI definition draft offer as a two-column card: the row headings on the
 * left, their values on the right, with no grid lines. The Surface exposes the
 * message's full text as its merged content description, exactly as [BotRow]
 * does, so the card is read and matched the same way as a plain bot message.
 */
@Composable
fun AiDraftCard(message: AiDefinitionDraftMessage, index: Int) {
    val draft = message.draft
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
            .testTag(AI_DRAFT_CARD)
            .semantics(mergeDescendants = true) { contentDescription = "$BOT$index:${message.text}" }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DraftRow("Name", draft.attributeName)
            DraftRow("Refers to", draft.source)
            DraftRow("Criteria", draft.criteria)
            if (draft.examples.isNotEmpty()) {
                DraftRow("Examples", draft.examples.joinToString("\n") {
                    "${it.input} → ${it.expected}" + (it.explanation?.let { e -> " ($e)" } ?: "")
                })
            }
            draft.run { previewValue ?: previewMessage }?.let { DraftRow(draft.valueLabel, it) }
            draft.evidence?.let { DraftRow("Evidence", it) }
            if (draft.changesEverywhere) {
                Text(
                    "This changes the default definition everywhere.",
                    color = Color.DarkGray,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp
                )
            }
            draft.prompt?.let {
                Text(it, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DraftRow(heading: String, value: String) {
    DefinitionDetailRow(heading, value, Modifier.testTag("$AI_DRAFT_VALUE_PREFIX$heading"))
}

fun main() = androidx.compose.ui.window.application {
    androidx.compose.ui.window.Window(onCloseRequest = ::exitApplication, title = "AI draft") {
        AiDraftCard(
            AiDefinitionDraftMessage(
                "preview",
                AiDefinitionDraftView(
                    "hasDiabetes", "clinical_notes",
                    "True when the notes clearly affirm a personal diagnosis.",
                    listOf(io.rippledown.model.rule.AiBooleanExample("T2 Diab", true)),
                    previewValue = "true", evidence = "T2 Diab", prompt = "Accept this definition?"
                )
            ), 0
        )
    }
}
