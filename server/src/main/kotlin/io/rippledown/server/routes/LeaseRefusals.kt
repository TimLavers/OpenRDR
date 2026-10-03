package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.rippledown.kb.lease.ProjectHeldException
import io.rippledown.kb.lease.projectHeldMessage

fun Application.leaseRefusals() {
    install(StatusPages) {
        exception<ProjectHeldException> { call, held ->
            call.respondText(projectHeldMessage(held.kbName, held.holder), status = Conflict)
        }
    }
}
