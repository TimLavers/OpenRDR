package io.rippledown.server.action

import io.mockk.mockk
import io.rippledown.server.ServerChatActionsInterface

open class ActionTestBase {
    lateinit var actionsInterface: ServerChatActionsInterface

    open fun setup() {
        actionsInterface = mockk()
    }
}
