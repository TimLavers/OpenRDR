package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import kotlin.test.Test

internal class RuleTest : RuleTestBase() {
    private val comment1 = comment("First comment")
    private val comment2 = comment("Second comment")
    private val comment3 = comment("Third comment")

    /**
     * The comment text of an assignment, which is how the knowledge base
     * describes a comment rule to the user.
     */
    private val describe: (AssignValue) -> String = { (it.expression as CommentTemplate).text }

    @Test
    fun `action summary for root rule`() {
        val rule = Rule(100, null)
        rule.actionSummary() shouldBe ""
    }

    @Test
    fun `action summary for a rule that adds a comment`() {
        val rule = Rule(100, null, setOf(createCondition("a")))
        val childRule = Rule(200, null, setOf(createCondition("b")), mutableSetOf(), comment2)
        rule.addChild(childRule)
        childRule.actionSummary(describe) shouldBe "$RULE_TO_ADD_COMMENT\nSecond comment"
    }

    @Test
    fun `action summary for a rule that removes a comment`() {
        val rule = Rule(100, null, setOf(createCondition("a")), mutableSetOf(), comment1)
        val childRule = Rule(200, null, setOf(createCondition("b")))
        rule.addChild(childRule)
        childRule.actionSummary(describe) shouldBe "$RULE_TO_REMOVE_COMMENT\nFirst comment"
    }

    @Test
    fun `action summary for a rule that replaces a comment`() {
        val rule = Rule(100, null, setOf(createCondition("a")), mutableSetOf(), comment1)
        val childRule = Rule(200, null, setOf(createCondition("b")), mutableSetOf(), comment2)
        rule.addChild(childRule)
        val expected = """
            $RULE_TO_REPLACE_COMMENT
            First comment
            $WITH
            Second comment
        """.trimIndent()
        childRule.actionSummary(describe) shouldBe expected
    }

    @Test
    fun `action summary describes an assignment to a derived attribute as a value`() {
        //Given a rule assigning a derived attribute
        val bmi = Attribute(1, "BMI", AttributeKind.DERIVED)
        val assignment = AssignValue(bmi, Literal("30.2"))
        val root = Rule(90, null)
        val rule = Rule(100, null, setOf(createCondition("a")), mutableSetOf(), assignment)
        root.addChild(rule)

        //Then the value wording is used, not the comment wording
        rule.actionSummary() shouldBe "$RULE_TO_ASSIGN_VALUE\nBMI = \"30.2\""
    }

    @Test
    fun `action summary uses the assignment text by default`() {
        val rule = Rule(100, null, setOf(createCondition("a")))
        val childRule = Rule(200, null, setOf(createCondition("b")), mutableSetOf(), comment2)
        rule.addChild(childRule)
        childRule.actionSummary() shouldBe "$RULE_TO_ADD_COMMENT\n${comment2.asText()}"
    }

    @Test
    fun `action summary for a stopping rule under the root is empty`() {
        val root = Rule(90, null)
        val stopping = Rule(100, null, setOf(createCondition("a")))
        root.addChild(stopping)
        stopping.actionSummary() shouldBe ""
    }

    @Test
    fun `adding a child in the constructor should set the parent`() {
        val child = Rule(10, null, setOf(), mutableSetOf(), comment2)
        val rule = Rule(1, null, setOf(), mutableSetOf(child), comment1)
        child.parent shouldBe rule
    }

    @Test
    fun `adding a child should set the parent`() {
        val child = Rule(10, null, setOf(), mutableSetOf(), comment2)
        val rule = Rule(1, null, setOf(), mutableSetOf(), comment1)
        rule.addChild(child)
        child.parent shouldBe rule
    }

    @Test
    fun `remove a child leaf rule`() {
        val root = Rule(10, null)
        val rule12 = Rule(12, root, setOf(), mutableSetOf(), comment1)
        root.addChild(rule12)
        val rule13 = Rule(13, root, setOf(), mutableSetOf(), comment2)
        rule12.addChild(rule13)
        val rule14 = Rule(14, root, setOf(), mutableSetOf(), comment2)
        rule12.addChild(rule14)

        shouldThrow<Exception> {
            root.removeChildLeafRule(rule12)
        }.message shouldBe "Only a leaf rule can be removed."

        shouldThrow<Exception> {
            root.removeChildLeafRule(rule13)
        }.message shouldBe "Leaf rule is not a child of this rule."

        rule12.childRules() shouldBe listOf(rule13, rule14)
        rule12.removeChildLeafRule(rule14)
        rule12.childRules() shouldBe listOf(rule13)
        rule14.parent shouldBe null
    }

    @Test
    fun `should be structurally equal if same conditions assignment and parent even if different children`() {
        val child1 = Rule(11, null, setOf(), mutableSetOf(), comment2)
        val child2 = Rule(12, null, setOf(), mutableSetOf(), comment2)
        val rule1 = Rule(1, null, setOf(), mutableSetOf(child1), comment1)
        val rule2 = Rule(2, null, setOf(), mutableSetOf(child2), comment1)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe true
    }

