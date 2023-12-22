package io.rippledown.server.routes

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import io.rippledown.server.KBEndpoint
import io.rippledown.server.ServerApplication

const val KB_ID = "kb"
fun PipelineContext<Unit, ApplicationCall>.longId(): Long {
    val id = call.parameters["id"] ?: error("Invalid id.")
    return id.toLongOrNull() ?: error("Id should be a long.")
}
fun PipelineContext<Unit, ApplicationCall>.kbId(): String {
    return call.parameters[KB_ID] ?: error("Missing kb parameter.")
}
fun PipelineContext<Unit, ApplicationCall>.kbEndpoint(serverApplication: ServerApplication): KBEndpoint {
    return serverApplication.kbForId(kbId())
}
