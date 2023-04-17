package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.server.ServerApplication

const val GET_OR_CREATE_CONCLUSION = "/api/conclusion/getOrCreate"

fun Application.conclusionManagement(application: ServerApplication) {
    routing {
        post(GET_OR_CREATE_CONCLUSION) {
            val name = call.receive<String>()
            val result = application.getOrCreateAttribute(name)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}