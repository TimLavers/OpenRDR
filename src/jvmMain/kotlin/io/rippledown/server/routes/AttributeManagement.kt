package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.model.OperationResult
import io.rippledown.server.ServerApplication

const val MOVE_ATTRIBUTE_JUST_BELOW_OTHER = "/api/moveAttributeJustBelowOther"
const val GET_OR_CREATE_ATTRIBUTE = "/api/attribute/getOrCreate"

fun Application.attributeManagement(application: ServerApplication) {
    routing {
        post(MOVE_ATTRIBUTE_JUST_BELOW_OTHER) {
            val attributeIdPair = call.receive<Pair<Int, Int>>()
            application.moveAttributeJustBelow(attributeIdPair.first, attributeIdPair.second)
            call.respond(HttpStatusCode.OK, OperationResult("Attribute moved"))
        }
        post(GET_OR_CREATE_ATTRIBUTE) {
            val name = call.receive<String>()
            val result = application.getOrCreateAttribute(name)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}