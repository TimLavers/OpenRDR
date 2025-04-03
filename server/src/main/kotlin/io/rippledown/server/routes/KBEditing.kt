package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.*
import io.rippledown.sample.SampleKB
import io.rippledown.server.ServerApplication
import io.rippledown.server.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

fun Application.kbEditing(application: ServerApplication) {
    routing {
        get(KB_INFO) {
            call.respond(kbEndpoint(application).kbName())
        }

        get(KB_DESCRIPTION) {
            call.respond(kbEndpoint(application).description())
        }

        post(KB_DESCRIPTION) {
            val newDescription = call.receive<String>()
            kbEndpoint(application).setDescription(newDescription)
            call.respond(OK)
        }

        get(LAST_RULE_DESCRIPTION){
            call.respond(kbEndpoint(application).descriptionOfMostRecentRule())
        }
    }
}