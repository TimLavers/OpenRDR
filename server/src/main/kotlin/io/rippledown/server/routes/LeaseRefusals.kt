package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.rippledown.constants.server.REFUSAL_HEADER
import io.rippledown.constants.server.REFUSAL_HELD
import io.rippledown.constants.server.REFUSAL_STALE
import io.rippledown.kb.lease.ProjectHeldException
import io.rippledown.kb.lease.projectHeldMessage
import io.rippledown.model.StaleRuleSessionException

fun Application.leaseRefusals() {
    install(StatusPages) {
        exception<ProjectHeldException> { call, held ->
            call.response.headers.append(REFUSAL_HEADER, REFUSAL_HELD)
            call.respondText(projectHeldMessage(held.kbName, held.holder), status = Conflict)
        }
        exception<StaleRuleSessionException> { call, stale ->
            call.response.headers.append(REFUSAL_HEADER, REFUSAL_STALE)
            call.respondText(stale.message, status = Conflict)
        }
    }
}
