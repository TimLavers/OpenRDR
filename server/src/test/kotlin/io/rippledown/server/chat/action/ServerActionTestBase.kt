package io.rippledown.server.chat.action

import io.mockk.mockk
import io.rippledown.server.ServerChatActionsInterface

open class ServerActionTestBase {
    lateinit var actionsInterface: ServerChatActionsInterface

    open fun setup() {
        actionsInterface = mockk()
    }
}
