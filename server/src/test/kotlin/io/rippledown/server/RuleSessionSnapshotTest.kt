package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.rippledown.constants.api.CORNERSTONE_STATUS
import io.rippledown.constants.server.KB_ID
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.UserId
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.persistence.inmemory.InMemoryKB
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RuleSessionSnapshotTest : OpenRDRServerTestBase() {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `snapshot distinguishes an inactive session from an active one without cornerstones`(active: Boolean) =
        testApplication {
            // Given
            setupServer()
            val session = KBSession(KB(InMemoryKB(KBInfo(kbId, kbName))))
            val endpoint = KBEndpoint(session)
            every { serverApplication.kbForId(kbId) } returns endpoint
            val manager = session.ruleSessionManagerFor(testUser)
            if (active) {
                val case = RDRCaseBuilder().build("Case")
                session.kb.interpret(case)
                manager.startRuleSessionToAddComment(case, "Advice.")
            }

            // When
            val response = httpClient.get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) }

            // Then
            response.status shouldBe if (active) HttpStatusCode.OK else HttpStatusCode.NoContent
            if (active) response.body<CornerstoneStatus>() shouldBe manager.cornerstoneStatus()
            manager.isRuleSessionActive() shouldBe active
            clientFor(UserId("other-user")).get(CORNERSTONE_STATUS) { parameter(KB_ID, kbId) }
                .status shouldBe HttpStatusCode.NoContent
        }
}
