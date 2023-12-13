package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.ADD_CONDITION
import io.rippledown.constants.api.COMMIT_SESSION
import io.rippledown.constants.api.START_SESSION_TO_ADD_CONCLUSION
import io.rippledown.constants.api.START_SESSION_TO_REPLACE_CONCLUSION
import io.rippledown.model.Conclusion
import io.rippledown.model.OperationResult
import io.rippledown.model.condition.Condition
import io.rippledown.server.ServerApplication
import kotlinx.serialization.json.Json

fun Application.ruleSession(application: ServerApplication) {
    routing {
        post(START_SESSION_TO_ADD_CONCLUSION) {
            val id = longId()
            val conclusion = call.receive<Conclusion>()
            application.startRuleSessionToAddConclusion(id, conclusion)
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
        }
        post(START_SESSION_TO_REPLACE_CONCLUSION) {
            val conclusionPair = call.receive<List<Conclusion>>()
            application.startRuleSessionToReplaceConclusion(longId(), conclusionPair[0], conclusionPair[1])
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
        }
        post(ADD_CONDITION) {
            val str = call.receiveText()
            val condition = Json.decodeFromString(Condition.serializer(), str)
            application.addConditionToCurrentRuleBuildingSession(condition)
            call.respond(HttpStatusCode.OK, OperationResult("Condition added"))
        }
        post(COMMIT_SESSION) {
            application.commitCurrentRuleSession()
            call.respond(HttpStatusCode.OK, OperationResult("Session committed"))
        }
    }
}