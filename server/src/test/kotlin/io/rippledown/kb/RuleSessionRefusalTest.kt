package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Replacement
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * A request that is refused must leave the pending-change preview as it found
 * it, so that nothing is left behind for the next session to show.
 */
class RuleSessionRefusalTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "TestKB")))
        webSocketManager = mockk()
        rsm = RuleSessionManager(kb, webSocketManager)
    }

    private fun glucose(): Attribute = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(name: String): RDRCase = with(RDRCaseBuilder()) {
        addValue(glucose(), defaultDate, "12.0")
        build(name)
    }

    private fun commitWithHighGlucose() {
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 11.0))
        rsm.commitCurrentRuleSession()
    }

    @Test
    fun `a comment session that starts shows its pending change`() {
        // Given no session in progress
        val case = createCase("Case")

        // When a comment session starts
        rsm.startRuleSessionToAddComment(case, "Patient is diabetic")

        // Then the change is previewed, naming the comment attribute
        rsm.pendingChange shouldBe Addition("Patient is diabetic", "C1")
        rsm.nameOfCommentAttributeInSession() shouldBe "C1"
    }

    @Test
    fun `a comment session refused because one is in progress leaves the running session's preview alone`() {
        // Given a session in progress to add a comment
        val case = createCase("Case")
        rsm.startRuleSessionToAddComment(case, "First comment")
        val previewOfRunningSession = rsm.pendingChange
        val commentOfRunningSession = rsm.nameOfCommentAttributeInSession()

        // When a second comment session is refused because the first is in progress
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAddComment(case, "Second comment")
        }

        // Then the running session's preview is untouched: the refusal concerns the
        // request that was turned away, not the session that turned it away
        rsm.pendingChange shouldBe previewOfRunningSession
        rsm.nameOfCommentAttributeInSession() shouldBe commentOfRunningSession
    }

    @Test
    fun `a refused request leaves both attribute names of a running replacement preview alone`() {
        // Given a session replacing a comment, whose preview identifies both
        // the attribute coming in and the one going out
        val case = createCase("Case")
        rsm.startRuleSessionToAddComment(case, "Patient is diabetic")
        commitWithHighGlucose()
        rsm.startRuleSessionToReplaceComment(case, "Patient is diabetic", "Patient is well")
        rsm.pendingChange shouldBe Replacement("Patient is diabetic", "Patient is well", "C2", "C1")

        // When another comment session is refused because the replacement is in progress
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAddComment(case, "A second request")
        }

        // Then the running replacement can still identify the row it previews
        rsm.pendingChange shouldBe Replacement("Patient is diabetic", "Patient is well", "C2", "C1")
    }

    @Test
    fun `an addition refused as inapplicable leaves no preview behind`() {
        // Given a committed rule giving the case a comment
        val case = createCase("Case")
        rsm.startRuleSessionToAddComment(case, "Already given")
        commitWithHighGlucose()

        // When the same comment is requested again for the same case
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAddComment(case, "Already given")
        }

        // Then nothing is left for the next session to show
        rsm.currentChange.shouldBeNull()
        rsm.pendingChange.shouldBeNull()
        rsm.nameOfCommentAttributeInSession().shouldBeNull()
    }

    @Test
    fun `a removal refused as inapplicable leaves no preview behind`() {
        // Given a comment given for one case but not for another
        rsm.startRuleSessionToAddComment(createCase("Diabetic"), "Patient is diabetic")
        commitWithHighGlucose()
        val caseWithoutTheComment = with(RDRCaseBuilder()) {
            addValue(glucose(), defaultDate, "5.0")
            build("Healthy")
        }

        // When the comment is asked to be removed from the case that was not given it
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToRemoveComment(caseWithoutTheComment, "Patient is diabetic")
        }

        // Then nothing is left for the next session to show
        rsm.currentChange.shouldBeNull()
        rsm.pendingChange.shouldBeNull()
    }
}
