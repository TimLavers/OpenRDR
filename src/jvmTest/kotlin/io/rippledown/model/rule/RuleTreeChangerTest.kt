package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.util.shouldContainSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleTreeChangerTest : RuleTestBase() {
    private val A = "A"
    private val B = "B"
    private val C = "C"
    var newRuleId = 1_000_000
    lateinit var ruleMaker: RuleMaker
    private lateinit var conclusionFactory: DummyConclusionFactory


    class RuleMaker(var id: Int): RuleFactory {
        override fun createRuleAndAddToParent(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>): Rule {
            return Rule(id++, parent, conclusion, conditions)
        }
    }

    @BeforeTest
    fun setup() {
        newRuleId = 1_000_000
        ruleMaker = RuleMaker(newRuleId)
        conclusionFactory = DummyConclusionFactory()
    }

    @Test
    fun an_AddAction_should_add_a_rule_under_be_the_root_rule() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +B
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()
        val addAction = AddConclusionRuleTreeChanger(tree, ruleMaker, conclusionFactory.getOrCreate(A))
        addAction.updateRuleTree(glucoseOnlyCase(), emptySet())
        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBe emptySet()
        ruleAdded.conclusion!!.text shouldBe A
        ruleAdded.parent shouldBe tree.root
    }

    @Test
    fun a_RemoveAction_should_add_a_Stopping_rule_for_the_conclusion_to_be_removed() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +B
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val removeAction = RemoveConclusionRuleTreeChanger(tree, ruleMaker, conclusionFactory.getOrCreate(A))
        removeAction.updateRuleTree(clinicalNotesCase("a"), emptySet())

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBe emptySet()
        ruleAdded.conclusion shouldBe null
        ruleAdded.parent!!.conclusion!!.text shouldBe A
    }

    @Test
    fun a_RemoveAction_should_add_a_Stopping_rule_for_each_instance_of_the_conclusion_to_be_removed() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
            child {
                +C
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 3 //sanity
        val rulesBefore = tree.rules()

        val removeAction = RemoveConclusionRuleTreeChanger(tree, ruleMaker, conclusionFactory.getOrCreate(A))
        removeAction.updateRuleTree(clinicalNotesCase("ab"), emptySet())

        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 2
        rulesAdded.forEach {
            it.childRules() shouldBe emptySet()
            it.conditions shouldBe emptySet()
            it.conclusion shouldBe null
            it.parent!!.conclusion!!.text shouldBe A
        }
    }

    @Test
    fun a_RemoveAction_should_only_add_Stopping_rules_for_instances_of_the_conclusion_to_be_removed_that_are_given_by_the_selected_case() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
            child {
                +C
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 3 //sanity
        val rulesBefore = tree.rules()

        val removeAction = RemoveConclusionRuleTreeChanger(tree, ruleMaker, conclusionFactory.getOrCreate(A))
        removeAction.updateRuleTree(clinicalNotesCase("b"), )

        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBe emptySet()
        ruleAdded.conclusion shouldBe null
        ruleAdded.parent!!.conclusion!!.text shouldBe A
        ruleAdded.parent!!.conditions shouldContainSameAs( ContainsText(null, clinicalNotes, "b"))
    }

    @Test
    fun a_ReplaceAction_should_add_a_rule_under_the_rule_giving_the_conclusion_to_be_replaced() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +B
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val replaceAction = ReplaceConclusionRuleTreeChanger(tree, ruleMaker, conclusionFactory.getOrCreate(A), conclusionFactory.getOrCreate(C))
        replaceAction.updateRuleTree(clinicalNotesCase("a"), emptySet())

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBe emptySet()
        ruleAdded.conclusion!!.text shouldBe C
        ruleAdded.parent!!.conclusion!!.text shouldBe A
        ruleAdded.parent!!.conditions shouldContainSameAs ContainsText(null, clinicalNotes, "a")
    }

    @Test
    fun a_ReplaceAction_should_add_a_rule_under_each_rule_giving_the_conclusion_to_be_replaced() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
            child {
                +B
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 3 //sanity
        val rulesBefore = tree.rules()

        val replaceAction = ReplaceConclusionRuleTreeChanger(tree, ruleMaker, conclusionFactory.getOrCreate(A), conclusionFactory.getOrCreate(C))
        replaceAction.updateRuleTree(clinicalNotesCase("ab"), emptySet())

        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 2
        rulesAdded.forEach {
            it.childRules() shouldBe emptySet()
            it.conditions shouldBe emptySet()
            it.conclusion!!.text shouldBe C
            it.parent!!.conclusion!!.text shouldBe A
        }
    }
}