package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleTreeTest : RuleTestBase() {
    private lateinit var tree: RuleTree
    private val A = "A"
    private val B = "B"
    private val notes = clinicalNotes.name
    private val conclusionA = conc(A)
    private val conclusionB = conc(B)
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
                    attributeName = notes
                    constant = "a"
                }
            }
        }.build()
        tree.apply(kase)
        checkInterpretation(kase.interpretation, conclusionA)
    }

    @Test
    fun add_to_root_that_has_one_child() {
        tree = ruleTree {
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
        tree.apply(kase)
        checkInterpretation(kase.interpretation, conclusionA, conclusionB)
    }

    @Test
    fun add_to_two_leaf_rules() {
        tree = ruleTree {
            child {
                + "ConcA"
                condition {
                    attributeName = notes
                    constant = "a"
                }
                child {
                    +"ConcC"
                    condition {
                        attributeName = notes
                        constant = "c"
                    }
                }
            }
            child {
                + "ConcB"
                condition {
                    attributeName = notes
                    constant = "b"
                }
                child {
                    + "ConcC"
                    condition {
                        attributeName = notes
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
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcB" }
                condition {
                    attributeName = notes
                    constant = "b"
                }
            }
            child {
                conclusion { "ConcC" }
                condition {
                    attributeName = notes
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
                    attributeName = notes
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
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcA" }
                condition {
                    attributeName = notes
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
                    attributeName = notes
                    constant = "a"
                }
                child {
                    conclusion { "ConcA" }
                    condition {
                        attributeName = notes
                        constant = "b"
                    }
                    child {
                        conclusion { "ConcB" }
                        condition {
                            attributeName = notes
                            constant = "c"
                        }
                    }
                }
                child {
                    conclusion { "ConcD" }
                    condition {
                        attributeName = notes
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
                    attributeName = notes
                    constant = "a"
                }
                child {
                    id = "c11"
                    conclusion { "ConcA" }
                    condition {
                        attributeName = notes
                        constant = "b"
                    }
                    child {
                        id = "c111"
                        conclusion { "ConcB" }
                        condition {
                            attributeName = notes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = "c12"
                    conclusion { "ConcD" }
                    condition {
                        attributeName = notes
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
                    attributeName = notes
                    constant = "a"
                }
                child {
                    conclusion { "ConcA" }
                    condition {
                        attributeName = notes
                        constant = "b"
                    }
                    child {
                        conclusion { "ConcB" }
                        condition {
                            attributeName = notes
                            constant = "c"
                        }
                        child {
                            conclusion { "ConcA" }
                            condition {
                                attributeName = notes
                                constant = "d"
                            }
                        }
                    }
                }
                child {
                    conclusion { "ConcD" }
                    condition {
                        attributeName = notes
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
                    attributeName = notes
                    constant = "a"
                }
                child {
                    id = "c2"
                    conclusion { "ConcB" }
                    condition {
                        attributeName = notes
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
                    attributeName = notes
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
                    attributeName = notes
                    constant = "a"
                }
            }
            child {
                conclusion { "ConcB" }
                condition {
                    attributeName = notes
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
        val case = RDRCase()
        case.interpretation.verifiedText = verifiedText
        tree.apply(case)
        case.interpretation.verifiedText shouldBe verifiedText
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