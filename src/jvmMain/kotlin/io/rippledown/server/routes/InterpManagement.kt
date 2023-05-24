package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.BUILD_RULE
import io.rippledown.constants.api.VERIFIED_INTERPRETATION_SAVED
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.RuleRequest
import io.rippledown.server.ServerApplication

fun Application.interpManagement(application: ServerApplication) {
    routing {

        post(VERIFIED_INTERPRETATION_SAVED) {
            val interpretation = call.receive<Interpretation>()
            val interpretationWithDiffs = application.saveInterpretation(interpretation)
            call.respond(HttpStatusCode.OK, interpretationWithDiffs)
        }

        post(BUILD_RULE) {
            val ruleRequest = call.receive<RuleRequest>()
            val interpretationWithDiffs = application.buildRule(ruleRequest)
            call.respond(HttpStatusCode.OK, interpretationWithDiffs)
        }

    }


}
