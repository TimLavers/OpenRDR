package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.api.*
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.kb.lease.ProjectHeldException
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.*
import kotlin.test.Test

/**
 * The REST surface of the project lease: every guarded route takes the lease
 * for the caller before acting, a refusal is a 409 carrying the holder, and
 * reads take nothing. See documentation/design/concurrent_users.md, Stage 2.
 */
class LeaseGuardTest : OpenRDRServerTestBase() {
    private val alice = UserId("alice")
    private val status = CornerstoneStatus()

    private fun ApplicationTestBuilder.aliceHoldsTheLease() {
        every { kbSession.hold(testUser) } throws ProjectHeldException(kbName, alice)
    }

    private suspend fun HttpResponse.shouldBeRefusedByAlice() {
        status shouldBe Conflict
        bodyAsText() shouldBe "$kbName is being edited by alice."
    }

    @Test
    fun `starting a rule session takes the lease`() = testApplication {
        // Given
        setupServer()
        val request = SessionStartRequest(1L, Addition("Go."))
        every { kbEndpoint.startRuleSession(request) } returns status

        // When
        val result = httpClient.post(START_RULE_SESSION) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `starting a rule session on a KB someone else holds is refused before anything happens`() = testApplication {
        // Given
        setupServer()
        aliceHoldsTheLease()
        val request = SessionStartRequest(1L, Addition("Go."))

        // When
        val result = httpClient.post(START_RULE_SESSION) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        // Then
        result.shouldBeRefusedByAlice()
        verify(exactly = 0) { kbEndpoint.startRuleSession(any()) }
    }

    @Test
    fun `committing a rule session takes the lease`() = testApplication {
        // Given
        setupServer()
        val request = RuleRequest(1L, RuleConditionList())
        every { kbEndpoint.commitRuleSession(request) } returns mockk<ViewableCase>()

        // When
        httpClient.post(COMMIT_RULE_SESSION) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        // Then
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `cancelling a rule session takes the lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.cancelRuleSession() } returns Unit

        // When
        val result = httpClient.post(CANCEL_RULE_SESSION) { parameter(KB_ID, kbId) }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `updating cornerstones takes the lease`() = testApplication {
        // Given
        setupServer()
        val request = UpdateCornerstoneRequest(status, RuleConditionList())
        every { kbEndpoint.updateCornerstone(request) } returns status

        // When
        val result = httpClient.post(UPDATE_CORNERSTONES) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `exempting a cornerstone takes the lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.exemptCornerstone(2) } returns status

        // When
        val result = httpClient.post(EXEMPT_CORNERSTONE) {
            contentType(ContentType.Application.Json)
            setBody(2)
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `selecting a cornerstone takes the lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.selectCornerstone(1) } returns status

        // When
        val result = httpClient.get(SELECT_CORNERSTONE) {
            contentType(ContentType.Application.Json)
            setBody(1)
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `building a rule takes the lease`() = testApplication {
        // Given
        setupServer()
        val request = BuildRuleRequest("Case1", Addition("Go."), emptyList())
        every { kbEndpoint.buildRule(request) } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()

        // When
        val result = httpClient.post(BUILD_RULE) {
            contentType(ContentType.Application.Json)
            setBody(request)
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `undoing the last rule takes the lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.undoLastRule() } returns Unit

        // When
        val result = httpClient.delete(LAST_RULE_DESCRIPTION) { parameter(KB_ID, kbId) }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `setting the description takes the lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.setDescription("Thyroid rules") } returns Unit

        // When
        val result = httpClient.post(KB_DESCRIPTION) {
            contentType(ContentType.Text.Plain)
            setBody("Thyroid rules")
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `setting the description of a held KB is refused`() = testApplication {
        // Given
        setupServer()
        aliceHoldsTheLease()

        // When
        val result = httpClient.post(KB_DESCRIPTION) {
            contentType(ContentType.Text.Plain)
            setBody("Thyroid rules")
            parameter(KB_ID, kbId)
        }

        // Then
        result.shouldBeRefusedByAlice()
        verify(exactly = 0) { kbEndpoint.setDescription(any()) }
    }

    @Test
    fun `deleting a case takes the lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.deleteCase("Case1") } returns Unit
        every { kbEndpoint.waitingCasesInfo() } returns CasesInfo()

        // When
        val result = httpClient.delete(DELETE_CASE_WITH_NAME) {
            parameter("name", "Case1")
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `moving an attribute takes the lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.moveAttribute(1, 2) } returns Unit

        // When
        val result = httpClient.post(MOVE_ATTRIBUTE) {
            contentType(ContentType.Application.Json)
            setBody(Pair(1, 2))
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `setting the attribute order takes the lease`() = testApplication {
        // Given
        setupServer()
        val attributes = listOf(Attribute(1, "A"), Attribute(2, "B"))
        every { kbEndpoint.setAttributeOrder(attributes) } returns Unit

        // When
        val result = httpClient.post(SET_ATTRIBUTE_ORDER) {
            contentType(ContentType.Application.Json)
            setBody(attributes)
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { kbSession.hold(testUser) }
    }

    @Test
    fun `deleting a KB passes the caller to the application`() = testApplication {
        // Given
        setupServer()
        every { serverApplication.deleteKB(kbId, testUser) } returns null

        // When
        val result = httpClient.delete(DELETE_KB) { parameter(KB_ID, kbId) }

        // Then
        result.status shouldBe HttpStatusCode.NoContent
        verify(exactly = 1) { serverApplication.deleteKB(kbId, testUser) }
    }

    @Test
    fun `deleting a held KB is refused`() = testApplication {
        // Given
        setupServer()
        every { serverApplication.deleteKB(kbId, testUser) } throws ProjectHeldException(kbName, alice)

        // When
        val result = httpClient.delete(DELETE_KB) { parameter(KB_ID, kbId) }

        // Then
        result.shouldBeRefusedByAlice()
    }

    @Test
    fun `renaming a KB passes the caller to the application`() = testApplication {
        // Given
        setupServer()
        val renamed = KBInfo(kbId, "Thyroid Function")
        every { serverApplication.renameKB(kbId, "Thyroid Function", testUser) } returns renamed

        // When
        val result = httpClient.post(RENAME_KB) {
            contentType(ContentType.Text.Plain)
            setBody("Thyroid Function")
            parameter(KB_ID, kbId)
        }

        // Then
        result.status shouldBe OK
        verify(exactly = 1) { serverApplication.renameKB(kbId, "Thyroid Function", testUser) }
    }

    @Test
    fun `reading a case takes no lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.viewableCase(7L) } returns mockk<ViewableCase>()

        // When
        httpClient.get(CASE) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, 7L)
        }

        // Then
        verify(exactly = 0) { kbSession.hold(any()) }
    }

    @Test
    fun `reading the description takes no lease`() = testApplication {
        // Given
        setupServer()
        every { kbEndpoint.description() } returns "Thyroid rules"

        // When
        val result = httpClient.get(KB_DESCRIPTION) { parameter(KB_ID, kbId) }

        // Then
        result.status shouldBe OK
        verify(exactly = 0) { kbSession.hold(any()) }
    }

    @Test
    fun `a guarded route without an identity is an error and takes no lease`() = testApplication {
        // Given
        setupServer()
        val anonymousClient = createClient { }

        // When
        val result = anonymousClient.post(CANCEL_RULE_SESSION) { parameter(KB_ID, kbId) }

        // Then
        result.status shouldBe HttpStatusCode.InternalServerError
        verify(exactly = 0) { kbSession.hold(any()) }
    }
}
