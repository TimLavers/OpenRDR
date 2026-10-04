package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.model.*
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Two users with their own sessions on one knowledge base. Alice's commit is
 * checked against what Bob committed in the meantime.
 * See documentation/design/concurrent_users_revalidation.md.
 */
class StaleRuleSessionTest {
    private lateinit var kb: KB
    private lateinit var alice: RuleSessionManager
    private lateinit var bob: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager
    private val aliceId = UserId("alice")
    private val bobId = UserId("bob")

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "TestKB")))
        webSocketManager = mockk()
        coEvery { webSocketManager.sendCasesInfo(any()) } just runs
        coEvery { webSocketManager.sendRuleSessionCompleted(any()) } just runs
        val session = KBSession(kb, webSocketManager)
        alice = session.ruleSessionManagerFor(aliceId)
        bob = session.ruleSessionManagerFor(bobId)
    }

    private fun glucose(): Attribute = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(name: String, value: String = "1.0"): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(glucose(), defaultDate, value)
        return builder.build(name).also { kb.interpret(it) }
    }

    @Test
    fun `alice's addition is stale when bob added the same comment meanwhile`() {
        // Given
        val case = createCase("Case1")
        alice.startRuleSessionToAddComment(case, "Go to Bondi.")
        bob.startRuleSessionToAddComment(createCase("Case2"), "Go to Bondi.")
        bob.commitCurrentRuleSession()
        val rulesBefore = kb.ruleTree.rules().size

        // When
        val stale = shouldThrow<StaleRuleSessionException> { alice.commitCurrentRuleSession() }

        // Then
        stale.message shouldBe interpretationChangedMessage("Case1")
        alice.isRuleSessionActive() shouldBe false
        kb.ruleTree.rules() shouldHaveSize rulesBefore
        coVerify(exactly = 1) { webSocketManager.sendRuleSessionCompleted(aliceId) }
    }

    @Test
    fun `alice's removal is stale when bob removed the comment meanwhile`() {
        // Given
        val case = createCase("Case1")
        alice.startRuleSessionToAddComment(case, "Go to Bondi.")
        alice.commitCurrentRuleSession()
        alice.startRuleSessionToRemoveComment(case, "Go to Bondi.")
        bob.startRuleSessionToRemoveComment(createCase("Case2"), "Go to Bondi.")
        bob.commitCurrentRuleSession()
        val rulesBefore = kb.ruleTree.rules().size

        // When
        val stale = shouldThrow<StaleRuleSessionException> { alice.commitCurrentRuleSession() }

        // Then
        stale.message shouldBe interpretationChangedMessage("Case1")
        alice.isRuleSessionActive() shouldBe false
        kb.ruleTree.rules() shouldHaveSize rulesBefore
        coVerify(exactly = 1) { webSocketManager.sendRuleSessionCompleted(aliceId) }
    }

    @Test
    fun `alice's commit is unaffected by an unrelated rule bob committed meanwhile, even one that changes her case`() {
        // Given
        val case = createCase("Case1")
        alice.startRuleSessionToAddComment(case, "Go to Bondi.")
        alice.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose(), 1.5))
        bob.startRuleSessionToAddComment(createCase("Case2", "2.0"), "Go to Manly.")
        bob.commitCurrentRuleSession()

        // When
        alice.commitCurrentRuleSession()

        // Then
        alice.isRuleSessionActive() shouldBe false
        kb.commentsFor(case) shouldBe setOf("Go to Bondi.", "Go to Manly.")
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }

    @Test
    fun `alice's commit is stale when bob's commit made a cornerstone she never saw conflict with her rule`() {
        // Given
        val case = createCase("Case1")
        alice.startRuleSessionToAddComment(case, "Go to Bondi.")
        alice.conflictingCasesInCurrentRuleSession() shouldHaveSize 0
        bob.startRuleSessionToAddComment(createCase("Case2", "2.0"), "Go to Manly.")
        bob.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 2.0))
        bob.commitCurrentRuleSession()
        val rulesBefore = kb.ruleTree.rules().size

        // When
        val stale = shouldThrow<StaleRuleSessionException> { alice.commitCurrentRuleSession() }

        // Then
        stale.message shouldBe cornerstonesChangedMessage()
        alice.isRuleSessionActive() shouldBe false
        kb.ruleTree.rules() shouldHaveSize rulesBefore
        coVerify(exactly = 1) { webSocketManager.sendRuleSessionCompleted(aliceId) }
    }

    @Test
    fun `a cornerstone alice exempted does not make her commit stale`() {
        // Given
        kb.addCornerstoneCaseIfNoEquivalentAlreadyPresent(createCase("Case2"))
        val case = createCase("Case1")
        alice.startRuleSessionToAddComment(case, "Go to Bondi.")
        alice.conflictingCasesInCurrentRuleSession().map { it.name } shouldBe listOf("Case2")
        alice.exemptCornerstone(0)
        bob.startRuleSessionToAddComment(createCase("Case3", "3.0"), "Go to Manly.")
        bob.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 3.0))
        bob.commitCurrentRuleSession()
        alice.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose(), 1.5))

        // When
        alice.commitCurrentRuleSession()

        // Then
        kb.commentsFor(case) shouldBe setOf("Go to Bondi.")
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }

    @Test
    fun `a cornerstone that stopped conflicting because of bob's rule does not make alice's commit stale`() {
        // Given
        kb.addCornerstoneCaseIfNoEquivalentAlreadyPresent(createCase("Case2", "2.0"))
        val case = createCase("Case1")
        alice.startRuleSessionToAddComment(case, "Go to Bondi.")
        alice.conflictingCasesInCurrentRuleSession().map { it.name } shouldBe listOf("Case2")
        bob.startRuleSessionToAddComment(createCase("Case3", "2.0"), "Go to Bondi.")
        bob.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 2.0))
        bob.commitCurrentRuleSession()

        // When
        alice.commitCurrentRuleSession()

        // Then
        kb.commentsFor(case) shouldBe setOf("Go to Bondi.")
        coVerify(exactly = 0) { webSocketManager.sendRuleSessionCompleted(any()) }
    }
}
