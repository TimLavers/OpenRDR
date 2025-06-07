package io.rippledown.integration.pageobjects

import io.rippledown.appbar.CHAT_ICON_TOGGLE
import io.rippledown.chat.BOT
import io.rippledown.chat.CHAT_SEND
import io.rippledown.chat.CHAT_TEXT_FIELD
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

    fun chatText(): String = execute<String> {
        chatTextContext()?.accessibleName ?: ""
    }

    fun enterChatText(text: String) =
        execute { chatEditableTextContext()?.setTextContents(text) }

    fun clickSend() =
        execute { chatTextContext().find(CHAT_SEND)?.accessibleAction?.doAccessibleAction(0) }

    fun clickChatIconToggle() =
        execute { contextProvider().find(CHAT_ICON_TOGGLE)?.accessibleAction?.doAccessibleAction(0) }

    fun botRowContainsText(text: String): Boolean {
        return execute<Boolean> {
            val matcher = { context: AccessibleContext ->
                context.foundText(text) && context.isBotResponse()
            }
            contextProvider().find(matcher) != null
        }
    }

}

fun AccessibleContext.foundText(text: String): Boolean =
    accessibleName?.contains(text) ?: false

fun AccessibleContext.isBotResponse(): Boolean =
    accessibleDescription?.startsWith(BOT) ?: false
