package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class EditDerivedDefinitionTest {
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
        kb.derivedDefinitionManager.store(bmi.id, rsm.valueExpressionFor("weight / height"))
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(bmi, ByDefinition), emptySet())
        return bmi
    }

    @Test
    fun `editing a definition stores the new expression and reports the change`() {
        // Given a derived attribute defined by a by-definition rule
        val bmi = bmiDefinedByRule()
        val case = caseWith("weight" to "93.0", "height" to "1.8")
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "51.67"
        val ruleTreeSizeBefore = kb.ruleTree.size()

        // When the definition is edited
        val summary = rsm.editDerivedAttributeDefinition("BMI", "weight / (height * height)")

        // Then the new definition applies with no rule change, and the change is reported
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "28.7"
        kb.ruleTree.size() shouldBe ruleTreeSizeBefore
        summary shouldBe "Changed the definition of \"BMI\" from weight / height to weight / (height * height)."
    }

    @Test
    fun `defining an attribute with no prior definition is reported as a definition`() {
        // Given a derived attribute with no stored definition
        kb.attributeManager.getOrCreate("Risk score", AttributeKind.DERIVED)

        // When its definition is set
        val summary = rsm.editDerivedAttributeDefinition("Risk score", "\"7\"")

        // Then the definition is stored and reported
        summary shouldBe "Defined \"Risk score\" as \"7\"."
    }

    @Test
    fun `editing the definition of an unknown attribute is refused`() {
        shouldThrow<IllegalStateException> {
            rsm.editDerivedAttributeDefinition("BMI", "weight / (height * height)")
        }.message shouldBe "No attribute with name \"BMI\" exists."
    }

    @Test
    fun `editing the definition of a non-derived attribute is refused`() {
        // Given an externally supplied attribute
        kb.attributeManager.getOrCreate("Glucose")

        // When its definition is edited
        // Then the edit is refused
        shouldThrow<IllegalStateException> {
            rsm.editDerivedAttributeDefinition("Glucose", "\"5.0\"")
        }.message shouldBe "\"Glucose\" is not a derived attribute, so it does not have a definition to edit."
    }

    @Test
    fun `a self-referencing definition is refused`() {
        // Given a derived attribute defined by a by-definition rule
        bmiDefinedByRule()

        // When its definition is edited to reference itself
        // Then the edit is refused with the cycle message
        shouldThrow<IllegalStateException> {
            rsm.editDerivedAttributeDefinition("BMI", "BMI * 2")
        }.message shouldContain "it would make \"BMI\" depend on itself"
    }

    @Test
    fun `a definition creating an indirect cycle is refused`() {
        // Given BMI defined by a rule, and a rule assigning derived attribute
        // "Size" that references BMI in its definition
        bmiDefinedByRule()
        val size = kb.attributeManager.getOrCreate("Size", AttributeKind.DERIVED)
        kb.derivedDefinitionManager.store(size.id, rsm.valueExpressionFor("BMI * 2"))
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(size, ByDefinition), emptySet())

        // When BMI's definition is edited to reference Size
        // Then the edit is refused with the cycle message
        shouldThrow<IllegalStateException> {
            rsm.editDerivedAttributeDefinition("BMI", "Size / 2")
        }.message shouldContain "it would make \"BMI\" depend on itself"
    }

    @Test
    fun `editing a definition is refused while a rule session is active`() {
        // Given an active rule session
        bmiDefinedByRule()
        val case = caseWith("weight" to "93.0", "height" to "1.8")
        rsm.startRuleSessionToAddComment(case, "Whatever.")

        // When a definition edit is attempted
        // Then it is refused
        shouldThrow<IllegalStateException> {
            rsm.editDerivedAttributeDefinition("BMI", "weight / (height * height)")
        }.message shouldBe "Session already in progress."
    }
}
