package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.constants.api.*
import io.rippledown.model.OperationResult
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
            val allParts = multipart.readAllParts()
            logger.info("KBManagement, import case, parts: ${allParts.size}")

            require(allParts.size == 1) {
                "Zip import takes a single file."
            }
            val part = allParts[0]
            val partReader = ByteArrayOutputStream()
            val buffered = BufferedOutputStream(partReader)
            val fileItem = part as PartData.FileItem
            fileItem.streamProvider().use {
                it.copyTo(buffered)
                it.close()
            }
            withContext(Dispatchers.IO) {
                buffered.flush()
            }
            val bytes = partReader.toByteArray()
            val kbInfo = application.importKBFromZip(bytes)
            call.respond(OK, kbInfo)
        }
        get(EXPORT_KB) {
            val kbEndpoint = kbEndpoint(application)
            val file = kbEndpoint.exportKBToZip()
            val kbName = kbEndpoint.kbName().name
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

        get(KB_INFO) {
            call.respond(kbEndpoint(application).kbName())
        }

        get(KB_LIST) {
            call.respond(application.kbList())
        }
    }
}