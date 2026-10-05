package io.rippledown.kb

import io.rippledown.model.UserId

fun projectHeldMessage(kbName: String, editor: UserId) = "$kbName is being edited by $editor."

/**
 * Deleting a knowledge base was refused because another user has a rule
 * session in progress on it. See documentation/design/concurrent_users.md.
 */
class ProjectHeldException(val kbName: String, val holder: UserId) :
    RuntimeException(projectHeldMessage(kbName, holder))