    @Test
    fun `should be structurally equal if identical`() {
        val rule1 = Rule(1, null, setOf(), mutableSetOf(), comment1)
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun `should be structurally equal if identical and no assignment`() {
        val rule1 = Rule(2, null, setOf())
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun `should not be structurally equal if different conditions`() {
        val rule1 = Rule(1, null, setOf(createCondition("a")), mutableSetOf(), comment1)
        val rule2 = Rule(1, null, setOf(), mutableSetOf(), comment1)
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun `should not be structurally equal to a root rule`() {
        val root = Rule(0, null, setOf())
        val rule = Rule(1, root, setOf(createCondition("a")), mutableSetOf(), comment1)
        root shouldNotBe rule
        rule.structurallyEqual(root) shouldBe false
        rule shouldNotBe root
        root.structurallyEqual(rule) shouldBe false
    }

    @Test
    fun `should not be structurally equal if different assignment`() {
        val rule1 = Rule(1, null, setOf(), mutableSetOf(), comment1)
        val rule2 = Rule(2, null, setOf(), mutableSetOf(), comment2)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun `should not be structurally equal if different parents`() {
        val parent1 = Rule(1, null, setOf(), mutableSetOf(), comment1)
        val parent2 = Rule(2, null, setOf(), mutableSetOf(), comment2)
        val rule1 = Rule(11, null, setOf(), mutableSetOf(), comment1)
        val rule2 = Rule(12, null, setOf(), mutableSetOf(), comment1)
        parent1.addChild(rule1)
        parent2.addChild(rule2)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun `conditions are satisfied if empty`() {
        val rule = Rule(1, null, setOf(), mutableSetOf(), comment1)
        rule.conditionsSatisfied(glucoseOnlyCase()) shouldBe true
    }

    @Test
    fun `single condition which is true for case`() {
        val rule = Rule(1, null, setOf(createCondition("vark")), mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("aardvark")) shouldBe true
    }

    @Test
    fun `single condition which is false for case`() {
        val rule = Rule(1, null, setOf(createCondition("vark")), mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("aardwolf")) shouldBe false
    }

    @Test
    fun `any condition false means rule does not apply`() {
        val conditions = setOf(createCondition("a"), createCondition("b"), createCondition("c"), createCondition("d"))
        val rule = Rule(1, null, conditions, mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("abc")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("abd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cbd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cba")) shouldBe false
    }

    @Test
    fun `rule applies if all true`() {
        val conditions = setOf(createCondition("a"), createCondition("b"), createCondition("c"), createCondition("d"))
        val rule = Rule(1, null, conditions, mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("abcd")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("bcda")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("xdcba")) shouldBe true
    }

    @Test
    fun summary() {
        val condA = createCondition("a")
        val condB = createCondition("b")
        val conditions = setOf(condA, condB)
        val rule1 = Rule(1, null, conditions)
        rule1.summary().assignment shouldBe null
        rule1.summary().conditions.size shouldBe 2
        rule1.summary().conditions shouldContain condA
        rule1.summary().conditions shouldContain condB

        val rule2 = Rule(2, null, conditions, mutableSetOf(), comment1)
        rule2.summary().assignment shouldBe comment1
        rule2.summary().conditions.size shouldBe 2
        rule2.summary().conditions shouldContain condA
        rule2.summary().conditions shouldContain condB
    }

    @Test
    fun `summary should contain conditions from root`() {
        val conditions1 = setOf(createCondition("a"), createCondition("b"))
        val rule1 = Rule(1, null, conditions1)
        rule1.summary().conditionTextsFromRoot shouldBe listOf(
            createCondition("a"),
            createCondition("b")
        ).map { it.asText() }

        val conditions2 = setOf(createCondition("x"), createCondition("y"))
        val rule2 = Rule(2, rule1, conditions2, mutableSetOf(), comment2)
        rule2.summary().conditionTextsFromRoot shouldBe listOf(
            createCondition("a"),
            createCondition("b"),
            createCondition("x"),
            createCondition("y")
        ).map {
            it.asText()
        }
    }

    @Test
    fun `rule with no children that applies to case`() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conditions, mutableSetOf(), comment1)
        val kase = clinicalNotesCase("ab")

        val result = rule.apply(kase, interpretation)
        result shouldBe true
        checkInterpretation(comment1)
    }

    @Test
    fun `rule that does not apply to case and has no children`() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conditions, mutableSetOf(), comment1)

        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun `rule applies to case but child does not`() {
        val rule = setupRuleWithOneChild()
        val result = rule.apply(clinicalNotesCase("ac"), interpretation)
        result shouldBe true
        checkInterpretation(comment1)
    }

    @Test
    fun `rule applies to case and so does child`() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conditions, mutableSetOf(), comment1)
        val childConditions = setOf(createCondition("b"))
        val childRule = Rule(3, null, childConditions, mutableSetOf(), comment2)
        rule.addChild(childRule)

        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        checkInterpretation(comment2)
    }

