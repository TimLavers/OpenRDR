package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CONDITION_HINTS
import io.rippledown.constants.api.GET_OR_CREATE_CONDITION
import io.rippledown.model.condition.Condition
import io.rippledown.server.ServerApplication
import io.rippledown.server.logger
import kotlinx.serialization.json.Json

fun Application.conditionManagement(application: ServerApplication) {
    routing {
        get(CONDITION_HINTS) {
            val kbEndpoint = kbEndpoint(application)
            val conditionHints = kbEndpoint.conditionHintsForCase(caseId())
            try {
                Json.encodeToString(conditionHints)
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("COULD NOT SERIALISE HINTS", e)
            }
            call.respond(conditionHints)
        }

        post(GET_OR_CREATE_CONDITION) {
            val str = call.receiveText()
            val prototype = Json.decodeFromString(Condition.serializer(), str)
            val result = kbEndpoint(application).getOrCreateCondition(prototype)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
