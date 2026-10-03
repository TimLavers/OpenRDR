package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.routing.*
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.KB_NAME
import io.rippledown.constants.server.USER_ID_HEADER
import io.rippledown.model.UserId
import io.rippledown.server.KBEndpoint
import io.rippledown.server.ServerApplication

const val ID_SHOULD_BE_A_LONG = "CaseId should be a long."
const val MISSING_CASE_ID = "CaseId is missing."
const val MISSING_KB_NAME = "$KB_NAME is missing."
const val MISSING_KB_ID = "$KB_ID is missing."
const val MISSING_USER_ID = "$USER_ID_HEADER header is missing."

fun RoutingContext.userId(): UserId = call.request.headers.userId()

fun Headers.userId(): UserId {
    val header = this[USER_ID_HEADER]
    if (header.isNullOrBlank()) error(MISSING_USER_ID)
    return UserId(header.trim())
}

fun RoutingContext.caseId() = call.parameterValue(CASE_ID, MISSING_CASE_ID).toLongOrNull() ?: error(ID_SHOULD_BE_A_LONG)

fun RoutingContext.kbId() = call.parameterValue(KB_ID, MISSING_KB_ID)

fun RoutingContext.kbEndpoint(serverApplication: ServerApplication): KBEndpoint {
    val kbId = call.parameterValue(KB_ID, MISSING_KB_ID)
    return serverApplication.kbForId(kbId)
}

/**
 * The endpoint for a route that edits the knowledge base: the caller takes or
 * renews the project lease first, so a KB held by someone else is refused
 * before anything happens. See documentation/design/concurrent_users.md.
 */
fun RoutingContext.heldKbEndpoint(serverApplication: ServerApplication): KBEndpoint {
    val userId = userId()
    return kbEndpoint(serverApplication).also { it.session.hold(userId) }
}

fun RoutingContext.kbEndpointByName(serverApplication: ServerApplication): KBEndpoint {
    val kbId = call.parameterValue(KB_NAME, MISSING_KB_NAME)
    return serverApplication.kbForName(kbId)
}

fun RoutingCall.parameterValue(key: String, errorMessage: String): String {
    val value = this.parameters[key] ?: error(errorMessage)
    if (value.isBlank()) {
        error(errorMessage)
    }
    return value
}