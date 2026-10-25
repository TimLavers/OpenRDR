package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.POSITIVE_CHANGE_LIMIT_MESSAGE
import io.rippledown.kb.chat.KnowledgeBaseService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ListRecentChanges(
    val limit: Int = 10,
    val attribute: String? = null,
    private val clock: Clock = Clock.systemDefaultZone()
) : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        if (kbService.openKnowledgeBase() == null) return done(NO_KB_OPEN_MESSAGE)
        if (limit <= 0) return done(POSITIVE_CHANGE_LIMIT_MESSAGE)
        val records = try {
            kbService.recentChanges(limit, attribute)
        } catch (e: IllegalArgumentException) {
            return done(e.message ?: "Could not read the recent changes.")
        }
        if (records.isEmpty()) {
            return done(
                if (attribute == null) "No changes have been recorded for this knowledge base."
                else "No recorded changes concern \"$attribute\"."
            )
        }
        val today = LocalDate.now(clock)
        return done(records.joinToString("\n") { record ->
            val time = Instant.ofEpochMilli(record.timestamp).atZone(clock.zone)
            val format = if (time.toLocalDate() == today) timeFormat else dateTimeFormat
            "${time.format(format)} — ${record.userId}: ${record.summary}"
        })
    }

    companion object {
        private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
        private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)
    }
}
