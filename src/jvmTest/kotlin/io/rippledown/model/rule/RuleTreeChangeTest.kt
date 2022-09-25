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
    private val notes = clinicalNotes.name

    @Test
    fun an_AddAction_should_add_a_rule_under_be_the_root_rule() {
        val tree = ruleTree {
            child {
                +A
                condition {
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                +B
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()
        val addAction = ChangeTreeToAddConclusion(Conclusion(A), tree)
        addAction.updateRuleTree(glucoseOnlyCase())
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
    fun an_AddAction_should_change_the_conclusions_of_an_empty_set_of_conclusions() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(A), RuleTree(rootRule()))
        addAction.wouldChangeConclusions(setOf()) shouldBe true
    }

    @Test
    fun an_AddAction_should_change_the_conclusions_of_a_set_of_conclusions_not_containing_the_conclusion_to_be_added() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(A), RuleTree())
        addAction.wouldChangeConclusions(setOf(Conclusion(B))) shouldBe true
    }

    @Test
    fun An_AddAction_should_not_change_the_conclusions_of_a_set_of_conclusions_if_the_conclusion_to_be_added_is_already_there() {
        val addAction = ChangeTreeToAddConclusion(Conclusion("A"), RuleTree())
        addAction.wouldChangeConclusions(setOf(Conclusion("A"), Conclusion("B"), Conclusion("C"))) shouldBe false
    }

    @Test
    fun A_RemoveAction_should_add_a_Stopping_rule_for_the_conclusion_to_be_removed() {
        val tree = ruleTree {
            child {
                +A
                condition {
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                +B
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A), tree)
        removeAction.updateRuleTree(clinicalNotesCase("a"))

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
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                +A
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
            child {
                +C
                condition {
                    attributeName = notes
                    constant = "a"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 3 //sanity
        val rulesBefore = tree.rules()

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A), tree)
        removeAction.updateRuleTree(clinicalNotesCase("ab"))

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
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                +A
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
            child {
                +C
                condition {
                    attributeName = notes
                    constant = "a"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 3 //sanity
        val rulesBefore = tree.rules()

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A), tree)
        removeAction.updateRuleTree(clinicalNotesCase("b"))

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
    fun a_RemoveAction_should_change_the_conclusions_of_a_set_of_conclusions_containing_the_conclusion_to_be_removed() {
        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A), RuleTree())
        removeAction.wouldChangeConclusions(setOf(Conclusion(A))) shouldBe true
    }

    @Test
    fun a_RemoveAction_should_change_the_conclusions_of_a_set_of_conclusions_containing_the_conclusion_to_be_removed2() {
        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A), RuleTree())
        removeAction.wouldChangeConclusions(setOf(Conclusion(A), Conclusion("X"))) shouldBe true
    }

    @Test
    fun a_RemoveAction_should_not_change_the_conclusions_of_a_set_of_conclusions_if_the_conclusion_to_be_removed_is_not_there() {
        val removeAction = ChangeTreeToRemoveConclusion(Conclusion(A), RuleTree())
        removeAction.wouldChangeConclusions(setOf(Conclusion("X"), Conclusion("Y"), Conclusion("Z"))) shouldBe false
    }

    @Test
    fun a_ReplaceAction_should_add_a_rule_under_the_rule_giving_the_conclusion_to_be_replaced() {
        val tree = ruleTree {
            child {
                +A
                condition {
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                +B
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val replaceAction = ChangeTreeToReplaceConclusion(Conclusion(A), Conclusion(C), tree)
        replaceAction.updateRuleTree(clinicalNotesCase("a"))

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
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                +A
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
            child {
                +B
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 3 //sanity
        val rulesBefore = tree.rules()

        val replaceAction = ChangeTreeToReplaceConclusion(Conclusion(A), Conclusion(C), tree)
        replaceAction.updateRuleTree(clinicalNotesCase("ab"))

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