package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.GET_OR_CREATE_ATTRIBUTE
import io.rippledown.constants.api.MOVE_ATTRIBUTE
import io.rippledown.constants.api.SET_ATTRIBUTE_ORDER
import io.rippledown.model.Attribute
import io.rippledown.model.OperationResult
import io.rippledown.server.ServerApplication

fun Application.attributeManagement(application: ServerApplication) {
    routing {
        post(MOVE_ATTRIBUTE) {
            val kbEndpoint = kbEndpoint(application)
            val attributeIdPair = call.receive<Pair<Int, Int>>()
            kbEndpoint.moveAttribute(attributeIdPair.first, attributeIdPair.second)
            call.respond(OK, OperationResult("Attribute moved"))
        }
        post(GET_OR_CREATE_ATTRIBUTE) {
            val kbEndpoint = kbEndpoint(application)
            val name = call.receive<String>()
            val result = kbEndpoint.getOrCreateAttribute(name)
            call.respond(OK, result)
        }
        post(SET_ATTRIBUTE_ORDER) {
            val kbEndpoint = kbEndpoint(application)
            val attributesInOrder = call.receive<List<Attribute>>()
            kbEndpoint.setAttributeOrder(attributesInOrder)
            call.respond(OK, OperationResult("Attribute order set"))
        }
    }
}
