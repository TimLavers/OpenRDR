package io.rippledown.kb

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DerivedValueAddition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * [RuleSessionManager.pendingChange], the change the session in progress is
 * about to make as it should be shown *now*. The name of the comment attribute
 * a diff concerns is read from the attribute rather than snapshotted when the
 * session started, because the user can rename a comment while its rule is
 * being built. See
 * documentation/design/previewing_pending_changes_when_a_rule_is_being_built.md.
 */
class RuleSessionManagerPendingChangeTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager

    @BeforeTest
    fun setup() {
        webSocketManager = mockk()
        kb = KB(InMemoryKB(KBInfo("id123", "TestKB")))
        rsm = RuleSessionManager(kb, webSocketManager)
    }

    private val case: RDRCase by lazy {
        kb.addProcessedCase(
            with(RDRCaseBuilder()) {
                addValue(kb.attributeManager.getOrCreate("Glucose"), defaultDate, "12.0")
                build("Bragg")
            }
        )
    }

    /** The case with its comments as they stand now, which a session is started on. */
    private fun currentCase() = kb.viewableCase(case).case

    @Test
    fun `there is no pending change when no session is in progress`() {
        rsm.pendingChange.shouldBeNull()
        rsm.currentDiff.shouldBeNull()
    }

    @Test
    fun `the pending change of a comment addition names the comment attribute`() {
        // When a session to add a comment is started
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")

        // Then the pending change is the addition, named
        rsm.pendingChange shouldBe Addition("Surf's up.", "Surf")
    }

    @Test
    fun `a comment attribute renamed during its session is renamed in the pending change`() {
        // Given a session to add a comment
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")

        // When the comment is renamed
        rsm.renameAttribute("Surf", "Beach")

        // Then the pending change shows the new name
        rsm.pendingChange shouldBe Addition("Surf's up.", "Beach")
    }

    @Test
    fun `the pending change of a removal follows a rename of the comment being removed`() {
        // Given a comment given to the case by a rule, and a session to remove it
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")
        rsm.commitCurrentRuleSession()
        rsm.startRuleSessionToRemoveComment(currentCase(), "Surf's up.")
        rsm.pendingChange shouldBe Removal("Surf's up.", "Surf")

        // When the comment is renamed
        rsm.renameAttribute("Surf", "Beach")

        // Then the pending change shows the new name
        rsm.pendingChange shouldBe Removal("Surf's up.", "Beach")
    }

    @Test
    fun `the pending change of a replacement follows a rename of the replacing comment`() {
        // Given a comment given to the case by a rule, and a session to replace it
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")
        rsm.commitCurrentRuleSession()
        rsm.startRuleSessionToReplaceComment(
            currentCase(),
            "Surf's up.",
            "Surf's flat.",
            proposedAttributeName = "Flat"
        )
        // The name a replacement carries is that of the replacing attribute
        rsm.pendingChange shouldBe Replacement("Surf's up.", "Surf's flat.", "Flat")

        // When the replacing comment is renamed
        rsm.renameAttribute("Flat", "Lake")

        // Then the pending change shows the new name
        rsm.pendingChange shouldBe Replacement("Surf's up.", "Surf's flat.", "Lake")
    }

    @Test
    fun `renaming the comment being replaced leaves the pending change's name alone`() {
        // Given a session to replace a comment
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")
        rsm.commitCurrentRuleSession()
        rsm.startRuleSessionToReplaceComment(
            currentCase(),
            "Surf's up.",
            "Surf's flat.",
            proposedAttributeName = "Flat"
        )

        // When the comment being replaced is renamed
        rsm.renameAttribute("Surf", "Beach")

        // Then the pending change still names the replacing attribute
        rsm.pendingChange shouldBe Replacement("Surf's up.", "Surf's flat.", "Flat")
    }

    /**
     * A derived-value change names the attribute being assigned, which the user
     * can rename too, but it is not a comment attribute, so the diff's own name
     * is left as it is: the Derived attributes panel matches its row by that
     * name and the case it is shown against carries the same name.
     */
    @Test
    fun `a derived value change is passed through unchanged`() {
        // When a session to assign a derived value is started
        rsm.startRuleSessionToAssignValue(case, "Diabetes status", "\"diabetic\"")

        // Then the pending change is that change
        rsm.pendingChange shouldBe DerivedValueAddition(attributeName = "Diabetes status", formula = "\"diabetic\"")
        rsm.currentDiff.shouldBeNull()
        rsm.currentDerivedValueChange shouldBe
                DerivedValueAddition(attributeName = "Diabetes status", formula = "\"diabetic\"")
    }

    @Test
    fun `the cornerstone status carries the pending change as it is named now`() {
        // Given a session to add a comment, renamed since it started
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")
        rsm.renameAttribute("Surf", "Beach")

        // Then the status the client is sent shows the new name
        rsm.cornerstoneStatus().commentDiff shouldBe Addition("Surf's up.", "Beach")
    }

    @Test
    fun `committing the session leaves no pending change`() {
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")

        rsm.commitCurrentRuleSession()

        rsm.pendingChange.shouldBeNull()
    }

    @Test
    fun `cancelling the session leaves no pending change`() {
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")

        rsm.cancelRuleSession()

        rsm.pendingChange.shouldBeNull()
    }

    /**
     * The attribute a diff's name is read from must not outlive its session,
     * or a later derived-value change would be given a comment's name.
     */
    @Test
    fun `a comment session does not name the pending change of the session after it`() {
        // Given a comment session that has been committed
        rsm.startRuleSessionToAddComment(case, "Surf's up.", proposedAttributeName = "Surf")
        rsm.commitCurrentRuleSession()

        // When a derived-value session is started
        rsm.startRuleSessionToAssignValue(case, "Diabetes status", "\"diabetic\"")

        // Then its change is untouched
        rsm.pendingChange shouldBe DerivedValueAddition(attributeName = "Diabetes status", formula = "\"diabetic\"")
    }
}
