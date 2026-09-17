package io.rippledown.integration.restclient

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.rippledown.constants.api.KB_DESCRIPTION
import io.rippledown.constants.api.SELECT_KB
import io.rippledown.constants.server.KB_ID
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ApiKbDescriptionTest {
    @Test
    fun `description request uses the captured KB even after selection changes`() = runTest {
        // Given
        val original = KBInfo("first", "First")
        val selected = KBInfo("second", "Second")
        val api = Api(MockEngine { request ->
            when (request.url.encodedPath) {
                SELECT_KB -> respond(
                    Json.encodeToString(selected),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )

                else -> {
                    request.url.encodedPath shouldBe KB_DESCRIPTION
                    respond(
                        "Description for ${request.url.parameters[KB_ID]}",
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
        })
        try {
            api.selectKB(selected.id)

            // When
            val captured = api.kbDescription(original)
            val current = api.kbDescription()

            // Then
            captured shouldBe "Description for first"
            current shouldBe "Description for second"
        } finally {
            api.client.close()
        }
    }
}
