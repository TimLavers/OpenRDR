package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

class
DummyRuleFactory: RuleFactory {
    override fun createRuleAndAddToParent(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>): Rule {
        return Rule(0, parent, conclusion, conditions)
    }
}

internal class RuleTreeTest : RuleTestBase() {
    private lateinit var tree: RuleTree
    private val A = "A"
    private val B = "B"
    private val kase = clinicalNotesCase("abc")
    private lateinit var conclusionFactory: DummyConclusionFactory
    private lateinit var conditionFactory: DummyConditionFactory

    @BeforeTest
    fun init() {
        tree = RuleTree()
        conclusionFactory = DummyConclusionFactory()
        conditionFactory = DummyConditionFactory()
    }

    @Test
    fun size_of_a_tree_with_root_only() {
        tree.size() shouldBe 1L
    }

    @Test
    fun should_be_one_rule_returned_for_a_tree_with_root_only() {
        tree.rules() shouldBe setOf(rootRule())
    }

    @Test
    fun no_conclusions_should_be_given_for_a_tree_with_only_the_root_rule() {
        tree.apply(kase)
        kase.interpretation.conclusions() shouldBe setOf()
    }

    @Test
    fun root_rule_should_not_apply_to_a_case() {
        tree.apply(kase)
        kase.interpretation shouldBe Interpretation(CaseId(kase.id, kase.name))
    }

    @Test
    fun add_to_empty_root() {
        tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()
        tree.apply(kase)
        val conclusion1 = tree.root.childRules().first().conclusion!!
        checkInterpretation(kase.interpretation, conclusion1)
        conclusion1.text shouldBe A
    }

    @Test
    fun add_to_root_that_has_one_child() {
        tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +B
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
        tree.apply(kase)
        val conclusion1 = tree.root.childRules().first().conclusion!!
        val conclusion2 = tree.root.childRules().last().conclusion!!
        checkInterpretation(kase.interpretation, conclusion1, conclusion2)
    }

    @Test
    fun add_to_two_leaf_rules() {
        tree = ruleTree(conclusionFactory) {
            child {
                + "ConcA"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    +"ConcC"
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "c"
                    }
                }
            }
            child {
                + "ConcB"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
                child {
                    + "ConcC"
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "c"
                    }
                }
            }
        }.build()

        checkInterpretationForCase("a", "ConcA")
        checkInterpretationForCase("b", "ConcB")
        checkInterpretationForCase("c")
        checkInterpretationForCase("abc", "ConcC")
    }

    @Test
    fun add_to_root_with_two_children() {
        tree = ruleTree(conclusionFactory) {
            child {
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcB" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
            child {
                conclusion { "ConcC" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "c"
                }
            }
        }.build()
        checkInterpretationForCase("a", "ConcA")
        checkInterpretationForCase("b", "ConcB")
        checkInterpretationForCase("a", "ConcA")
        checkInterpretationForCase("b", "ConcB")
        checkInterpretationForCase("c", "ConcC")
        checkInterpretationForCase("abc", "ConcA", "ConcB", "ConcC")
    }

    @Test
    fun sizeTest() {
        tree.size() shouldBe 1L
    }

    @Test
    fun size_with_one_child() {
        tree = ruleTree(conclusionFactory) {
            child {
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()
        tree.size() shouldBe 2L
    }

    @Test
    fun size_with_two_children_of_root() {
        tree = ruleTree(conclusionFactory) {
            child {
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
        tree.size() shouldBe 3L
    }

    @Test
    fun size_with_depth_four() {
        tree = ruleTree(conclusionFactory) {
            child {
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    conclusion { "ConcA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        conclusion { "ConcB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    conclusion { "ConcD" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        tree.size() shouldBe 5L
    }

    @Test
    fun rules() {
        tree = ruleTree(conclusionFactory) {
            child {
                id = 1
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 11
                    conclusion { "ConcA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = 111
                        conclusion { "ConcB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = 12
                    conclusion { "ConcD" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        tree.rules().size shouldBe 5
        tree.rules().contains(tree.root) shouldBe true
        tree.rules().map { rule -> rule.id } shouldContainAll listOf(tree.root.id, 1, 11, 111, 12)
    }

    @Test
    fun rulesWithConclusionTest() {
        tree = ruleTree(conclusionFactory) {
            child {
                id = 1
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    conclusion { "ConcA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        conclusion { "ConcB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                        child {
                            conclusion { "ConcA" }
                            condition(conditionFactory) {
                                attribute = clinicalNotes
                                constant = "d"
                            }
                        }
                    }
                }
                child {
                    conclusion { "ConcD" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        val predicate: ((Rule) -> Boolean) = { r ->
            r.conclusion?.text == "ConcA"
        }
        val rulesMatching = tree.rulesMatching(predicate)
        rulesMatching.size shouldBe 3
        rulesMatching.forEach { it.conclusion?.text shouldBe "ConcA" }
    }

    @Test
    fun add_child_under_child_under_root() {
        tree = ruleTree(conclusionFactory) {
            child {
                id = 1
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 2
                    conclusion { "ConcB" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                }
            }
        }.build()
        tree.size() shouldBe 3L
        tree.rules().contains(tree.root) shouldBe true

        tree.rules().map { rule -> rule.id } shouldBe setOf(tree.root.id, 1, 2)
    }

    @Test
    fun copy_Empty_Tree() {
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @Test
    fun copy_root() {
        tree = ruleTree(conclusionFactory) {
        }.build()
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @Test
    fun copy_tree_with_1_child() {
        tree = ruleTree(conclusionFactory) {
            child {
                conclusion { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @Test
    fun copy_tree_with_2_children() {
        tree = ruleTree(conclusionFactory) {
            child {
                + "ConcA"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcB" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @Test
    fun `interpreting a case should not overwrite the verified text`() {
        val verifiedText = "Go to Bondi"
        val case = RDRCase("")
        case.interpretation.verifiedText = verifiedText
        tree.apply(case)
        case.interpretation.verifiedText shouldBe verifiedText
    }

    private fun checkInterpretationForCase(text: String, vararg conclusions: String) {
        val case = clinicalNotesCase(text)
        tree.apply(case)
        case.interpretation.conclusions().map { it.text }.toSet() shouldBe conclusions.toSet()
    }
}