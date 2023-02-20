package io.rippledown.server.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.rippledown.model.OperationResult
import io.rippledown.server.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

const val KB_INFO = "/api/kbInfo"
const val CREATE_KB = "/api/createKB"
const val IMPORT_KB = "/api/importKB"
const val EXPORT_KB = "/api/exportKB"

fun Application.kbManagement(application: ServerApplication) {
    routing {
        post(CREATE_KB) {
            application.createKB()
            call.respond(HttpStatusCode.OK, OperationResult("KB created"))
        }
        post(IMPORT_KB) {
            val multipart = call.receiveMultipart()
            val allParts = multipart.readAllParts()
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
            application.importKBFromZip(bytes)
            call.respond(HttpStatusCode.OK, OperationResult("KB imported"))
        }
        get(EXPORT_KB) {
            val file = application.exportKBToZip()
            val kbName = application.kbName().name
            call.response.header(
                HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName, "$kbName.zip"
            ).toString())
            call.respondFile(file)
        }
        get(KB_INFO) {
            call.respond(application.kbName())
        }
    }
}