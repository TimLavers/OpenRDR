package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.*
import io.rippledown.model.diff.Addition
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.serializeDeserialize
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * This test reproduces a rule building bug found by the
 * cucumber tests.
 */
class InterpManagementRealServerTest {
    private val persistenceProvider = InMemoryPersistenceProvider()
    private lateinit var app: ServerApplication
    private lateinit var kbEndpoint: KBEndpoint

    @BeforeTest
    fun setup() {
        app = ServerApplication(persistenceProvider)
        FileUtils.cleanDirectory(app.kbDataDir)
    }

    @Test
    fun `concurrent fetching of cornerstone cases`() = testApplication {
        val kbInfo =  app.createKBFromSample("Test TSH", SampleKB.TSH)
        kbEndpoint = app.kbFor(kbInfo)
        val caseId = kbEndpoint.waitingCasesInfo().caseIds[0]
        val start = SessionStartRequest(caseId.id!!, Addition("Cool new comment."))
        kbEndpoint.startRuleSession(start)
        runBlocking {
            repeat(1000) {
                launch(Dispatchers.Default) {
                    getCCAndSerializeIt()
                }
            }
        }
    }

    private fun getCCAndSerializeIt() {
        val status = kbEndpoint.selectCornerstone(0)
        status shouldBe serializeDeserialize(status)
    }
}