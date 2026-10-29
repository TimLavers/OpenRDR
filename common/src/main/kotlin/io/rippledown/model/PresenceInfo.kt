package io.rippledown.model

import kotlinx.serialization.Serializable

/**
 * Who is on a knowledge base right now: connected and with it open. Pushed to
 * everyone and filtered by the client on [kbId]. See concurrent_users.md.
 */
@Serializable
data class PresenceInfo(val kbId: String, val users: List<PresentUser>)

@Serializable
data class PresentUser(val userId: String, val editing: Boolean)
