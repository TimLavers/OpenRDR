package io.rippledown.server.routes

import io.ktor.server.routing.*
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.constants.server.KB_NAME
import io.rippledown.server.KBEndpoint
import io.rippledown.server.ServerApplication

const val ID_SHOULD_BE_A_LONG = "CaseId should be a long."
const val MISSING_CASE_ID = "CaseId is missing."
const val MISSING_KB_NAME = "$KB_NAME is missing."
const val MISSING_KB_ID = "$KB_ID is missing."

fun RoutingContext.caseId() = call.parameterValue(CASE_ID, MISSING_CASE_ID).toLongOrNull() ?: error(ID_SHOULD_BE_A_LONG)
fun RoutingContext.caseIdOrNull() = call.parameters[CASE_ID]?.toLongOrNull() // TODO TEST

fun RoutingContext.kbId() = call.parameterValue(KB_ID, MISSING_KB_ID)

fun RoutingContext.kbEndpoint(serverApplication: ServerApplication): KBEndpoint {
    val kbId = call.parameterValue(KB_ID, MISSING_KB_ID)
    return serverApplication.kbForId(kbId)
}

fun RoutingContext.kbEndpointByName(serverApplication: ServerApplication): KBEndpoint {
    val kbName = call.parameterValue(KB_NAME, MISSING_KB_NAME)
    val result = serverApplication.kbForName(kbName)
    if (result.isSuccess) {
        return result.getOrThrow()
    } else {
        throw result.exceptionOrNull()!!
    }
}

fun RoutingCall.parameterValue(key: String, errorMessage: String): String {
    val value = this.parameters[key] ?: error(errorMessage)
    if (value.isBlank()) {
        error(errorMessage)
    }
    return value
}

fun RoutingContext.kbIdOrNull() = call.parameters[KB_ID]
