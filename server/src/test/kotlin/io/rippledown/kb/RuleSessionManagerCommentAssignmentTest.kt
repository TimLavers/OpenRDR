package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class RuleSessionManagerCommentAssignmentTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager

    @BeforeTest
    fun setup() {
        webSocketManager = mockk(relaxed = true)
        kb = KB(InMemoryKB(KBInfo("id123", "Blah")))
        rsm = RuleSessionManager(kb, webSocketManager)
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(name: String, glucoseValue: String = "12.0"): RDRCase =
        with(RDRCaseBuilder()) {
            addValue(glucose(), defaultDate, glucoseValue)
            build(name)
        }

    private fun viewableCase(name: String, glucoseValue: String = "12.0"): ViewableCase =
        kb.viewableCase(kb.addProcessedCase(createCase(name, glucoseValue)))

    private fun commentsShownFor(name: String, glucoseValue: String = "12.0"): List<String> =
        viewableCase(name, glucoseValue).viewableInterpretation.renderedComments.map { it.text }

    private fun buildAddCommentRule(comment: String, variables: List<CommentVariable> = emptyList()) {
        rsm.startRuleSessionToAddComment(viewableCase("Builder"), comment, variables)
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 11.0))
        rsm.commitCurrentRuleSession()
    }

    @Test
    fun `adding a comment creates a comment attribute with the text as its definition`() {
        // When an add-comment session is committed
        buildAddCommentRule("Diabetic diet advice given.")

        // Then a comment attribute holds the text as its definition
        val comment = kb.attributeManager.byName("C1")!!
        comment.kind shouldBe AttributeKind.COMMENT
        kb.derivedDefinitionManager.definitionFor(comment.id) shouldBe
                CommentTemplate("Diabetic diet advice given.")

        // And no conclusion is created
        kb.conclusionManager.all() shouldBe emptySet()

        // And the rule assigns the attribute by definition, for matching cases only
        kb.interpret(createCase("A")).assignments() shouldBe setOf(AssignValue(comment, ByDefinition))
        commentsShownFor("A") shouldBe listOf("Diabetic diet advice given.")
        commentsShownFor("B", "5.0") shouldBe emptyList()
    }

    @Test
    fun `adding the same comment text reuses the comment attribute`() {
        // Given a committed comment rule
        buildAddCommentRule("Diabetic diet advice given.")

        // When another session is started with the same text on a case not yet given it
        rsm.startRuleSessionToAddComment(viewableCase("C", "5.0"), "Diabetic diet advice given.")
        rsm.cancelRuleSession()

        // Then no second comment attribute was created
        kb.attributeManager.commentAttributes().size shouldBe 1
    }

    @Test
    fun `comment variables are carried into the definition and rendered for the case`() {
        // When a comment with a variable is added
        buildAddCommentRule("Glucose is \${} today.", listOf(CommentVariable(glucose().id)))

        // Then the definition carries the variable and the rendering substitutes it
        val comment = kb.attributeManager.byName("C1")!!
        kb.derivedDefinitionManager.definitionFor(comment.id) shouldBe
                CommentTemplate("Glucose is \${} today.", listOf(glucose()))
        commentsShownFor("A") shouldBe listOf("Glucose is 12.0 today.")
    }

    @Test
    fun `removing a comment builds a stopping rule`() {
        // Given a comment rule
        buildAddCommentRule("Diabetic diet advice given.")

        // When a remove-comment session is committed for very high glucose
        rsm.startRuleSessionToRemoveComment(viewableCase("C", "25.0"), "Diabetic diet advice given.")
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 20.0))
        rsm.commitCurrentRuleSession()

        // Then the comment is retracted for such cases only
        commentsShownFor("D", "25.0") shouldBe emptyList()
        commentsShownFor("E") shouldBe listOf("Diabetic diet advice given.")
    }

    @Test
    fun `removing an unknown comment is refused`() {
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToRemoveComment(viewableCase("A"), "No such comment.")
        }.message shouldBe "Cannot remove comment: no comment matching \"No such comment.\" exists."
    }

    @Test
    fun `replacing a comment mints a new attribute for the replacement text`() {
        // Given a comment rule
        buildAddCommentRule("Diabetic diet advice given.")

        // When a replace-comment session is committed for very high glucose
        rsm.startRuleSessionToReplaceComment(
            viewableCase("C", "25.0"), "Diabetic diet advice given.", "Urgent diabetic review required."
        )
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 20.0))
        rsm.commitCurrentRuleSession()

        // Then the replacement has its own attribute with its text as definition
        val replacement = kb.attributeManager.byName("C2")!!
        kb.derivedDefinitionManager.definitionFor(replacement.id) shouldBe
                CommentTemplate("Urgent diabetic review required.")

        // And the replacement applies for such cases only
        commentsShownFor("D", "25.0") shouldBe listOf("Urgent diabetic review required.")
        commentsShownFor("E") shouldBe listOf("Diabetic diet advice given.")
    }

    @Test
    fun `replacing an unknown comment is refused`() {
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToReplaceComment(viewableCase("A"), "No such comment.", "Whatever.")
        }.message shouldBe "Cannot replace comment: no comment matching \"No such comment.\" exists."
    }

    @Test
    fun `a comment attribute is not created if the replaced comment is unknown`() {
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToReplaceComment(viewableCase("A"), "No such comment.", "Whatever.")
        }
        kb.attributeManager.byName("C1").shouldBeNull()
    }
}
