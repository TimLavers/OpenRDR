package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.GET_OR_CREATE_ATTRIBUTE
import io.rippledown.constants.api.MOVE_ATTRIBUTE
import io.rippledown.model.OperationResult
import io.rippledown.server.ServerApplication

fun Application.attributeManagement(application: ServerApplication) {
    routing {
        post(MOVE_ATTRIBUTE) {
            val attributeIdPair = call.receive<Pair<Int, Int>>()
            kbEndpoint(application).moveAttribute(attributeIdPair.first, attributeIdPair.second)
            call.respond(HttpStatusCode.OK, OperationResult("Attribute moved"))
        }
        post(GET_OR_CREATE_ATTRIBUTE) {
            val name = call.receive<String>()
            val result = kbEndpoint(application).getOrCreateAttribute(name)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}