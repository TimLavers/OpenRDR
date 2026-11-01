package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.UserId
import io.rippledown.model.change.ChangeKind
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.locks.ReentrantLock

class UndoConfirmationTest {
    private val kb = KB(InMemoryKB(KBInfo("undo", "Undo")))
    private val session = KBSession(kb, mockk())
    private val alice = session.ruleSessionManagerFor(UserId("Alice"))
    private val bob = session.ruleSessionManagerFor(UserId("Bob"))
    private var caseNumber = 0

    private fun commit(manager: RuleSessionManager, comment: String) = session.locked {
        val case = RDRCaseBuilder().apply {
            addValue(kb.attributeManager.getOrCreate("Glucose"), defaultDate, "1")
        }.build("Case ${++caseNumber}")
        kb.interpret(case)
        manager.startRuleSessionToAddComment(case, comment)
        manager.commitCurrentRuleSession()
    }

    @Test
    fun `another user's rule is attributed while the author's preview stays plain`() {
        // Given
        commit(bob, "Bob's advice.")

        // When
        val own = session.locked { bob.descriptionOfMostRecentRule() }
        val other = session.locked { alice.descriptionOfMostRecentRule() }

        // Then
        own.canRemove shouldBe true
        own.description shouldNotContain "added by"
        other.description shouldBe "The most recent rule was added by Bob: ${own.description}"
    }

    @Test
    fun `a commit between preview and confirmation is refused without deleting anything`() {
        // Given
        commit(alice, "Alice's advice.")
        session.locked { alice.descriptionOfMostRecentRule() }
        commit(bob, "Bob's advice.")
        val histories = alice.ruleSessionHistories()
        val changes = kb.changeLog.all()
        val ruleCount = kb.ruleTree.size()

        // When
        val refused = shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        refused.message shouldContain "The most recent rule changed since it was shown."
        refused.message shouldContain "The most recent rule was added by Bob:"
        refused.message shouldContain "Bob's advice."
        refused.message shouldContain "Undo it?"
        alice.ruleSessionHistories() shouldBe histories
        kb.changeLog.all() shouldBe changes
        kb.ruleTree.size() shouldBe ruleCount
    }

    @Test
    fun `undo requires the caller's own preview`() {
        // Given
        commit(bob, "Bob's advice.")
        session.locked { bob.descriptionOfMostRecentRule() }

        // When
        shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        kb.ruleTree.size() shouldBe 2
        kb.changeLog.all().map { it.kind } shouldBe listOf(ChangeKind.RULES_ADDED)
    }

    @Test
    fun `undo and a replacement commit cannot reuse a previewed session identity`() {
        // Given
        commit(bob, "Original advice.")
        session.locked {
            alice.descriptionOfMostRecentRule()
            bob.descriptionOfMostRecentRule()
            bob.undoLastRuleSession()
        }
        commit(bob, "Replacement advice.")

        // When
        val refused = shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        refused.message shouldContain "Replacement advice."
        kb.ruleTree.size() shouldBe 2
        kb.changeLog.all().last().kind shouldBe ChangeKind.RULES_ADDED
    }

    @Test
    fun `a fresh preview after refusal allows undo and records the confirming user`() {
        // Given
        commit(alice, "Alice's advice.")
        session.locked { alice.descriptionOfMostRecentRule() }
        commit(bob, "Bob's advice.")
        shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }
        val bobRecord = kb.changeLog.all().last()

        // When
        session.locked {
            alice.descriptionOfMostRecentRule()
            alice.undoLastRuleSession()
        }

