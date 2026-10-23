package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CHANGES
import io.rippledown.constants.api.KB_DESCRIPTION
import io.rippledown.constants.api.KB_INFO
import io.rippledown.constants.api.LAST_RULE_DESCRIPTION
import io.rippledown.constants.server.DEFAULT_CHANGES_LIMIT
import io.rippledown.constants.server.LIMIT
import io.rippledown.constants.server.SINCE_ID
import io.rippledown.server.ServerApplication

fun Application.kbEditing(application: ServerApplication) {
    routing {
        get(KB_INFO) {
            call.respond(kbEndpoint(application).kbInfo())
        }

        get(KB_DESCRIPTION) {
            call.respond(kbEndpoint(application).description())
        }

        post(KB_DESCRIPTION) {
            val newDescription = call.receive<String>()
            kbEndpoint(application).setDescription(newDescription, userId())
            call.respond(OK)
        }

        get(CHANGES) {
            val sinceId = call.parameters[SINCE_ID]?.toInt()
            val limit = call.parameters[LIMIT]?.toInt() ?: DEFAULT_CHANGES_LIMIT
            call.respond(kbEndpoint(application).changes(sinceId, limit))
        }

        get(LAST_RULE_DESCRIPTION){
            call.respond(kbEndpoint(application).descriptionOfMostRecentRule(userId()))
        }

        delete(LAST_RULE_DESCRIPTION){
            kbEndpoint(application).undoLastRule(userId())
            call.respond(OK)
        }
    }
}