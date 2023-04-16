package io.rippledown.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CASE_NAME
import io.rippledown.constants.api.DIFF
import io.rippledown.server.ServerApplication

fun Application.interpManagement(application: ServerApplication) {
    routing {
        get(DIFF) {
            val caseId = call.parameters[CASE_NAME]!!
            call.respond(application.diffListForCase(caseId))
        }
    }

}
