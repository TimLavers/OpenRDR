package io.rippledown.integration.pageobjects

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.chat.*
import io.rippledown.chat.ChatTestHook.snapshot
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.renderedText
import io.rippledown.voice.CHAT_MIC_BUTTON
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
        await().atMost(ofSeconds(60)).until { snapshot().sendIsEnabled }
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

    fun clickMic() {
        await().atMost(ofSeconds(10)).until {
            try {
                execute<Boolean> {
                    val node = chatTextContext().find(CHAT_MIC_BUTTON) ?: return@execute false
                    node.accessibleAction?.doAccessibleAction(0)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun mostRecentBotRowContainsTerms(terms: List<String>): Boolean {
        // Read the in-JVM test hook rather than walking the accessibility
        // tree — see [ChatTestHook] for the rationale.
        val text = snapshot().mostRecentBotText
        return text != null && terms.all { text.contains(it, ignoreCase = true) }
    }

    fun mostRecentBotRowContainsAnyOfTheTerms(terms: List<String>): Boolean {
        val text = snapshot().mostRecentBotText
        return text != null && terms.any { text.contains(it, ignoreCase = true) }
    }

    fun mostRecentTipRowContainsTerms(terms: List<String>): Boolean {
        val text = snapshot().mostRecentTipText
        return text != null && terms.all { text.contains(it, ignoreCase = true) }
    }

    fun numberOfTipMessages(): Int =
        snapshot().messageList.count { it is TipMessage }

    fun mostRecentBotRowDoesNotContainTheTerm(term: String) {
        val text = snapshot().mostRecentBotText
        val found = text != null && text.contains(term, ignoreCase = true)
        withClue("did not expect to find the text $term") {
            found shouldBe false
        }
    }

    fun mostRecentSuggestionRowContainsTerms(terms: List<String>): Boolean {
        val text = snapshot().mostRecentSuggestionText
        return text != null && terms.all { text.contains(it, ignoreCase = true) }
    }

    fun mostRecentSuggestionRowDoesNotContainsTerm(term: String): Boolean {
        val text = snapshot().mostRecentSuggestionText ?: return true
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
        val text = snapshot().mostRecentSuggestionText ?: return emptyList()
        if (text.isEmpty()) return emptyList()
        return text.lines().map { line ->
            // Each line is "<n>. <suggestion text>"; strip the numbering prefix.
            val dot = line.indexOf(". ")
            if (dot >= 0) line.substring(dot + 2) else line
        }
    }

    fun numberOfSuggestionRows(): Int = snapshot().suggestionRowCount

    fun chatTextFieldContains(text: String): Boolean {
        return execute<Boolean> {
            // Compose 1.11's accessibility bridge overrides accessibleName
            // on Text-like nodes with the contentDescription used to locate
            // the chat field. Read the actual edited characters via
            // AccessibleText (renderedText) instead.
            val ctx = chatTextContext() ?: return@execute false
            renderedText(ctx).contains(text)
        }
    }

    fun numberOfChatMessages(): Int = snapshot().messageList.size

    fun messageList() = snapshot().messageList
}


