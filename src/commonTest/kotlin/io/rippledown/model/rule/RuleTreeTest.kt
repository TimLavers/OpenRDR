package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
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
        val interpretation = tree.apply(kase)
        interpretation.conclusions() shouldBe setOf<Conclusion>()
    }

    @Test
    fun root_rule_should_not_apply_to_a_case() {
        tree.apply(kase) shouldBe Interpretation(CaseId(kase.name, kase.name))
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

        val interpretation = tree.apply(kase)
        checkInterpretation(interpretation, conclusionA)
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

        val interpretation = tree.apply(kase)
        checkInterpretation(interpretation, conclusionA, conclusionB)
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
        tree.rules().size shouldBe 5
        tree.rules().contains(tree.root) shouldBe true
        val r111 = Rule(null, Conclusion("ConcB"), setOf(cond("c")))
        val childRules11: MutableSet<Rule> = mutableSetOf(r111)
        val r11 = Rule(null, Conclusion("ConcA"), setOf(cond("b")), childRules11)
        val r12 = Rule(null, Conclusion("ConcD"), setOf(cond("d")))
        val childRules12: MutableSet<Rule> = mutableSetOf(r11, r12)
        val r1 = Rule(null, Conclusion("ConcA"), setOf(cond("a")), childRules12)
        tree.root.addChild(r1)
        tree.rules() shouldBe setOf(tree.root, r1, r11, r111, r12)
    }

    @Test
    fun rulesWithConclusionTest() {
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
                conclusion { "ConcA" }
                condition {
                    attributeName = notes
                    constant = "a"
                }
                child {
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
        val r1 = Rule(tree.root, Conclusion("ConcA"), setOf(cond("a")))
        val r11 = Rule(r1, Conclusion("ConcB"), setOf(cond("b")))

        tree.rules() shouldBe setOf(tree.root, r1, r11)
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

    @BeforeTest
    fun init() {
        tree = RuleTree()
    }

    private fun checkInterpretationForCase(text: String, vararg conclusions: String) {
        val case = clinicalNotesCase(text)
        val interp = tree.apply(case)
        interp.conclusions().map { it -> it.text }.toSet() shouldBe conclusions.toSet()
    }
}