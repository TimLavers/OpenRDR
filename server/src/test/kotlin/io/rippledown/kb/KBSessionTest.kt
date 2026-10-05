package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.lessThanOrEqualTo
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

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "TestKB")
        kb = KB(InMemoryKB(kbInfo))
        webSocketManager = mockk()
        coEvery { webSocketManager.sendRuleSessionCompleted(any()) } returns Unit
        session = KBSession(kb, webSocketManager)
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
    fun `a user gets the same rule session manager each time`() {
        // Given/When
        val rsm = session.ruleSessionManagerFor(alice)

        // Then
        rsm.shouldBeInstanceOf<RuleSessionManager>()
        session.ruleSessionManagerFor(alice) shouldBeSameInstanceAs rsm
    }

    @Test
    fun `different users get rule session managers with independent state`() {
        // Given
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(createCase("Case1"), "Go.")

        // When / Then
        session.ruleSessionManagerFor(bob).isRuleSessionActive() shouldBe false
        session.ruleSessionManagerFor(alice).isRuleSessionActive() shouldBe true
    }

    @Test
    fun `each user's rule session manager works on the one KB`() {
        // Given
        val sessionCase = createCase("Case1")
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(sessionCase, "Go.")
        session.ruleSessionManagerFor(alice).commitCurrentRuleSession()

        // When
        val description = session.ruleSessionManagerFor(bob).descriptionOfMostRecentRule()

        // Then
        description.description shouldNotBe ""
        kb.ruleTree.size() shouldBe 2
    }

    @Test
    fun `should allow rule session operations through ruleSessionManager`() {
        // Given
        val sessionCase = createCase("Case1")

        // When
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(sessionCase, "Go.")
        session.ruleSessionManagerFor(alice).commitCurrentRuleSession()

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
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(viewableCase, comment, emptyList())

        // Then
        session.ruleSessionManagerFor(alice).currentDiff shouldBe
                Addition(comment, "C1", kb.attributeManager.byName("C1")?.id)
    }

    @Test
    fun `two users can have rule sessions on the one KB at once`() {
        // Given
        val case1 = createCase("Case1", "1.0")
        val case2 = createCase("Case2", "2.0")
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(case1, "Go.")
        session.ruleSessionManagerFor(alice).addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose(), 1.5))
        session.ruleSessionManagerFor(bob).startRuleSessionToAddComment(case2, "Stop.")
        session.ruleSessionManagerFor(bob).addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 1.5))

        // When
        session.ruleSessionManagerFor(alice).commitCurrentRuleSession()
        session.ruleSessionManagerFor(bob).commitCurrentRuleSession()

        // Then
        kb.ruleTree.size() shouldBe 3
        kb.interpret(case1)
        kb.interpret(case2)
        kb.commentsFor(case1) shouldBe setOf("Go.")
        kb.commentsFor(case2) shouldBe setOf("Stop.")
    }

    @Test
    fun `the users editing are those with an active rule session`() {
        // Given
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(createCase("Case1"), "Go.")
        session.ruleSessionManagerFor(bob)

        // When / Then
        session.usersEditing() shouldBe setOf(alice)
    }

    @Test
    fun `nobody is editing a KB with no rule sessions`() {
        // Given
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(createCase("Case1"), "Go.")
        session.ruleSessionManagerFor(alice).cancelRuleSession()

        // When / Then
        session.usersEditing() shouldBe emptySet()
    }

    @Test
    fun `cancelling a user's rule session cancels only theirs and tells them`() {
        // Given
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(createCase("Case1"), "Go.")
        session.ruleSessionManagerFor(bob).startRuleSessionToAddComment(createCase("Case2"), "Stop.")

        // When
        session.cancelRuleSessionOf(alice)

        // Then
        session.ruleSessionManagerFor(alice).isRuleSessionActive() shouldBe false
        session.ruleSessionManagerFor(bob).isRuleSessionActive() shouldBe true
        coVerify(exactly = 1) { webSocketManager.sendRuleSessionCompleted(alice) }
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(bob) }
    }

    @Test
    fun `cancelling for a user with no rule session pushes nothing`() {
        // Given
        session.ruleSessionManagerFor(alice)

        // When
        session.cancelRuleSessionOf(alice)
        session.cancelRuleSessionOf(bob)

        // Then
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }

    @Test
    fun `a rule session's pushes go to its user`() {
        // Given
        coEvery { webSocketManager.sendStatus(any(), any()) } returns Unit
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(createCase("Case1"), "Go.")

        // When
        session.ruleSessionManagerFor(alice).sendCornerstoneStatus()
        session.ruleSessionManagerFor(alice).sendRuleSessionCompleted()

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
        session.ruleSessionManagerFor(alice).setConditionParser(object : ConditionParser {
            override fun parse(expression: String, attributeFor: (String) -> Attribute): Condition? {
                lockWasFreeDuringTranslation = anotherThreadCanEnterTheLock()
                return greaterThanOrEqualTo(null, attributeFor("Glucose"), 11.0)
            }
        })

        // When
        val result = session.ruleSessionManagerFor(alice).conditionForExpression(case, "raised glucose")

        // Then
        lockWasFreeDuringTranslation shouldBe true
        result.isFailure shouldBe false
        result.condition?.attributeNames() shouldBe setOf("Glucose")
    }

    @Test
    fun `a rule session started through the RuleSessionManager is active`() {
        // Given - start a rule session through rsm
        val sessionCase = createCase("Case1")
        session.ruleSessionManagerFor(alice).startRuleSessionToAddComment(sessionCase, "Go.")

        // When/Then - the session should show active
        session.ruleSessionManagerFor(alice).isRuleSessionActive() shouldBe true
    }
}
