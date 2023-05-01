package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.model.condition.Condition
import io.rippledown.server.ServerApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


const val GET_OR_CREATE_CONDITION = "/api/condition/getOrCreate"

fun Application.conditionManagement(application: ServerApplication) {
    routing {
        post(GET_OR_CREATE_CONDITION) {
            val str = call.receiveText()
            val prototype = Json.decodeFromString(Condition.serializer(), str)
            val result = application.getOrCreateCondition(prototype)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}