    @Test
    fun `rule does not apply to case but child does`() {
        val rule = setupRuleWithOneChild()

        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun `rule does not apply to case nor does child`() {
        val rule = setupRuleWithOneChild()

        val result = rule.apply(clinicalNotesCase("xy"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun `rule applies no child does`() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("a"), interpretation)
        result shouldBe true
        checkInterpretation(comment1)
    }

    @Test
    fun `rule applies and one child does`() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        checkInterpretation(comment2)
    }

    @Test
    fun `rule applies and so do both children`() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("abc"), interpretation)
        result shouldBe true
        checkInterpretation(comment2, comment3)
    }

    @Test
    fun addRuleTest() {
        val grandChildConditions = setOf(createCondition("a"), createCondition("c"))
        val grandChild = Rule(12, null, grandChildConditions, mutableSetOf(), comment3)
        val childConditions = setOf(createCondition("b"))
        val childRule = Rule(13, null, childConditions, mutableSetOf(), comment2)
        childRule.addChild(grandChild)
        childRule.conditions shouldBe childConditions
        childRule.assignment shouldBe comment2
        val rootConditions = setOf(createCondition("a"), createCondition("b"))
        val root = Rule(45, null, rootConditions, mutableSetOf(), comment1)
        root.addChild(childRule)
        root.assignment shouldBe comment1
        root.conditions shouldBe rootConditions
        root.childRules() shouldContain (childRule)
        val result = root.apply(clinicalNotesCase("abc"), interpretation)
        result shouldBe true
        checkInterpretation(comment3)
    }

    @Test
    fun visitTest() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conditions, mutableSetOf(), comment1)
        val visited = mutableSetOf<Rule>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it)
        }
        rule.visit(action)
        visited.size shouldBe 1
        visited shouldContain (rule)
    }

    @Test
    fun `visit rule with children`() {
        val rule = setupRuleWithTwoChildren()
        val visited = mutableSetOf<AssignValue?>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it.assignment)
        }
        rule.visit(action)
        val expected = mutableSetOf(comment1, comment2, comment3)
        visited shouldBe expected
    }

    @Test
    fun `visit deep`() {
        val rule = setupRuleWithOneChild()
        val grandChildConditions = setOf(createCondition("a"), createCondition("c"))
        val grandChild = Rule(1, null, grandChildConditions, mutableSetOf(), comment3)
        rule.childRules().first().addChild(grandChild)

        val visited = mutableSetOf<AssignValue?>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it.assignment)
        }
        rule.visit(action)
        val expected = mutableSetOf(comment1, comment2, comment3)
        visited shouldBe expected
    }

    @Test
    fun `rule should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.assignment shouldBe rule.assignment
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun `rule with null parent should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.parent shouldBe rule.parent
        copy.assignment shouldBe rule.assignment
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun `rule with not null parent should be copied`() {
        val rule = setupRuleWithOneChild()
        rule.parent = Rule(1, null)
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.parent shouldBe Rule(1, null)
        copy.assignment shouldBe rule.assignment
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun `child rules should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        val copyChild = copy.childRules().iterator().next()
        val ruleChild = rule.childRules().iterator().next()
        copyChild shouldBe ruleChild
        (copyChild !== ruleChild) shouldBe true
    }

    @Test
    fun `conditions should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        val copyCondition = copy.conditions.iterator().next()
        val ruleCondition = rule.conditions.iterator().next()
        copyCondition shouldBe ruleCondition
    }

    @Test
    fun `should list conditions for rule with null parent`() {
        val child = Rule(99, null, setOf(createCondition("a"), createCondition("b")), mutableSetOf(), comment1)
        child.conditionTextsFromRoot() shouldBe listOf(createCondition("a"), createCondition("b")).map { it.asText() }
    }

    @Test
    fun `should list conditions for rule with not null parent`() {
        val parent =
            Rule(
                23,
                null,
                setOf(createCondition("x"), createCondition("y"), createCondition("z")),
                mutableSetOf(),
                comment1
            )
        val child =
            Rule(
                24,
                parent,
                setOf(createCondition("a"), createCondition("b"), createCondition("c")),
                mutableSetOf(),
                comment2
            )
        child.conditionTextsFromRoot() shouldBe listOf(
            createCondition("x"),
            createCondition("y"),
            createCondition("z"),
            createCondition("a"),
            createCondition("b"),
            createCondition("c")
        ).map { it.asText() }

    }

    private fun setupRuleWithTwoChildren(): Rule {
        val rule = setupRuleWithOneChild()
        val childConditions = setOf(createCondition("c"))
        val childRule = Rule(12, null, childConditions, mutableSetOf(), comment3)
        rule.addChild(childRule)
        return rule
    }

    private fun setupRuleWithOneChild(): Rule {
        val rule = Rule(100, null, setOf(createCondition("a")), mutableSetOf(), comment1)
        val childRule = Rule(200, null, setOf(createCondition("b")), mutableSetOf(), comment2)
        rule.addChild(childRule)
        return rule
    }

    private fun checkInterpretation(vararg assignments: AssignValue) {
        checkInterpretation(interpretation, *assignments)
    }
}
