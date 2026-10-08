package io.rippledown.main

import io.rippledown.model.UserId

const val USER_ID_PROPERTY = "openrdr.userId"
const val NO_USER_IDENTITY = "No user identity: set the $USER_ID_PROPERTY system property."

/**
 * Until a gateway injects the identity header, the desktop client identifies
 * itself: by the system property if set, else by the OS user name.
 */
fun defaultUserId(
    properties: (String) -> String? = System::getProperty
): UserId {
    val candidate = properties(USER_ID_PROPERTY)?.takeIf { it.isNotBlank() }
        ?: properties("user.name")?.takeIf { it.isNotBlank() }
        ?: error(NO_USER_IDENTITY)
    return UserId(candidate.trim())
}
