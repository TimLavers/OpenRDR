package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.*
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
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
            val interpretationWithDiffs = application.commitRuleSession(ruleRequest)
            call.respond(HttpStatusCode.OK, interpretationWithDiffs)
        }

        post(START_RULE_SESSION) {
            val sessionStartRequest = call.receive<SessionStartRequest>()
            val cornerstoneStatus = application.startRuleSession(sessionStartRequest)
            call.respond(HttpStatusCode.OK, cornerstoneStatus)
        }

        post(UPDATE_CORNERSTONES) {
            val request = call.receive<UpdateCornerstoneRequest>()
            val cornerstoneStatus = application.updateCornerstone(request)
            call.respond(HttpStatusCode.OK, cornerstoneStatus)
        }

        get(SELECT_CORNERSTONE) {
            val cornerstoneIndex = call.parameters[INDEX_PARAMETER]?.toInt() ?: error("Invalid cornerstone index.")
            val cornerstoneStatus = application.cornerstoneStatusForIndex(cornerstoneIndex)
            call.respond(cornerstoneStatus)
        }
    }
}