        // Then
        kb.ruleTree.size() shouldBe 2
        alice.ruleSessionHistories().size shouldBe 1
        with(kb.changeLog.all().last()) {
            kind shouldBe ChangeKind.RULES_UNDONE
            userId shouldBe "Alice"
            ruleIds shouldBe bobRecord.ruleIds
            summary shouldBe "Undid: ${bobRecord.summary}"
        }
    }

    @Test
    fun `a successful undo consumes the preview and cannot undo the previous session too`() {
        // Given
        commit(alice, "First advice.")
        commit(alice, "Second advice.")
        session.locked {
            alice.descriptionOfMostRecentRule()
            alice.undoLastRuleSession()
        }
        val changes = kb.changeLog.all()

        // When
        val refused = shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        refused.message shouldBe "Please review the most recent rule before undoing it."
        kb.ruleTree.size() shouldBe 2
        kb.changeLog.all() shouldBe changes
    }

    @Test
    fun `a refusal clears the preview even if the originally shown rule becomes latest again`() {
        // Given
        commit(alice, "Original advice.")
        session.locked { alice.descriptionOfMostRecentRule() }
        commit(bob, "Intervening advice.")
        shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }
        session.locked {
            bob.descriptionOfMostRecentRule()
            bob.undoLastRuleSession()
        }

        // When
        val refused = shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        refused.message shouldBe "Please review the most recent rule before undoing it."
        kb.ruleTree.size() shouldBe 2
    }

    @Test
    fun `another user's undo cannot cause confirmation to remove an older rule`() {
        // Given
        commit(alice, "Older advice.")
        commit(bob, "Latest advice.")
        session.locked {
            alice.descriptionOfMostRecentRule()
            bob.descriptionOfMostRecentRule()
            bob.undoLastRuleSession()
        }

        // When
        val refused = shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        refused.message shouldContain "The most recent rule changed since it was shown."
        refused.message shouldContain "Older advice."
        kb.ruleTree.size() shouldBe 2
    }

    @Test
    fun `a rule removed by another user leaves nothing to confirm`() {
        // Given
        commit(bob, "Advice.")
        session.locked {
            alice.descriptionOfMostRecentRule()
            bob.descriptionOfMostRecentRule()
            bob.undoLastRuleSession()
        }
        val changes = kb.changeLog.all()

        // When
        val refused = shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        refused.message shouldBe "There are no rules to undo."
        kb.ruleTree.size() shouldBe 1
        kb.changeLog.all() shouldBe changes
    }

    @Test
    fun `an empty preview does not authorise undo of a subsequently committed rule`() {
        // Given
        val preview = session.locked { alice.descriptionOfMostRecentRule() }
        preview.canRemove shouldBe false
        preview.description shouldBe "There are no rules to undo."
        commit(bob, "New advice.")

        // When
        shouldThrow<IllegalStateException> { session.locked { alice.undoLastRuleSession() } }

        // Then
        kb.ruleTree.size() shouldBe 2
    }

    @Test
    fun `unrelated changes do not invalidate a reviewed session or supply its author`() {
        // Given
        commit(alice, "Advice.")
        session.locked { alice.descriptionOfMostRecentRule() }
        kb.changeLog.record(UserId("Bob"), ChangeKind.DESCRIPTION_SET, "Changed the description.")

        // When
        session.locked { alice.undoLastRuleSession() }

        // Then
        kb.ruleTree.size() shouldBe 1
        kb.changeLog.all().last().userId shouldBe "Alice"
    }

    @Test
    fun `rules without change history remain reviewable and undoable without invented authors`() {
        // Given a pre-log rule, represented by a manager without an attributed user
        commit(RuleSessionManager(kb), "Historic advice.")

        // When
        val description = session.locked { alice.descriptionOfMostRecentRule() }
        session.locked { alice.undoLastRuleSession() }

        // Then
        description.canRemove shouldBe true
        description.description shouldContain "Historic advice."
        description.description shouldNotContain "added by"
        kb.ruleTree.size() shouldBe 1
    }

    @Test
    fun `a new unlogged rule cannot inherit the author of a deleted rule`() {
        // Given
        commit(bob, "Old advice.")
        session.locked {
            bob.descriptionOfMostRecentRule()
            bob.undoLastRuleSession()
        }
        commit(RuleSessionManager(kb), "Unlogged advice.")

        // When
        val description = session.locked { alice.descriptionOfMostRecentRule() }

        // Then
        description.description shouldContain "Unlogged advice."
        description.description shouldNotContain "added by"
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `preview and undo acquire the KB lock even when called directly`(undo: Boolean) {
        // Given
        val lock = ReentrantLock()
        val lockedKB = mockk<KB>()
        val recorder = mockk<io.rippledown.model.rule.RuleSessionRecorder>()
        every { lockedKB.ruleSessionRecorder } answers {
            lock.isHeldByCurrentThread shouldBe true
            recorder
        }
        every { recorder.idsOfRulesAddedInMostRecentSession() } returns null
        val manager = RuleSessionManager(lockedKB, kbLock = lock)

        // When / Then
        if (undo) shouldThrow<IllegalStateException> { manager.undoLastRuleSession() }
        else manager.descriptionOfMostRecentRule().canRemove shouldBe false
        lock.isLocked shouldBe false
    }
}
