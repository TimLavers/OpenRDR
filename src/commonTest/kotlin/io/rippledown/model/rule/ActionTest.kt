package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ActionTest : RuleTestBase() {
    private val A = "A"
    private val B = "B"
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
        val rulesBefore = tree.root.childRules()
        val addAction = AddAction(Conclusion(A), tree)
        addAction.updateRuleTree(glucoseOnlyCase())
        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.root.childRules().minus(rulesBefore)
        rulesAdded.size shouldBe  1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBe emptySet()
        ruleAdded.conclusion!!.text shouldBe A
        ruleAdded.parent shouldBe tree.root
    }

    @Test
    fun an_AddAction_should_change_the_conclusions_of_an_empty_set_of_conclusions() {
            val addAction = AddAction(Conclusion(A), RuleTree(rootRule()))
            addAction.wouldChangeConclusions(setOf()) shouldBe true
        }

    @Test
    fun an_AddAction_should_change_the_conclusions_of_a_set_of_conclusions_not_containing_the_conclusion_to_be_added() {
            val addAction = AddAction(Conclusion(A), RuleTree())
            addAction.wouldChangeConclusions(setOf(Conclusion(B))) shouldBe true
        }

    @Test
    fun An_AddAction_should_not_change_the_conclusions_of_a_set_of_conclusions_if_the_conclusion_to_be_added_is_already_there() {
            val addAction = AddAction(Conclusion("A"), RuleTree())
            addAction.wouldChangeConclusions(setOf(Conclusion("A"), Conclusion("B"), Conclusion("C"))) shouldBe false
        }
//
//        @Test
//    fun A_RemoveAction_should_add_a_Stopping_rule_for_the_conclusion_to_be_removed() {
//            val tree = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                }
//                child {
//                    conclusion { "ConcB" }
//                    condition { "b" }
//                }
//            }.build()
//
//            val removeAction = RemoveAction(Conc("ConcA"), tree)
//            removeAction.updateRuleTree(Kase("a"))
//
//            val expected = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                    child {
//                        stop()
//                    }
//                }
//                child {
//                    conclusion { "ConcB" }
//                    condition { "b" }
//                }
//            }.build()
//            tree shouldEqual expected
//        }
//
//        a_RemoveAction_should_add_a_Stopping_rule_for_each_instance_of_the_conclusion_to_be_removed"{
//            val tree = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                }
//                child {
//                    conclusion { "ConcA" }
//                    condition { "b" }
//                }
//                child {
//                    conclusion { "ConcC" }
//                    condition { "a" }
//                }
//            }.build()
//
//            val removeAction = RemoveAction(Conc("ConcA"), tree)
//            removeAction.updateRuleTree(Kase("ab"))
//
//            val expected = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                    child {
//                        stop()
//                    }
//                }
//                child {
//                    conclusion { "ConcA" }
//                    condition { "b" }
//                    child {
//                        stop()
//                    }
//                }
//                child {
//                    conclusion { "ConcC" }
//                    condition { "a" }
//                }
//            }.build()
//            tree shouldEqual expected
//        }
//
//@Test
//    fun a_RemoveAction_should_only_add_a_Stopping_rule_for_instances_of_the_conclusion_to_be_removed_that_are_given_by_the_selected_case"{
//            val tree = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                }
//                child {
//                    conclusion { "ConcA" }
//                    condition { "b" }
//                }
//                child {
//                    conclusion { "ConcC" }
//                    condition { "a" }
//                }
//            }.build()
//
//            val removeAction = RemoveAction(Conc("ConcA"), tree)
//            removeAction.updateRuleTree(Kase("b"))
//
//            val expected = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                }
//                child {
//                    conclusion { "ConcA" }
//                    condition { "b" }
//                    child {
//                        stop()
//                    }
//                }
//                child {
//                    conclusion { "ConcC" }
//                    condition { "a" }
//                }
//            }.build()
//            tree shouldEqual expected
//        }
//
//
//        @Test
//    fun A_RemoveAction_should_change_the_conclusions_of_a_set_of_conclusions_containing_the_conclusion_to_be_removed"{
//            val removeAction = RemoveAction(Conc("A"), RuleTree())
//            removeAction.wouldChangeConclusions(setOf(Conc("A"))) shouldBe true
//        }
//
//        @Test
//    fun A_RemoveAction_should_change_the_conclusions_of_a_set_of_conclusions_containing_the_conclusion_to_be_removed"{
//            val removeAction = RemoveAction(Conc("A"), RuleTree())
//            removeAction.wouldChangeConclusions(setOf(Conc("A"), Conc("X"))) shouldBe true
//        }
//
//        @Test
//    fun "A_RemoveAction_should_not_change_the_conclusions_of_a_set_of_conclusions_if_the_conclusion_to_be_removed_is_not_there"{
//            val removeAction = RemoveAction(Conc("A"), RuleTree())
//            removeAction.wouldChangeConclusions(setOf(Conc("X"), Conc("Y"), Conc("Z"))) shouldBe false
//        }
//
//        @Test
//    fun "A_ReplaceAction_should_add_a_rule_under_the_rule_giving_the_conclusion_to_be_replaced"{
//            val tree = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                }
//                child {
//                    conclusion { "ConcB" }
//                    condition { "b" }
//                }
//            }.build()
//
//            val replaceAction = ReplaceAction(Conc("ConcA"), Conc("ConcC"), tree)
//            replaceAction.updateRuleTree(Kase("a"))
//
//            val expected = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                    child {
//                        conclusion { "ConcC" }
//                    }
//                }
//                child {
//                    conclusion { "ConcB" }
//                    condition { "b" }
//                }
//            }.build()
//            tree shouldEqual expected
//        }
//
//        @Test
//    fun "A_ReplaceAction_should_add_a_rule_under_each_rule_giving_the_conclusion_to_be_replaced"{
//            val tree = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                }
//                child {
//                    conclusion { "ConcA" }
//                    condition { "b" }
//                }
//                child {
//                    conclusion { "ConcB" }
//                    condition { "b" }
//                }
//            }.build()
//
//            val replaceAction = ReplaceAction(Conc("ConcA"), Conc("ConcC"), tree)
//            replaceAction.updateRuleTree(Kase("ab"))
//
//            val expected = ruleTree {
//                child {
//                    conclusion { "ConcA" }
//                    condition { "a" }
//                    child {
//                        conclusion { "ConcC" }
//                    }
//                }
//                child {
//                    conclusion { "ConcA" }
//                    condition { "b" }
//                    child {
//                        conclusion { "ConcC" }
//                    }
//                }
//                child {
//                    conclusion { "ConcB" }
//                    condition { "b" }
//                }
//            }.build()
//            tree shouldEqual expected
//        }
//    }
}