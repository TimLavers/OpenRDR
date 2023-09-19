package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.GET_OR_CREATE_CONCLUSION
import io.rippledown.server.ServerApplication

fun Application.conclusionManagement(application: ServerApplication) {
    routing {
        post(GET_OR_CREATE_CONCLUSION) {
            val text = call.receive<String>()
            val result = application.getOrCreateConclusion(text)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}