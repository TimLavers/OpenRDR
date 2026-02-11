package io.rippledown.server

import io.rippledown.model.KBInfo
import io.rippledown.server.chat.KbEditInterface

interface ServerChatActionsInterface {
    fun kbList(): List<KBInfo>
    fun openKB(name: String): Result<KBInfo>
    fun kb(id: String): KbEditInterface
}