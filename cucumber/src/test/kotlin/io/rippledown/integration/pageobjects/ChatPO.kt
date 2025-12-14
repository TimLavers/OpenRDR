package io.rippledown.integration.pageobjects

import io.rippledown.appbar.CHAT_ICON_TOGGLE
import io.rippledown.chat.BOT
import io.rippledown.chat.CHAT_SEND
import io.rippledown.chat.CHAT_TEXT_FIELD
import io.rippledown.chat.NUMBER_OF_CHAT_MESSAGES_
import io.rippledown.integration.utils.find
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleEditableText

// ORD2
class ChatPO(private val contextProvider: () -> AccessibleContext) {

    private fun chatTextContext() =
        execute<AccessibleContext> { contextProvider().find(CHAT_TEXT_FIELD) }

    private fun chatEditableTextContext() =
        execute<AccessibleEditableText> { chatTextContext().accessibleEditableText }

    fun enterChatText(text: String) =
        execute { chatEditableTextContext()?.setTextContents(text) }

    fun clickSend() {
        execute { chatTextContext().find(CHAT_SEND)?.accessibleAction?.doAccessibleAction(0) }
    }

    fun clickChatIconToggle() =
        execute { contextProvider().find(CHAT_ICON_TOGGLE)?.accessibleAction?.doAccessibleAction(0) }

    fun mostRecentBotRowContainsTerms(terms: List<String>): Boolean {
        val numberOfChatMessages = numberOfChatMessages()
        return execute<Boolean> {
            val matcher = { context: AccessibleContext ->
                terms.all { term -> context.foundText(term) } && context.isBotResponseForIndex(numberOfChatMessages - 1)
            }
            contextProvider().find(matcher) != null
        }
    }

    fun mostRecentBotRowIs(expected: String): Boolean {
        return execute<Boolean> {
            val matcher = { context: AccessibleContext ->
                context.foundText(expected)
            }
            contextProvider().find(matcher) != null
        }
    }

    fun numberOfChatMessages(): Int =
        execute<Int> {
            val matcher = { context: AccessibleContext ->
                context.accessibleDescription?.startsWith(NUMBER_OF_CHAT_MESSAGES_) ?: false
            }
            contextProvider().find(matcher)
                ?.accessibleDescription
                ?.substring(NUMBER_OF_CHAT_MESSAGES_.length)
                ?.toIntOrNull() ?: 0
        }
}

fun AccessibleContext.foundText(text: String): Boolean =
    accessibleName?.contains(text) ?: false

fun AccessibleContext.isBotResponseForIndex(index: Int): Boolean =
    accessibleDescription == "$BOT$index"


