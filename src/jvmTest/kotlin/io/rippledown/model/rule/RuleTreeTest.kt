package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleTreeTest : RuleTestBase() {
    private lateinit var tree: RuleTree
    private val A = "A"
    private val B = "B"
    private val kase = clinicalNotesCase("abc")

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
        kase.interpretation shouldBe Interpretation(CaseId(kase.name, kase.name))
    }

    @Test
    fun add_to_empty_root() {
        tree = ruleTree {
            child {
                +A
                condition {
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
        tree = ruleTree {
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
        tree.apply(kase)
        val conclusion1 = tree.root.childRules().first().conclusion!!
        val conclusion2 = tree.root.childRules().last().conclusion!!
        checkInterpretation(kase.interpretation, conclusion1, conclusion2)
    }

    @Test
    fun add_to_two_leaf_rules() {
        tree = ruleTree {
            child {
                + "ConcA"
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    +"ConcC"
                    condition {
                        attribute = clinicalNotes
                        constant = "c"
                    }
                }
            }
            child {
                + "ConcB"
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
                child {
                    + "ConcC"
                    condition {
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
        tree = ruleTree {
            child {
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcB" }
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
            child {
                conclusion { "ConcC" }
                condition {
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
        tree = ruleTree {
            child {
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()
        tree.size() shouldBe 2L
    }

    @Test
    fun size_with_two_children_of_root() {
        tree = ruleTree {
            child {
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
        tree.size() shouldBe 3L
    }

    @Test
    fun size_with_depth_four() {
        tree = ruleTree {
            child {
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    conclusion { "ConcA" }
                    condition {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        conclusion { "ConcB" }
                        condition {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    conclusion { "ConcD" }
                    condition {
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
        tree = ruleTree {
            child {
                id = "c1"
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = "c11"
                    conclusion { "ConcA" }
                    condition {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = "c111"
                        conclusion { "ConcB" }
                        condition {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = "c12"
                    conclusion { "ConcD" }
                    condition {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        tree.rules().size shouldBe 5
        tree.rules().contains(tree.root) shouldBe true
        tree.rules().map { rule -> rule.id } shouldContainAll listOf(tree.root.id, "c1", "c11", "c111", "c12")
    }

    @Test
    fun rulesWithConclusionTest() {
        tree = ruleTree {
            child {
                id = "c1"
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    conclusion { "ConcA" }
                    condition {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        conclusion { "ConcB" }
                        condition {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                        child {
                            conclusion { "ConcA" }
                            condition {
                                attribute = clinicalNotes
                                constant = "d"
                            }
                        }
                    }
                }
                child {
                    conclusion { "ConcD" }
                    condition {
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
        tree = ruleTree {
            child {
                id = "c1"
                conclusion { "ConcA" }
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = "c2"
                    conclusion { "ConcB" }
                    condition {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                }
            }
        }.build()
        tree.size() shouldBe 3L
        tree.rules().contains(tree.root) shouldBe true

        tree.rules().map { rule -> rule.id } shouldBe setOf(tree.root.id, "c1", "c2")
    }

    @Test
    fun copy_Empty_Tree() {
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @Test
    fun copy_root() {
        tree = ruleTree {
        }.build()
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @Test
    fun copy_tree_with_1_child() {
        tree = ruleTree {
            child {
                conclusion { "ConcA" }
                condition {
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
        tree = ruleTree {
            child {
                + "ConcA"
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcB" }
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @BeforeTest
    fun init() {
        tree = RuleTree()
    }

    private fun checkInterpretationForCase(text: String, vararg conclusions: String) {
        val case = clinicalNotesCase(text)
        tree.apply(case)
        case.interpretation.conclusions().map { it.text }.toSet() shouldBe conclusions.toSet()
    }
}