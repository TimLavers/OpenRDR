package io.rippledown.kb

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.condition.isPresent
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.persistence.inmemory.InMemoryKB
import kotlin.test.BeforeTest
import kotlin.test.Test

class CheckDefinitionEditIsAcyclicTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private var nextConditionId = 1000

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "Blah")))
        rsm = RuleSessionManager(kb, mockk(relaxed = true))
        kb.attributeManager.getOrCreate("Glucose")
    }

    private fun derived(name: String) = kb.attributeManager.getOrCreate(name, AttributeKind.DERIVED)

    /** A rule assigning [attribute] by definition, with [definition] stored. */
    private fun definedByRule(attribute: Attribute, definition: String) {
        kb.derivedDefinitionManager.store(attribute.id, rsm.valueExpressionFor(definition))
        kb.ruleManager.createRuleAndAddToParent(kb.ruleTree.root, AssignValue(attribute, ByDefinition), emptySet())
    }

    private fun checkEdit(attribute: Attribute, newDefinition: String) =
        rsm.checkDefinitionEditIsAcyclic(attribute, rsm.valueExpressionFor(newDefinition))

    @Test
    fun `a definition referencing only external attributes is accepted`() {
        val a = derived("A")
        definedByRule(a, "Glucose + 1")
        shouldNotThrowAny { checkEdit(a, "Glucose * 2") }
    }

    @Test
    fun `a literal definition is accepted`() {
        val a = derived("A")
        definedByRule(a, "Glucose + 1")
        shouldNotThrowAny { checkEdit(a, "\"high\"") }
    }

    @Test
    fun `a definition referencing an independent derived attribute is accepted`() {
        // Given B does not depend on A
        val a = derived("A")
        val b = derived("B")
        definedByRule(a, "Glucose + 1")
        definedByRule(b, "Glucose * 2")

        // Then A may reference B
        shouldNotThrowAny { checkEdit(a, "B + 1") }
    }

    @Test
    fun `a direct self-reference is refused`() {
        val a = derived("A")
        definedByRule(a, "Glucose + 1")
        shouldThrow<IllegalStateException> {
            checkEdit(a, "A * 2")
        }.message shouldBe "This definition cannot be used: it would make \"A\" depend on itself (A → A)."
    }

    @Test
    fun `a direct self-reference is refused even when no rule assigns the attribute yet`() {
        // Given a derived attribute with no by-definition rule
        val a = derived("A")

        // Then a self-referencing definition is still refused
        shouldThrow<IllegalStateException> {
            checkEdit(a, "A * 2")
        }.message shouldBe "This definition cannot be used: it would make \"A\" depend on itself (A → A)."
    }

    @Test
    fun `a cycle through another attribute's definition is refused`() {
        // Given B's definition references A
        val a = derived("A")
        val b = derived("B")
        definedByRule(a, "Glucose + 1")
        definedByRule(b, "A * 2")

        // Then A may not reference B
        shouldThrow<IllegalStateException> {
            checkEdit(a, "B / 2")
        }.message shouldBe "This definition cannot be used: it would make \"A\" depend on itself (A → B → A)."
    }

    @Test
    fun `a cycle through a rule condition is refused`() {
        // Given a rule assigning B that is conditioned on A
        val a = derived("A")
        val b = derived("B")
        definedByRule(a, "Glucose + 1")
        kb.derivedDefinitionManager.store(b.id, rsm.valueExpressionFor("Glucose * 2"))
        kb.ruleManager.createRuleAndAddToParent(
            kb.ruleTree.root, AssignValue(b, ByDefinition), setOf(isPresent(a, nextConditionId++))
        )

        // Then A may not reference B
        shouldThrow<IllegalStateException> {
            checkEdit(a, "B + 1")
        }.message shouldBe "This definition cannot be used: it would make \"A\" depend on itself (A → B → A)."
    }

    @Test
    fun `a three-attribute cycle is refused`() {
        // Given B's definition references C, and C's definition references A
        val a = derived("A")
        val b = derived("B")
        val c = derived("C")
        definedByRule(a, "Glucose + 1")
        definedByRule(b, "C * 2")
        definedByRule(c, "A * 2")

        // Then A may not reference B
        shouldThrow<IllegalStateException> {
            checkEdit(a, "B + 1")
        }.message shouldBe "This definition cannot be used: it would make \"A\" depend on itself (A → B → C → A)."
    }

    @Test
    fun `the check uses the edited definition, not the stored one`() {
        // Given A's stored definition references B
        val a = derived("A")
        val b = derived("B")
        definedByRule(a, "B * 2")
        definedByRule(b, "Glucose * 2")

        // When A is edited to no longer reference B, then B is edited to reference A
        rsm.editDerivedAttributeDefinition("A", "Glucose + 1")

        // Then B may reference A, since the A -> B edge is gone
        shouldNotThrowAny { checkEdit(b, "A + 1") }
    }

    @Test
    fun `an edit that keeps an existing cycle-free reference is accepted`() {
        // Given A's stored definition references B, and B is independent
        val a = derived("A")
        val b = derived("B")
        definedByRule(a, "B * 2")
        definedByRule(b, "Glucose * 2")

        // Then A may be edited to a different expression still referencing B
        shouldNotThrowAny { checkEdit(a, "B + 1") }
    }
}
