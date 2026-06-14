package io.rippledown.chat

import io.rippledown.chat.ChatTestHook.snapshotRef
import io.rippledown.chat.ChatTestHook.update
import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only observation surface for the live state of [ChatPanel].
 *
 * Why this exists: cucumber integration tests run in the same JVM as the
 * Compose Desktop UI, and need to poll the bot's current message / the
 * current suggestion list to decide when to step the scenario forward.
 * Going through the AWT/Compose accessibility bridge to answer those
 * questions is prohibitively slow when the window also contains a large
 * case table — a single tree walk can block the EDT for many seconds,
 * visibly freezing animations and dragging scenarios out to well over a
 * minute. See `cucumber/.../ChatPO` for the polling call sites.
 *
 * Contract: [ChatPanel] calls [update] from a `SideEffect` on every
 * successful composition, so readers are guaranteed to see values that
 * match what the Composable was last asked to render. Production code
 * never reads this object; it is effectively free when no test is
 * observing (one atomic reference write per chat recomposition).
 *
 * Thread-safety: [snapshotRef] is atomic; reads and writes can come from
 * any thread. Readers always see a self-consistent [Snapshot].
 */
object ChatTestHook {

    data class Snapshot(
        val messageList: List<ChatMessage>,
        val suggestionRowCount: Int,
        val mostRecentBotText: String?,
        val mostRecentSuggestionText: String?,
        val sendIsEnabled: Boolean
    ) {
        companion object {
            val EMPTY = Snapshot(
                messageList = emptyList(),
                suggestionRowCount = 0,
                mostRecentBotText = null,
                mostRecentSuggestionText = null,
                // Default to false so tests that poll `waitForChatReady`
                // block until the UI has had a chance to publish its
                // first real state.
                sendIsEnabled = false
            )
        }
    }

    private val snapshotRef = AtomicReference(Snapshot.EMPTY)

    /**
     * Publish a new snapshot derived from [messages]. Intended to be
     * called from a Compose `SideEffect` inside [ChatPanel].
     */
    fun update(messages: List<ChatMessage>, sendIsEnabled: Boolean) {
        val mostRecentBot = messages.lastOrNull { it is BotMessage }?.text
        val mostRecentSuggestion = (messages.lastOrNull { it is SuggestionListMessage } as? SuggestionListMessage)
            ?.let { msg ->
                msg.suggestions.mapIndexed { i, s ->
                    "${i + 1}. ${s.removeSuffix(EDITABLE_MARKER)}"
                }.joinToString("\n")
            }
        val suggestionRowCount = messages.count { it is SuggestionListMessage }
        snapshotRef.set(
            Snapshot(
                messageList = messages,
                suggestionRowCount = suggestionRowCount,
                mostRecentBotText = mostRecentBot,
                mostRecentSuggestionText = mostRecentSuggestion,
                sendIsEnabled = sendIsEnabled
            )
        )
    }

    fun snapshot(): Snapshot = snapshotRef.get()

    /**
     * Reset to an empty snapshot. Useful between cucumber scenarios when
     * the Compose window persists across runs and the previous
     * scenario's tail state would otherwise be momentarily visible to
     * the next scenario's first poll.
     */
    fun reset() {
        snapshotRef.set(Snapshot.EMPTY)
    }
}
