package io.rippledown.server.routes

import io.ktor.server.application.*
import io.ktor.util.pipeline.*

fun PipelineContext<Unit, ApplicationCall>.longId(): Long {
    val id = call.parameters["id"] ?: error("Invalid id.")
    return id.toLongOrNull() ?: error("Id should be a long.")
}