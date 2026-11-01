package io.rippledown.main

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.rippledown.constants.api.CHANGES
import io.rippledown.constants.server.*
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ApiChangesTest {
    @Test
    fun `changes sends the KB cursor and identity and requests all missed records`() = runTest {
        // Given
        val records = (8..68).map { ChangeRecord(it, "Bob", 1L, ChangeKind.RULES_ADDED, "Rule $it") }
        var request: Url? = null
        val engine = MockEngine {
            if (it.url.encodedPath == CHANGES) {
                request = it.url
                it.headers[USER_ID_HEADER] shouldBe "Alice"
                respond(records.toJsonString(), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else respond(KBInfo("clinic", "Clinic").toJsonString(), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = Api(engine, userId = UserId("Alice"))
        try {
            api.selectKB("clinic")

            // When
            val result = api.changes(7)

            // Then
            result shouldBe records
            request?.parameters?.get(KB_ID) shouldBe "clinic"
            request?.parameters?.get(SINCE_ID) shouldBe "7"
            request?.parameters?.get(LIMIT) shouldBe Int.MAX_VALUE.toString()
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `an absent cursor is omitted from the query`() = runTest {
        // Given
        val engine = MockEngine {
            if (it.url.encodedPath == CHANGES) {
                it.url.parameters.contains(SINCE_ID) shouldBe false
                respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else respond(KBInfo("clinic", "Clinic").toJsonString(), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = Api(engine)
        try {
            api.selectKB("clinic")

            // When
            val result = api.changes()

            // Then
            result shouldBe emptyList()
        } finally {
            api.client.close()
        }
    }

    @Test
    fun `changes requires an open KB`() = runTest {
        // Given
        val api = Api(MockEngine { error("No request expected") })
        try {
            // When
            val failure = shouldThrow<IllegalStateException> { api.changes() }

            // Then
            failure.message shouldBe "No knowledge base is open."
        } finally {
            api.client.close()
        }
    }
}
