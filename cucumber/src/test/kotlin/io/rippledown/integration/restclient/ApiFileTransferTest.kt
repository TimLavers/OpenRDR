package io.rippledown.integration.restclient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.constants.api.EXPORT_KB
import io.rippledown.constants.api.IMPORT_KB
import io.rippledown.constants.api.SELECT_KB
import io.rippledown.constants.server.KB_ID
import io.rippledown.files.FileSelection
import io.rippledown.files.KbFileDialogs
import io.rippledown.files.KbFileTransferController
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.KbFileDialogRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.io.IOException

class ApiFileTransferTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `import sends the archive bytes as multipart without a KB parameter`() = runTest {
        // Given
        val archive = File(directory, "Clinic backup.zip").apply { writeText("archive bytes") }
        val imported = KBInfo("imported", "Clinic")
        val api = Api(MockEngine { request ->
            request.url.encodedPath shouldBe IMPORT_KB
            request.url.parameters[KB_ID] shouldBe null
            request.body.contentType?.match(ContentType.MultiPart.FormData) shouldBe true
            val body = request.body.toByteArray().decodeToString()
            body shouldContain "archive bytes"
            body shouldContain "Clinic backup.zip"
            body shouldContain "Content-Disposition: form-data; name=document; filename=\"Clinic backup.zip\""
            respond(Json.encodeToString(imported), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            // When
            val result = api.importKBFromZip(archive)

            // Then
            result shouldBe imported
            api.kbInfo() shouldBe imported
        } finally {
            api.client.close()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["Invalid zip for KB import.", "Pathology is the name of a demonstration knowledge base; please choose another"])
    fun `HTTP validation errors reach chat and do not open a KB`(message: String) = runTest {
        // Given
        val archive = File(directory, "invalid.zip").apply { writeText("invalid") }
        val dialogs = mockk<KbFileDialogs>()
        coEvery { dialogs.chooseImportArchive() } returns FileSelection.Selected(archive)
        val api = Api(MockEngine { respond(message, HttpStatusCode.BadRequest) })
        val messages = mutableListOf<String>()
        val controller = KbFileTransferController(dialogs, api, { error("Must not open a KB") }, { messages.add(it) })
        try {
            // When
            controller.handle(KbFileDialogRequest.Import("one"))

            // Then
            messages shouldBe listOf("Import failed: $message")
            controller.state shouldBe KbFileTransferController.State.Idle
            shouldThrow<IllegalStateException> { api.kbInfo() }
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `empty HTTP error body still produces a useful failure and creates no file`() = runTest {
        // Given
        val destination = File(directory, "new.zip")
        val api = Api(MockEngine { respond("", HttpStatusCode.InternalServerError) })
        try {
            // When
            val failure = shouldThrow<IOException> { api.exportKBToZip(destination, KBInfo("id", "Clinic")) }

            // Then
            failure.message shouldBe "Server returned HTTP 500."
            destination.exists() shouldBe false
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `export uses the captured identity after another KB is selected`() = runTest {
        // Given
        val captured = KBInfo("original", "Clinic")
        val other = KBInfo("other", "Other")
        val bytes = byteArrayOf(80, 75, 0, -1, 12)
        val destination = File(directory, "Thyroïde backup.zip")
        val api = Api(MockEngine { request ->
            when (request.url.encodedPath) {
                SELECT_KB -> respond(
                    Json.encodeToString(other),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )

                EXPORT_KB -> {
                    request.url.parameters[KB_ID] shouldBe captured.id
                    respond(bytes, headers = headersOf(HttpHeaders.ContentType, "application/zip"))
                }

                else -> error("Unexpected request: ${request.url}")
            }
        })
        try {
            api.selectKB(other.id)

            // When
            api.exportKBToZip(destination, captured)

            // Then
            destination.readBytes() shouldBe bytes
            api.kbInfo() shouldBe other
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `failed export preserves the existing destination`() = runTest {
        // Given
        val destination = File(directory, "existing.zip").apply { writeText("previous archive") }
        val api = Api(MockEngine { respond("Export unavailable", HttpStatusCode.InternalServerError) })
        try {
            // When
            val failure = shouldThrow<IOException> { api.exportKBToZip(destination, KBInfo("id", "Clinic")) }

            // Then
            failure.message shouldBe "Export unavailable"
            destination.readText() shouldBe "previous archive"
        } finally {
            api.client.close()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["Invalid zip for KB import.", "Pathology is the name of a demonstration knowledge base; please choose another"])
    fun `import validation failure preserves the current KB and exposes the server message`(message: String) = runTest {
        // Given
        val current = KBInfo("original", "Clinic")
        val archive = File(directory, "archive.zip").apply { writeText("invalid archive") }
        val api = Api(MockEngine { request ->
            if (request.url.encodedPath == SELECT_KB)
                respond(Json.encodeToString(current), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            else respond(message, HttpStatusCode.BadRequest)
        })
        try {
            api.selectKB(current.id)

            // When
            val failure = shouldThrow<IOException> { api.importKBFromZip(archive) }

            // Then
            failure.message shouldBe message
            api.kbInfo() shouldBe current
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `local read failure sends no request`() = runTest {
        // Given
        val api = Api(MockEngine { error("No HTTP request expected") })
        try {
            // When
            shouldThrow<IOException> { api.importKBFromZip(File(directory, "missing.zip")) }

            // Then
            shouldThrow<IllegalStateException> { api.kbInfo() }
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `local write failure is propagated`() = runTest {
        // Given
        val api = Api(MockEngine { respond(byteArrayOf(1, 2, 3)) })
        try {
            // When
            shouldThrow<IOException> { api.exportKBToZip(directory, KBInfo("id", "Clinic")) }

            // Then
            directory.isDirectory shouldBe true
        } finally {
            api.client.close()
        }
    }
}
