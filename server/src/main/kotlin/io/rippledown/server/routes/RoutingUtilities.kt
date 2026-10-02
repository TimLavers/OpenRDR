package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.routing.*
import io.rippledown.constants.server.*
import io.rippledown.model.UserId
import io.rippledown.server.KBEndpoint
import io.rippledown.server.ServerApplication
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

const val ID_SHOULD_BE_A_LONG = "CaseId should be a long."
const val MISSING_CASE_ID = "CaseId is missing."
const val MISSING_KB_NAME = "$KB_NAME is missing."
const val MISSING_KB_ID = "$KB_ID is missing."

val LOCAL_USER = UserId(LOCAL_USER_ID)

private val identityLogger = LoggerFactory.getLogger("io.rippledown.server.routes.UserIdentity")

// Warned once rather than per request: the packaged demo and the cucumber suite
// run unauthenticated by design, and a warning on every call would drown the log.
private val warnedAboutMissingUserId = AtomicBoolean(false)

fun RoutingContext.userId(): UserId = call.request.headers.userId()

fun Headers.userId(): UserId {
    val header = this[USER_ID_HEADER]
    if (header.isNullOrBlank()) {
        if (warnedAboutMissingUserId.compareAndSet(false, true)) {
            identityLogger.warn("No $USER_ID_HEADER header on request; using the local user id '$LOCAL_USER_ID'.")
        }
        return LOCAL_USER
    }
    return UserId(header.trim())
}

fun RoutingContext.caseId() = call.parameterValue(CASE_ID, MISSING_CASE_ID).toLongOrNull() ?: error(ID_SHOULD_BE_A_LONG)

fun RoutingContext.kbId() = call.parameterValue(KB_ID, MISSING_KB_ID)

fun RoutingContext.kbEndpoint(serverApplication: ServerApplication): KBEndpoint {
    val kbId = call.parameterValue(KB_ID, MISSING_KB_ID)
    return serverApplication.kbForId(kbId)
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