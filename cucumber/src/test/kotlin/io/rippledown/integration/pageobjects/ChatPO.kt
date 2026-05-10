package io.rippledown.integration.pageobjects

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.chat.*
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAll
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleEditableText

// ORD2
class ChatPO(private val contextProvider: () -> AccessibleContext) {

    /**
     * Cached reference to the chat-panel root accessibility node (the
     * Column whose description starts with [NUMBER_OF_CHAT_MESSAGES_], see
     * `ChatPanel.kt`). All chat-related lookups should be scoped to this
     * subtree rather than the window root, otherwise every poll walks the
     * case table — for cases like Einstein with ~130 attributes that costs
     * many seconds per poll, swamping the cuke run with non-LLM latency.
     *
     * The cache is invalidated lazily: on each access we verify the cached
     * node still exposes a matching description and is still attached to a
     * parent. If not we re-discover it from the window root.
     */
    private var cachedChatRoot: AccessibleContext? = null

    private fun chatRoot(): AccessibleContext? = execute<AccessibleContext?> {
        cachedChatRoot
            ?.takeIf {
                try {
                    it.accessibleDescription?.startsWith(NUMBER_OF_CHAT_MESSAGES_) == true &&
                            it.accessibleParent != null
                } catch (_: Exception) {
                    false
                }
            }
            ?: contextProvider().find(
                matcher = { ctx: AccessibleContext ->
                    ctx.accessibleDescription?.startsWith(NUMBER_OF_CHAT_MESSAGES_) == true
                }
            )?.also { cachedChatRoot = it }
    }

    private fun chatTextContext() =
        execute<AccessibleContext> { contextProvider().find(CHAT_TEXT_FIELD) }

    private fun chatEditableTextContext() =
        execute<AccessibleEditableText> { chatTextContext().accessibleEditableText }

    /**
     * Blocks until the chat text field becomes the focused compose node, or the
     * timeout elapses. Useful for synchronizing after a case selection: chat's
     * LaunchedEffect(id) steals focus once startConversation returns, and tests
     * that then need to restore focus elsewhere must wait for this steal to
     * happen first (otherwise it races against later key presses).
     */
    fun waitForChatToBeFocused() {
        await().atMost(ofSeconds(10)).until { isChatFocused() }
    }

    /**
     * Like [waitForChatToBeFocused] but returns quietly if the chat never
     * becomes focused within [timeoutSeconds]. This is appropriate for
     * best-effort serialization before an arrow-key press: if the previous
     * case selection did not change the current case, or if accessibility
     * actions already moved focus away from chat, no focus-steal is pending
     * and we should simply proceed rather than hanging for 10 seconds.
     *
     * @return true if the chat ended up focused before the timeout.
     */
    fun waitForChatToBeFocusedQuietly(timeoutSeconds: Long = 2): Boolean =
        try {
            await().atMost(ofSeconds(timeoutSeconds)).until { isChatFocused() }
            true
        } catch (_: Throwable) {
            false
        }

    private fun isChatFocused(): Boolean = try {
        execute<Boolean> {
            chatTextContext()?.accessibleStateSet
                ?.contains(javax.accessibility.AccessibleState.FOCUSED) ?: false
        }
    } catch (_: Exception) {
        false
    }

    /**
     * Blocks until [ChatTestHook] reports `sendIsEnabled = true` (i.e. the
     * chat is currently idle and can accept a new user message). Prevents
     * the test from racing ahead and typing / sending into a field that
     * is still disabled while the client is processing a prior message,
     * which was previously masked by slow accessibility polling and now
     * surfaces as silently-dropped user input.
     */
    private fun waitForChatReady() {
        await().atMost(ofSeconds(60)).until { ChatTestHook.snapshot().sendIsEnabled }
    }

