package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CONDITION_HINTS
import io.rippledown.constants.api.GET_OR_CREATE_CONDITION
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.server.ServerApplication
import io.rippledown.server.logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.conditionManagement(application: ServerApplication) {
    routing {
        get(CONDITION_HINTS) {
            val id = longId() // todo test
            val conditionHints = kbEndpoint(application).conditionHintsForCase(id)
            logger.info("$CONDITION_HINTS, suggestions: $conditionHints")
            try {
                val encodedSuggestions = Json.encodeToString(conditionHints)
                logger.info("$CONDITION_HINTS, encoded suggestions: $encodedSuggestions")
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
