package io.rippledown.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.rippledown.server.ServerApplication

const val GET_OR_CREATE_CONDITION = "/api/condition/getOrCreate"

fun Application.conditionManagement(application: ServerApplication) {
    routing {
        post(GET_OR_CREATE_CONDITION) {
            TODO()
//            val name = call.receive<String>()
//            val result = application.getOrCreateAttribute(name)
//            call.respond(HttpStatusCode.OK, result)
        }
    }
}