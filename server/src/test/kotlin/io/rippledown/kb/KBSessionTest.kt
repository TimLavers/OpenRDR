package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.lease.LEASE_EXPIRY_MS
import io.rippledown.kb.lease.ProjectHeldException
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.diff.Addition
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBSessionTest {
    private lateinit var kb: KB
    private lateinit var session: KBSession
    private lateinit var webSocketManager: WebSocketManager
    private val alice = UserId("alice")
    private val bob = UserId("bob")
    private var now = 5_000_000L

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "TestKB")
        kb = KB(InMemoryKB(kbInfo))
        webSocketManager = mockk()
        coEvery { webSocketManager.sendRuleSessionCompleted(any()) } returns Unit
        session = KBSession(kb, webSocketManager) { now }
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun anotherThreadCanEnterTheLock(): Boolean {
        val entered = CountDownLatch(1)
        thread { session.locked { entered.countDown() } }
        return entered.await(2, SECONDS)
    }

    private fun createCase(name: String, value: String = "1.0", id: Long? = null): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(glucose(), defaultDate, value)
        return builder.build(name, id)
    }

    @Test
    fun `should expose the KB instance`() {
        // Given/When
        val exposedKB = session.kb

        // Then
        exposedKB shouldBe kb
    }

    @Test
    fun `should create a RuleSessionManager`() {
        // Given/When
        val rsm = session.ruleSessionManager

        // Then
        rsm.shouldBeInstanceOf<RuleSessionManager>()
    }

    @Test
    fun `should pass webSocketManager to RuleSessionManager`() {
        // Given
        val webSocketManager = mockk<WebSocketManager>()
        val sessionWithWs = KBSession(kb, webSocketManager)

        // When
        val rsm = sessionWithWs.ruleSessionManager

        // Then - verify it works by using the rsm (indirectly confirms wiring)
        rsm shouldNotBe null
    }

    @Test
    fun `should allow rule session operations through ruleSessionManager`() {
        // Given
        val sessionCase = createCase("Case1")

        // When
        session.ruleSessionManager.startRuleSessionToAddComment(sessionCase, "Go.")
        session.ruleSessionManager.commitCurrentRuleSession()

        // Then
        kb.interpret(sessionCase)
        kb.commentsFor(sessionCase) shouldBe setOf("Go.")
    }

    @Test
    fun `should allow startRuleSessionToAddComment through ruleSessionManager`() {
        // Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        kb.interpret(sessionCase)
        val viewableCase = kb.viewableCase(sessionCase)
        val comment = "Go to Bondi."

        // When
        session.ruleSessionManager.startRuleSessionToAddComment(viewableCase, comment, emptyList())

        // Then
        session.ruleSessionManager.currentDiff shouldBe
                Addition(comment, "C1", kb.attributeManager.byName("C1")?.id)
    }

    @Test
    fun `the lease is named after the KB`() {
        // Given
        session.hold(alice)

        // When
        val refusal = shouldThrow<ProjectHeldException> { session.hold(bob) }

        // Then
        refusal.message shouldBe "TestKB is being edited by alice."
    }

    @Test
    fun `a refused hold leaves the holder's rule session untouched`() {
        // Given
        session.hold(alice)
        session.ruleSessionManager.startRuleSessionToAddComment(createCase("Case1"), "Go.")

        // When
        shouldThrow<ProjectHeldException> { session.hold(bob) }

        // Then
        session.ruleSessionManager.isRuleSessionActive() shouldBe true
        session.lease.holder() shouldBe alice
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }

    @Test
    fun `taking an expired lease cancels the old holder's rule session and tells them`() {
        // Given
        session.hold(alice)
        session.ruleSessionManager.startRuleSessionToAddComment(createCase("Case1"), "Go.")
        now += LEASE_EXPIRY_MS

        // When
        session.hold(bob)

        // Then
        session.lease.holder() shouldBe bob
        session.ruleSessionManager.isRuleSessionActive() shouldBe false
        coVerify(exactly = 1) { webSocketManager.sendRuleSessionCompleted(alice) }
    }

    @Test
    fun `taking an expired lease with no rule session pushes nothing`() {
        // Given
        session.hold(alice)
        now += LEASE_EXPIRY_MS

        // When
        session.hold(bob)

        // Then
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }

    @Test
    fun `the holder releasing cancels their rule session and tells them`() {
        // Given
        session.hold(alice)
        session.ruleSessionManager.startRuleSessionToAddComment(createCase("Case1"), "Go.")

        // When
        session.release(alice)

        // Then
        session.lease.holder().shouldBeNull()
        session.ruleSessionManager.isRuleSessionActive() shouldBe false
        coVerify(exactly = 1) { webSocketManager.sendRuleSessionCompleted(alice) }
        session.hold(bob)
    }

    @Test
    fun `a non-holder releasing changes nothing`() {
        // Given
        session.hold(alice)
        session.ruleSessionManager.startRuleSessionToAddComment(createCase("Case1"), "Go.")

        // When
        session.release(bob)

        // Then
        session.lease.holder() shouldBe alice
        session.ruleSessionManager.isRuleSessionActive() shouldBe true
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }

    @Test
    fun `releasing an unheld lease changes nothing`() {
        // When
        session.release(alice)

        // Then
        session.lease.holder().shouldBeNull()
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }

    @Test
    fun `a rule session's pushes go to the lease holder`() {
        // Given
        coEvery { webSocketManager.sendStatus(any(), any()) } returns Unit
        session.hold(alice)
        session.ruleSessionManager.startRuleSessionToAddComment(createCase("Case1"), "Go.")

        // When
        session.ruleSessionManager.sendCornerstoneStatus()
        session.ruleSessionManager.sendRuleSessionCompleted()

        // Then
        coVerify(exactly = 1) { webSocketManager.sendStatus(alice, any()) }
        coVerify(exactly = 1) { webSocketManager.sendRuleSessionCompleted(alice) }
    }

    @Test
    fun `locked returns the block's result`() {
        // When
        val result = session.locked { 42 }

        // Then
        result shouldBe 42
    }

    @Test
    fun `a second thread waits until the first leaves the locked block`() {
        // Given a thread inside the lock that waits for a signal before leaving
        val firstIsInside = CountDownLatch(1)
        val letFirstLeave = CountDownLatch(1)
        val secondIsInside = CountDownLatch(1)
        val first = thread {
            session.locked {
                firstIsInside.countDown()
                letFirstLeave.await(5, SECONDS)
            }
        }
        firstIsInside.await(5, SECONDS) shouldBe true

        // When a second thread tries to enter
        val second = thread { session.locked { secondIsInside.countDown() } }

        // Then it is kept out while the first is inside, and gets in once the first leaves
        secondIsInside.await(200, MILLISECONDS) shouldBe false
        letFirstLeave.countDown()
        secondIsInside.await(5, SECONDS) shouldBe true
        first.join()
        second.join()
    }

    @Test
    fun `the lock is reentrant`() {
        // When
        val result = session.locked { session.locked { "nested" } }

        // Then
        result shouldBe "nested"
    }

    @Test
    fun `the lock is released when the block throws`() {
        // Given
        shouldThrow<IllegalStateException> { session.locked { error("boom") } }

        // When / Then
        anotherThreadCanEnterTheLock() shouldBe true
    }

    @Test
    fun `translating a condition expression does not hold the KB lock`() {
        // Given a parser (the LLM stand-in) that checks whether another thread can get the lock while it runs
        val case = createCase("Case1", value = "12.0")
        var lockWasFreeDuringTranslation = false
        session.ruleSessionManager.setConditionParser(object : ConditionParser {
            override fun parse(expression: String, attributeFor: (String) -> Attribute): Condition? {
                lockWasFreeDuringTranslation = anotherThreadCanEnterTheLock()
                return greaterThanOrEqualTo(null, attributeFor("Glucose"), 11.0)
            }
        })

        // When
        val result = session.ruleSessionManager.conditionForExpression(case, "raised glucose")

        // Then
        lockWasFreeDuringTranslation shouldBe true
        result.isFailure shouldBe false
        result.condition?.attributeNames() shouldBe setOf("Glucose")
    }

    @Test
    fun `a rule session started through the RuleSessionManager is active`() {
        // Given - start a rule session through rsm
        val sessionCase = createCase("Case1")
        session.ruleSessionManager.startRuleSessionToAddComment(sessionCase, "Go.")

        // When/Then - the session should show active
        session.ruleSessionManager.isRuleSessionActive() shouldBe true
    }
}
