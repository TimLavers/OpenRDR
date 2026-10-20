package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.rippledown.constants.server.PING
import io.rippledown.constants.server.RESET
import io.rippledown.constants.server.SHUTDOWN
import io.rippledown.server.ResetRejectedException
import io.rippledown.server.ServerApplication
import io.rippledown.server.TestRequestGate

internal val testRequestGeneration = AttributeKey<Long>("testRequestGeneration")

fun Application.testRequestAdmission(gate: TestRequestGate) {
    intercept(ApplicationCallPipeline.Monitoring) {
        if (call.request.path() in setOf(PING, SHUTDOWN, RESET)) {
            proceed()
        } else {
            val generation = gate.admitRequest()
            if (generation == null) {
                call.respondText(
                    "Test server is resetting or requires replacement",
                    status = HttpStatusCode.ServiceUnavailable
                )
                finish()
            } else {
                call.attributes.put(testRequestGeneration, generation)
                try {
                    proceed()
                } finally {
                    gate.leaveRequest()
                }
            }
        }
    }
}

fun Application.testServerReset(application: ServerApplication) {
    routing {
        post(RESET) {
            try {
                application.reset()
                call.respondText("OK")
            } catch (e: ResetRejectedException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.Conflict)
            }
        }
    }
}
