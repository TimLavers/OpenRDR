package io.rippledown.integration.restclient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.rippledown.constants.api.IMPORT_KB
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ApiImportTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `import returns the imported KB and makes it current`() = runTest {
        // Given
        val imported = KBInfo("imported_id", "Imported")
        val api = apiReturning(Json.encodeToString(imported))
        val archive = File(directory, "kb.zip").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            // When
            val result = api.importKBFromZip(archive)

            // Then
            result.id shouldBe imported.id
            result.name shouldBe imported.name
            api.kbInfo() shouldBe imported
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `an import without a returned KB reports a descriptive state error`() = runTest {
        // Given
        val api = apiReturning("null")
        val archive = File(directory, "kb.zip").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            // When
            val error = shouldThrow<IllegalStateException> { api.importKBFromZip(archive) }

            // Then
            error.message shouldBe "Import did not return a knowledge base."
        } finally {
            api.client.close()
        }
    }

    private fun apiReturning(body: String) = Api(MockEngine { request ->
        request.method shouldBe HttpMethod.Post
        request.url.encodedPath shouldBe IMPORT_KB
        respond(body, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    })
}
