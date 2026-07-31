package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.rule.*
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBDefinitionResolutionTest {
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "Blah")))
    }

    @Test
    fun `interpretation resolves by-definition assignments against the definition store`() {
        // Given a derived attribute whose definition is stored, and a rule assigning it by definition
        val weight = kb.attributeManager.getOrCreate("weight")
        val height = kb.attributeManager.getOrCreate("height")
        val bmi = kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        val bmiFormula = Formula(
            Binary(
                Operator.DIVIDE,
                AttributeValue(weight),
                Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
            )
        )
        kb.derivedDefinitionManager.store(bmi.id, bmiFormula)
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(bmi, ByDefinition), emptySet())
        val case = kb.addProcessedCase(with(RDRCaseBuilder()) {
            addValue(weight, defaultDate, "93.0")
            addValue(height, defaultDate, "1.8")
            build("Bragg")
        })

        // When the case is viewed
        val viewable = kb.viewableCase(case)

        // Then the definition's value has been assigned
        viewable.case.latestValue(bmi) shouldBe "28.7"
    }

    @Test
    fun `editing the definition changes the value on re-interpretation with no rule change`() {
        // Given a by-definition BMI rule with a stored definition
        val weight = kb.attributeManager.getOrCreate("weight")
        val height = kb.attributeManager.getOrCreate("height")
        val bmi = kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        kb.derivedDefinitionManager.store(
            bmi.id,
            Formula(
                Binary(
                    Operator.DIVIDE,
                    AttributeValue(weight),
                    Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
                )
            )
        )
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(bmi, ByDefinition), emptySet())
        val case = kb.addProcessedCase(with(RDRCaseBuilder()) {
            addValue(weight, defaultDate, "93.0")
            addValue(height, defaultDate, "1.8")
            build("Bragg")
        })
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "28.7"
        val ruleTreeSizeBefore = kb.ruleTree.size()

        // When the definition is edited in place
        kb.derivedDefinitionManager.store(
            bmi.id,
            Formula(Binary(Operator.TIMES, AttributeValue(weight), Num(2.0)))
        )

        // Then re-interpretation gives the new value, and no rule has changed
        kb.viewableCase(case).case.latestValue(bmi) shouldBe "186"
        kb.ruleTree.size() shouldBe ruleTreeSizeBefore
    }
}
