package io.rippledown.server.routes

import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.server.KBEndpoint
import io.rippledown.server.ServerApplication

const val ID_SHOULD_BE_A_LONG = "CaseId should be a long."
const val MISSING_CASE_ID = "CaseId is missing. Please provide a caseId in the URL."
private const val MISSING_KB_PARAMETER = "kb id is missing. Please provide a kb id in the URL."

fun RoutingContext.caseId(): Long {
    val caseId = call.parameters[CASE_ID] ?: error(MISSING_CASE_ID)
    return caseId.toLongOrNull() ?: error(ID_SHOULD_BE_A_LONG)
}

fun RoutingContext.kbId(): String {
    return call.parameters[KB_ID] ?: error(MISSING_KB_PARAMETER)
}

fun RoutingContext.kbEndpoint(serverApplication: ServerApplication): KBEndpoint {
    val kbId = call.parameters.getOrFail(KB_ID)
    return serverApplication.kbForId(kbId)
}