    fun enterChatText(text: String) {
        waitForChatReady()
        await().atMost(ofSeconds(60)).until {
            try {
                execute { chatEditableTextContext()?.setTextContents(text) }
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun clickSend() {
        waitForChatReady()
        execute { chatTextContext().find(CHAT_SEND)?.accessibleAction?.doAccessibleAction(0) }
    }


    /**
     * Per-index cache of bot-row accessibility nodes. Because chat
     * messages only grow and each bot row keeps its index for life, once
     * a node with description prefix "$BOT$index:" has been located we
     * can re-use that reference on subsequent polls and read its
     * description directly — no tree recursion.
     *
     * This matters because in a Compose Desktop window with a large case
     * table, each `AccessibleContext.find` walks enough of the semantics
     * tree that it blocks the EDT for seconds and visibly freezes other
     * EDT-driven work (e.g. the typing-indicator animation).
     */
    private val cachedBotRowByIndex = mutableMapOf<Int, AccessibleContext>()

    /**
     * Negative cache: index -> the `numberOfChatMessages` value at which
     * we last failed to find a bot-text node at that index. If the count
     * hasn't changed since, we don't re-scan: either the row at `index`
     * isn't a bot text message (it's a user row or a suggestion list) or
     * no new messages have arrived, so the result cannot have changed.
     */
    private val missedBotRowAtCount = mutableMapOf<Int, Int>()

    /**
     * Reads the bot-row contentDescription encoded by [BotRow] as
     * "$BOT$index:$text" and returns the text suffix, or null if the row
     * at [index] isn't a bot text message (e.g. it's a SuggestionListRow,
     * whose outer description is just "$BOT$index" with no colon).
     *
     * [currentCount] is the current `numberOfChatMessages()` — used to
     * invalidate the negative cache when new messages arrive.
     *
     * MUST be called from the EDT (wrap in `execute`).
     */
    private fun botRowText(root: AccessibleContext, index: Int, currentCount: Int): String? {
        val prefix = "$BOT$index:"

        cachedBotRowByIndex[index]?.let { cached ->
            val desc = try {
                cached.accessibleDescription
            } catch (_: Exception) {
                null
            }
            if (desc != null && desc.startsWith(prefix)) {
                return desc.substring(prefix.length)
            }
            // Stale entry — drop and fall through to re-find.
            cachedBotRowByIndex.remove(index)
        }

        // Negative-cache short-circuit: already scanned at the same count
        // and found nothing; skip another expensive tree walk.
        if (missedBotRowAtCount[index] == currentCount) return null

        val node = root.find(
            matcher = { ctx: AccessibleContext ->
                ctx.accessibleDescription?.startsWith(prefix) == true
            }
        )
        if (node == null) {
            missedBotRowAtCount[index] = currentCount
            return null
        }
        cachedBotRowByIndex[index] = node
        missedBotRowAtCount.remove(index)
        return node.accessibleDescription?.substring(prefix.length)
    }

    fun mostRecentBotRowContainsTerms(terms: List<String>): Boolean {
        // Read the in-JVM test hook rather than walking the accessibility
        // tree — see [ChatTestHook] for the rationale.
        val text = ChatTestHook.snapshot().mostRecentBotText
        return text != null && terms.all { it -> text.contains(it, ignoreCase = true) }
    }

    fun mostRecentBotRowContainsAnyOfTheTerms(terms: List<String>): Boolean {
        val text = ChatTestHook.snapshot().mostRecentBotText
        return text != null && terms.any { it -> text.contains(it, ignoreCase = true) }
    }

    fun mostRecentBotRowDoesNotContainTheTerm(term: String) {
        val text = ChatTestHook.snapshot().mostRecentBotText
        val found = text != null && text.contains(term, ignoreCase = true)
        withClue("did not expect to find the text $term") {
            found shouldBe false
        }
    }

    /**
     * Cache of the highest-index suggestion-list node seen so far, keyed
     * by the `numberOfChatMessages` value at the time it was discovered.
     * Mirrors [cachedBotRowByIndex]: a new suggestion row can only appear
     * when the overall chat message count changes, so we use the count as
     * a cheap invalidation signal (reading it is a single
     * `accessibleDescription` access on the chat root — no tree walk).
     */
    private var cachedLatestSuggestionNode: AccessibleContext? = null
    private var cachedLatestSuggestionCount: Int = -1

    /**
     * Reads the encoded contentDescription on the most recent
     * SuggestionListRow (see `SuggestionListRow.kt`), which has the form
     * "$SUGGESTION_LIST$index:<numbered-suggestion-text>". Returns the
     * text suffix or null if no suggestion row is present.
     *
     * MUST be called from the EDT (wrap in `execute`).
     */
    private fun mostRecentSuggestionText(root: AccessibleContext, currentCount: Int): String? {
        // Fast path: if the message count hasn't changed since we last
        // scanned, the cached node is still the most recent one.
        cachedLatestSuggestionNode
            ?.takeIf { currentCount == cachedLatestSuggestionCount }
            ?.let { cached ->
                val desc = try {
                    cached.accessibleDescription
                } catch (_: Exception) {
                    null
                }
                if (desc != null && desc.startsWith(SUGGESTION_LIST)) {
                    val colon = desc.indexOf(':')
                    return if (colon >= 0) desc.substring(colon + 1) else ""
                }
            }

        val nodes = root.findAll({ ctx ->
            ctx.accessibleDescription?.startsWith(SUGGESTION_LIST) ?: false
        })
        if (nodes.isEmpty()) {
            cachedLatestSuggestionNode = null
            cachedLatestSuggestionCount = currentCount
            return null
        }
        // findAll returns a Set; pick the node whose trailing index is
        // largest to get the "most recent" row deterministically.
        val latest = nodes.maxByOrNull { node ->
            val desc = node.accessibleDescription ?: return@maxByOrNull -1
            val afterPrefix = desc.substring(SUGGESTION_LIST.length)
            afterPrefix.substringBefore(':').toIntOrNull() ?: -1
        } ?: return null
        cachedLatestSuggestionNode = latest
        cachedLatestSuggestionCount = currentCount
        val desc = latest.accessibleDescription ?: return null
        val colon = desc.indexOf(':')
        return if (colon >= 0) desc.substring(colon + 1) else ""
    }

    fun mostRecentSuggestionRowContainsTerms(terms: List<String>): Boolean {
        val text = ChatTestHook.snapshot().mostRecentSuggestionText
        return text != null && terms.all { it -> text.contains(it, ignoreCase = true) }
    }

    fun mostRecentSuggestionRowDoesNotContainsTerm(term: String): Boolean {
        val text = ChatTestHook.snapshot().mostRecentSuggestionText ?: return true
        return !text.contains(term, ignoreCase = true)
    }

    fun clickSuggestion(text: String) {
        val description = "$SUGGESTION_ITEM$text"
        await().atMost(ofSeconds(10)).until {
            try {
                execute<Boolean> {
                    val root = chatRoot() ?: return@execute false
                    val suggestionItem = root.find(description)
                    if (suggestionItem != null) {
                        suggestionItem.accessibleAction?.doAccessibleAction(0)
                        true
                    } else {
                        false
                    }
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Suggestions in the most recent suggestion list, in display order.
     *
     * Sourced from `ChatTestHook.snapshot().mostRecentSuggestionText` (a
     * numbered, newline-separated string like "1. alpha\n2. beta") rather
     * than the accessibility tree, because Compose's lazy list rendering
     * does not consistently emit accessibility nodes for off-screen items.
     */
    fun suggestionsInMostRecentMessage(): List<String> {
        val text = ChatTestHook.snapshot().mostRecentSuggestionText ?: return emptyList()
        if (text.isEmpty()) return emptyList()
        return text.lines().map { line ->
            // Each line is "<n>. <suggestion text>"; strip the numbering prefix.
            val dot = line.indexOf(". ")
            if (dot >= 0) line.substring(dot + 2) else line
        }
    }

    fun numberOfSuggestionRows(): Int = ChatTestHook.snapshot().suggestionRowCount

    fun chatTextFieldContains(text: String): Boolean {
        return execute<Boolean> {
            val chatText = chatTextContext()?.accessibleName ?: ""
            chatText.contains(text)
        }
    }

    fun numberOfChatMessages(): Int = ChatTestHook.snapshot().messageCount
}


