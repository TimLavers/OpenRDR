package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.ATTRIBUTE_NAMES
import io.rippledown.constants.server.EXPRESSION
import io.rippledown.model.Conclusion
import io.rippledown.model.OperationResult
import io.rippledown.model.condition.Condition
import io.rippledown.server.ServerApplication
import io.rippledown.server.logger
import kotlinx.serialization.json.Json

fun Application.ruleSession(application: ServerApplication) {
    routing {
        post(START_SESSION_TO_ADD_CONCLUSION) {
            val id = longId()
            val conclusion = call.receive<Conclusion>()
            kbEndpoint(application).startRuleSessionToAddConclusion(id, conclusion)
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
            logger.info("session started to add conclusion $conclusion")
        }
        post(START_SESSION_TO_REMOVE_CONCLUSION) {
            val conclusion = call.receive<Conclusion>()
            kbEndpoint(application).startRuleSessionToRemoveConclusion(longId(), conclusion)
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
        }
        post(START_SESSION_TO_REPLACE_CONCLUSION) {
            val conclusionPair = call.receive<List<Conclusion>>()
            kbEndpoint(application).startRuleSessionToReplaceConclusion(longId(), conclusionPair[0], conclusionPair[1])
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
        }
        post(ADD_CONDITION) {
            val str = call.receiveText()
            val condition = Json.decodeFromString(Condition.serializer(), str)
            kbEndpoint(application).addConditionToCurrentRuleBuildingSession(condition)
            call.respond(HttpStatusCode.OK, OperationResult("Condition added"))
        }
        post(COMMIT_SESSION) {
            kbEndpoint(application).commitCurrentRuleSession()
            call.respond(HttpStatusCode.OK, OperationResult("Session committed"))
            logger.info("session committed")
        }
        get(TIP_FOR_EXPRESSION) {
            val expression = call.parameters[EXPRESSION] ?: error("Invalid expression.")
            val attributeNames = call.parameters[ATTRIBUTE_NAMES] ?: error("Invalid expression.")
            val tip = kbEndpoint(application).tipForExpression(expression, attributeNames)
            call.respond(HttpStatusCode.OK, tip)
            logger.info("tip for expression '$expression' and attributes '$attributeNames' was '$tip'")
        }
    }
}