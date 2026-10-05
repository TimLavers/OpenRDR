package io.rippledown.model.chat

import io.rippledown.model.KBInfo
import kotlinx.serialization.Serializable

/**
 * What a user's conversation is about, as seen by their windows: the open
 * knowledge base, if any, and the selected case, if any. A user's windows
 * share one conversation, so when one of them changes the context the
 * others are told and follow. See documentation/design/concurrent_users.md.
 */
@Serializable
data class ChatContextInfo(val kbInfo: KBInfo? = null, val caseId: Long? = null)
