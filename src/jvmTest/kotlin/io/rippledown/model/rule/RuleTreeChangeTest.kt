package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.Test

internal class ActionTest : RuleTestBase() {
    private val A = "A"
    private val B = "B"
    private val C = "C"

    @Test
    fun an_AddAction_should_add_a_rule_under_be_the_root_rule() {
        val tree = ruleTree {
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
        val addAction = ChangeTreeToAddConclusion(Conclusion(A))
        addAction.updateRuleTree(tree, glucoseOnlyCase())
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
    fun A_RemoveAction_should_add_a_Stopping_rule_for_the_conclusion_to_be_removed() {
        val tree = ruleTree {
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

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A))
        removeAction.updateRuleTree(tree, clinicalNotesCase("a"))

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
        val tree = ruleTree {
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

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A))
        removeAction.updateRuleTree(tree, clinicalNotesCase("ab"))

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
        val tree = ruleTree {
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

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A))
        removeAction.updateRuleTree(tree, clinicalNotesCase("b"))

        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBe emptySet()
        ruleAdded.conclusion shouldBe null
        ruleAdded.parent!!.conclusion!!.text shouldBe A
        ruleAdded.parent!!.conditions shouldContain ContainsText(clinicalNotes, "b")
    }

    @Test
    fun a_ReplaceAction_should_add_a_rule_under_the_rule_giving_the_conclusion_to_be_replaced() {
        val tree = ruleTree {
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

        val replaceAction = ChangeTreeToReplaceConclusion(Conclusion(A), Conclusion(C))
        replaceAction.updateRuleTree(tree, clinicalNotesCase("a"))

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBe emptySet()
        ruleAdded.conclusion!!.text shouldBe C
        ruleAdded.parent!!.conclusion!!.text shouldBe A
        ruleAdded.parent!!.conditions shouldContain ContainsText(clinicalNotes, "a")
    }

    @Test
    fun a_ReplaceAction_should_add_a_rule_under_each_rule_giving_the_conclusion_to_be_replaced() {
        val tree = ruleTree {
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

        val replaceAction = ChangeTreeToReplaceConclusion(Conclusion(A), Conclusion(C))
        replaceAction.updateRuleTree(tree, clinicalNotesCase("ab"))

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