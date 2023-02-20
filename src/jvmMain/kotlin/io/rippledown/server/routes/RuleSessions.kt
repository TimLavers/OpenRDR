package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.model.Conclusion
import io.rippledown.model.OperationResult
import io.rippledown.model.condition.Condition
import io.rippledown.server.ServerApplication
import kotlinx.serialization.json.Json

const val START_SESSION_TO_ADD_CONCLUSION = "/api/startSessionToAddConclusion"
const val START_SESSION_TO_REPLACE_CONCLUSION = "/api/startSessionToReplaceConclusion"
const val ADD_CONDITION = "/api/addCondition"
const val COMMIT_SESSION = "/api/commitSession"

fun Application.ruleSession(application: ServerApplication) {
    routing {
        post(START_SESSION_TO_ADD_CONCLUSION) {
            val id = call.parameters["id"] ?: error("Invalid case id.")
            val conclusion = call.receive<Conclusion>()
            application.startRuleSessionToAddConclusion(id, conclusion)
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
        }
        post(START_SESSION_TO_REPLACE_CONCLUSION) {
            val id = call.parameters["id"] ?: error("Invalid case id.")
            val conclusionPair = call.receive<List<Conclusion>>()
            application.startRuleSessionToReplaceConclusion(id, conclusionPair[0], conclusionPair[1])
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