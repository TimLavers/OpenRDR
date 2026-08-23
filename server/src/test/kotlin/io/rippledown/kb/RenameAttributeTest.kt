package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import io.rippledown.utils.serializeDeserialize
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Renaming a comment or derived attribute. See step 14 of
 * documentation/design/repeat_inferencing.md.
 */
class RenameAttributeTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager

    @BeforeTest
    fun setup() {
        webSocketManager = mockk(relaxed = true)
        kb = KB(InMemoryKB(KBInfo("id123", "Blah")))
        rsm = RuleSessionManager(kb, webSocketManager)
    }

    private fun caseWith(vararg attributeToValue: Pair<String, String>) = kb.addProcessedCase(
        with(RDRCaseBuilder()) {
            attributeToValue.forEach { (name, value) ->
                addValue(kb.attributeManager.getOrCreate(name), defaultDate, value)
            }
            build("Bragg")
        }
    )

    private fun bmiDefinedByRule(): Attribute {
        val bmi = kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        kb.derivedDefinitionManager.store(bmi.id, rsm.valueExpressionFor("weight / (height * height)"))
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(bmi, ByDefinition), emptySet())
        return bmi
    }

    @Test
    fun `renaming a derived attribute changes its name and reports the change`() {
        // Given a derived attribute
        val bmi = bmiDefinedByRule()

        // When it is renamed
        val summary = rsm.renameAttribute("BMI", "Body mass index")

        // Then it has the new name, and the change is reported
        bmi.name shouldBe "Body mass index"
        kb.attributeManager.byName("Body mass index") shouldBe bmi
        kb.attributeManager.byName("BMI") shouldBe null
        summary shouldBe "Renamed \"BMI\" to \"Body mass index\"."
    }

    @Test
    fun `a renamed derived attribute keeps its value, under its new name`() {
        // Given a derived attribute with a value for a case
        val bmi = bmiDefinedByRule()
        val case = caseWith("weight" to "93.0", "height" to "1.8")
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "28.7"

        // When it is renamed
        rsm.renameAttribute("BMI", "Body mass index")

        // Then the value is unchanged, and the case shows the new name
        val viewableCase = kb.viewableCase(case)
        viewableCase.case.latestValue(bmi) shouldBe "28.7"
        viewableCase.case.attributes.first { it.id == bmi.id }.name shouldBe "Body mass index"

        // And so does the derived values panel, which is what the user sees
        viewableCase.derivedValues().map { it.name } shouldBe listOf("Body mass index")
        viewableCase.derivedValues().map { it.value } shouldBe listOf("28.7")

        // And the case survives the trip to the client, which shows the panel
        val asSentToClient = serializeDeserialize(viewableCase)
        asSentToClient.derivedValues().map { it.name } shouldBe listOf("Body mass index")
        asSentToClient.derivedValues().map { it.value } shouldBe listOf("28.7")
    }

    @Test
    fun `renaming a comment attribute changes its name`() {
        // Given a comment attribute
        val comment = kb.attributeManager.createCommentAttribute()
        comment.name shouldBe "C1"

        // When it is renamed
        val summary = rsm.renameAttribute("C1", "Diabetes advice")

        // Then it has the new name
        comment.name shouldBe "Diabetes advice"
        summary shouldBe "Renamed \"C1\" to \"Diabetes advice\"."
    }

    @Test
    fun `renaming is allowed while a rule session is in progress`() {
        // Given a rule session in progress
        val case = caseWith("weight" to "93.0", "height" to "1.8")
        rsm.startRuleSessionToAddComment(case, "Overweight.")
        rsm.isRuleSessionActive() shouldBe true

        // And a derived attribute
        bmiDefinedByRule()

        // When it is renamed
        rsm.renameAttribute("BMI", "Body mass index")

        // Then the rename is made, and the session is unaffected
        kb.attributeManager.byName("Body mass index") shouldNotBe null
        kb.attributeManager.byName("BMI") shouldBe null
        rsm.isRuleSessionActive() shouldBe true
    }

    @Test
    fun `renaming the comment being added shows the new name in the pending change`() {
        // Given a session in progress to add a comment, whose pending change is
        // shown under the comment attribute's name
        val case = caseWith("weight" to "93.0")
        rsm.startRuleSessionToAddComment(case, "Overweight.")
        rsm.currentDiff shouldBe Addition("Overweight.", "C1")

        // When the comment is renamed
        rsm.renameAttribute("C1", "Beach")

        // Then the pending change carries the new name
        rsm.currentDiff shouldBe Addition("Overweight.", "Beach")
        rsm.cornerstoneStatus().commentDiff shouldBe Addition("Overweight.", "Beach")
    }

    @Test
    fun `renaming the comment being removed shows the new name in the pending change`() {
        // Given a comment given to a case by a rule
        val case = caseWith("weight" to "93.0")
        rsm.startRuleSessionToAddComment(case, "Overweight.")
        rsm.commitCurrentRuleSession()

        // And a session in progress to remove it
        rsm.startRuleSessionToRemoveComment(kb.viewableCase(case).case, "Overweight.")
        rsm.currentDiff shouldBe Removal("Overweight.", "C1")

        // When the comment is renamed
        rsm.renameAttribute("C1", "Beach")

        // Then the pending change carries the new name
        rsm.currentDiff shouldBe Removal("Overweight.", "Beach")
    }

    @Test
    fun `an external attribute cannot be renamed`() {
        // Given an external attribute
        kb.attributeManager.getOrCreate("Glucose")

        // When it is renamed
        // Then the request is refused
        shouldThrow<IllegalStateException> {
            rsm.renameAttribute("Glucose", "Blood glucose")
        }.message shouldBe "\"Glucose\" is not a comment or a derived attribute, so it cannot be renamed."
    }

    @Test
    fun `renaming an unknown attribute is refused`() {
        shouldThrow<IllegalStateException> {
            rsm.renameAttribute("Whatever", "Something")
        }.message shouldBe "No attribute with name \"Whatever\" exists."
    }

    @Test
    fun `a name already in use is refused`() {
        // Given a derived attribute and an external attribute
        bmiDefinedByRule()
        kb.attributeManager.getOrCreate("Glucose")

        // When the derived attribute is renamed to the external attribute's name, ignoring case
        // Then the request is refused
        shouldThrow<IllegalStateException> {
            rsm.renameAttribute("BMI", "glucose")
        }.message shouldBe "An attribute with name \"Glucose\" already exists. Choose a different name."
    }
}
