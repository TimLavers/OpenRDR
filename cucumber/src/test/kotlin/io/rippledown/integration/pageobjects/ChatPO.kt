package io.rippledown.integration.pageobjects

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

    fun enterChatText(text: String) = execute {
        execute { chatEditableTextContext()?.setTextContents(text) }
    }

    fun clickSend() = execute {
        execute { chatTextContext().find(CHAT_SEND)?.accessibleAction?.doAccessibleAction(0) }
    }

}
