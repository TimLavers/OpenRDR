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

fun Application.kbManagement(application: ServerApplication) {
    routing {
        post(IMPORT_KB) {
            logger.info("KBManagement, import kb...")

            val multipart = call.receiveMultipart()
            multipart.forEachPart { partData ->
                logger.info("KBManagement, import kb, part: $partData")
                if (partData is PartData.FileItem) {
                    val partReader = ByteArrayOutputStream()
                    val buffered = BufferedOutputStream(partReader)
                    partData.streamProvider().use { inputStream ->
                        inputStream.copyTo(buffered)
                        inputStream.close()
                    }
                    withContext(Dispatchers.IO) {
                        buffered.flush()
                    }
                    val bytes = partReader.toByteArray()
                    val kbInfo = application.importKBFromZip(bytes)
                    call.respond(OK, kbInfo)
                }
            }
        }

        get(EXPORT_KB) {
            val kbEndpoint = kbEndpoint(application)
            val file = kbEndpoint.exportKBToZip()
            val kbName = kbEndpoint.kbInfo().name
            call.response.header(
                HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName, "$kbName.zip"
                ).toString()
            )
            call.respondFile(file)
        }

        post(CREATE_KB) {
            val name = call.receive<String>()
            val kbInfo = application.createKB(name, true)
            call.respond(kbInfo)
        }

        post(CREATE_KB_FROM_SAMPLE) {
            val data = call.receive<Pair<String, SampleKB>>()
            val kbInfo = application.createKBFromSample(data.first, data.second)
            call.respond(kbInfo)
        }

        post(SELECT_KB) {
            logger.info("KBManagement: select kb...")
            val id = call.receive<String>()
            logger.info("KBManagement: select kb, id is: $id")
            val kbInfo = application.selectKB(id)
            call.respond(kbInfo)
        }

        delete(DELETE_KB) {
            application.deleteKB(kbId())
            call.respond(OK)
        }

        get(DEFAULT_KB) {
            call.respond(application.getDefaultProject())
        }

        get(KB_LIST) {
            call.respond(application.kbList())
        }
    }
}