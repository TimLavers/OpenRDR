package io.rippledown.main

import io.rippledown.constants.server.LOCAL_USER_ID
import io.rippledown.model.UserId

const val USER_ID_PROPERTY = "openrdr.userId"

/**
 * Until a gateway injects the identity header, the desktop client identifies
 * itself: by the system property if set, else by the OS user name.
 */
fun defaultUserId(
    properties: (String) -> String? = System::getProperty
): UserId {
    val candidate = properties(USER_ID_PROPERTY)?.takeIf { it.isNotBlank() }
        ?: properties("user.name")?.takeIf { it.isNotBlank() }
        ?: LOCAL_USER_ID
    return UserId(candidate.trim())
}
