package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.Formula
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class AssignValueDefinitionFlowTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "Blah")))
        rsm = RuleSessionManager(kb, mockk(relaxed = true))
    }

    private fun case() = kb.addProcessedCase(
        with(RDRCaseBuilder()) {
            addValue(kb.attributeManager.getOrCreate("weight"), defaultDate, "93.0")
            addValue(kb.attributeManager.getOrCreate("height"), defaultDate, "1.8")
            build("Bragg")
        }
    )

    private fun assignBmiAndCommit(case: RDRCase) {
        rsm.startRuleSessionToAssignValue(case, "BMI", "weight / (height * height)")
        rsm.commitCurrentRuleSession()
    }

    @Test
    fun `assigning a value stores the definition and builds a by-definition rule`() {
        // When a value is assigned through the normal flow
        val case = case()
        assignBmiAndCommit(case)

        // Then the definition is on the attribute, and the rule points at it
        val bmi = kb.attributeManager.byName("BMI")!!
        kb.derivedDefinitionManager.definitionFor(bmi.id)!!.asText() shouldBe "weight / (height * height)"
        val rule = kb.ruleTree.rulesMatching { it.assignment?.attribute == bmi }.single()
        rule.assignment!!.expression shouldBe ByDefinition

        // And the value evaluates via the definition
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "28.7"
    }

    @Test
    fun `editing the definition after assignment changes the value with no rule change`() {
        // Given an assigned value
        val case = case()
        assignBmiAndCommit(case)
        val ruleTreeSizeBefore = kb.ruleTree.size()

        // When the definition is edited
        rsm.editDerivedAttributeDefinition("BMI", "weight * 2")

        // Then the value reflects the edit and no rule has changed
        val bmi = kb.attributeManager.byName("BMI")!!
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "186"
        kb.ruleTree.size() shouldBe ruleTreeSizeBefore
    }

    @Test
    fun `a self-referencing expression is refused before anything is stored`() {
        // When a self-referencing value is assigned
        // Then it is refused, and no definition has been stored
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAssignValue(case(), "BMI", "BMI * 2")
        }.message shouldBe "This value cannot be assigned: it would make \"BMI\" depend on itself (BMI → BMI)."
        val bmi = kb.attributeManager.byName("BMI")!!
        kb.derivedDefinitionManager.definitionFor(bmi.id).shouldBeNull()
        rsm.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `replacing a value builds a concrete override and leaves the definition untouched`() {
        // Given an assigned value
        val case = case()
        assignBmiAndCommit(case)
        val bmi = kb.attributeManager.byName("BMI")!!

        // When the value is replaced through a rule session
        rsm.startRuleSessionToReplaceAssignment(case, "BMI", "weight * 2")
        rsm.commitCurrentRuleSession()

        // Then the override rule carries the concrete expression
        val overrideRule = kb.ruleTree
            .rulesMatching { it.assignment?.attribute == bmi && it.parent?.assignment != null }
            .single()
        overrideRule.assignment!!.expression shouldBe rsm.valueExpressionFor("weight * 2")
        (overrideRule.assignment!!.expression is Formula) shouldBe true

        // And the stored definition is unchanged
        kb.derivedDefinitionManager.definitionFor(bmi.id)!!.asText() shouldBe "weight / (height * height)"

        // And the override wins for the case
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "186"
    }
}
