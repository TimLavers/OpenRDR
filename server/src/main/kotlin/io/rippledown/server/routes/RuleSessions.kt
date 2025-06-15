package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.EXPRESSION
import io.rippledown.log.lazyLogger
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.server.ServerApplication

fun Application.ruleSession(application: ServerApplication) {
    val logger = lazyLogger
    routing {

        post(START_RULE_SESSION) {
            logger.info(START_RULE_SESSION)
            val sessionStartRequest = call.receive<SessionStartRequest>()
            logger.info("session start request: $sessionStartRequest")
            val cornerstoneStatus = kbEndpoint(application).startRuleSession(sessionStartRequest)
            call.respond(HttpStatusCode.OK, cornerstoneStatus)
            logger.info("start rule session returned with OK")
        }

        post(COMMIT_RULE_SESSION) {
            logger.info(COMMIT_RULE_SESSION)
            val ruleRequest = call.receive<RuleRequest>()
            logger.info("commit session request: $ruleRequest")
            val viewableCase = kbEndpoint(application).commitRuleSession(ruleRequest)
            call.respond(HttpStatusCode.OK, viewableCase)
        }

        post(CANCEL_RULE_SESSION) {
            logger.info(CANCEL_RULE_SESSION)
            kbEndpoint(application).cancelRuleSession()
            call.respond(HttpStatusCode.OK)
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
            val index = call.receive<Int>()
            val updatedCornerstoneStatus = kbEndpoint(application).selectCornerstone(index)
            call.respond(HttpStatusCode.OK, updatedCornerstoneStatus)
        }

        get(CONDITION_FOR_EXPRESSION) {
            val expression = call.parameters[EXPRESSION] ?: error("Invalid expression.")
            val conditionParsingResult = kbEndpoint(application).conditionForExpression(expression)
            call.respondNullable(HttpStatusCode.OK, conditionParsingResult)
            logger.info("Condition for expression '$expression' was '${conditionParsingResult.condition?.asText()}'")
        }
    }
}