package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.*
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.server.ServerApplication
import io.rippledown.server.logger

fun Application.interpManagement(application: ServerApplication) {
    routing {

        post(BUILD_RULE) {
            logger.info(BUILD_RULE)
            val ruleRequest = call.receive<RuleRequest>()
            logger.info("rule request: $ruleRequest")
            val viewableCase = kbEndpoint(application).commitRuleSession(ruleRequest)
            call.respond(HttpStatusCode.OK, viewableCase)
        }

        post(START_RULE_SESSION) {
            logger.info(START_RULE_SESSION)
            val sessionStartRequest = call.receive<SessionStartRequest>()
            logger.info("session start request: $sessionStartRequest")
            val cornerstoneStatus = kbEndpoint(application).startRuleSession(sessionStartRequest)
            call.respond(HttpStatusCode.OK, cornerstoneStatus)
            logger.info("start rule session returned with OK")
        }

        post(UPDATE_CORNERSTONES) {
            logger.info(UPDATE_CORNERSTONES)
            val request = call.receive<UpdateCornerstoneRequest>()
            val cornerstoneStatus = kbEndpoint(application).updateCornerstone(request)
            call.respond(HttpStatusCode.OK, cornerstoneStatus)
        }

        post(EXEMPT_CORNERSTONE) {
            val index = call.receive<Int>()
            val updatedCornerstoneStatus = kbEndpoint(application).exemptCornerstone(index)
            call.respond(HttpStatusCode.OK, updatedCornerstoneStatus)
        }

        get(SELECT_CORNERSTONE) {
            val cornerstoneIndex = call.parameters[INDEX_PARAMETER]?.toInt() ?: error("Invalid cornerstone index.")
            val cornerstone = kbEndpoint(application).cornerstoneForIndex(cornerstoneIndex)
            call.respond(cornerstone)
        }
    }
}
