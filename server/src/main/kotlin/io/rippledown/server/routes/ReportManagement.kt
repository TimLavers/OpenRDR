package io.rippledown.server.routes

import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.CASE_REPORT
import io.rippledown.log.lazyLogger
import io.rippledown.server.ServerApplication

fun Application.reportManagement(application: ServerApplication) {
    val logger = lazyLogger
    routing {
        get(CASE_REPORT) {
            try {
                val endpoint = kbEndpoint(application)
                val report = endpoint.caseReport(caseId())
                call.respond(report)
            } catch (e: Exception) {
                // Report generation failures are server-side (e.g. the LLM call),
                // so respond 500 and keep the exception detail in the server log
                // rather than returning it to the client.
                logger.error("caseReport failed", e)
                call.respond(InternalServerError)
            }
        }
    }
}
