package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

class DummyRuleFactory : RuleFactory {
    override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>): Rule =
        Rule(0, parent, conditions, mutableSetOf(), assignment)
}

internal class RuleTreeTest : RuleTestBase() {
    private lateinit var tree: RuleTree
    private val A = "A"
    private val B = "B"
    private val kase = clinicalNotesCase("abc")
    private lateinit var commentFactory: CommentFactory
    private lateinit var conditionFactory: DummyConditionFactory

    @BeforeTest
    fun init() {
        tree = RuleTree()
        commentFactory = CommentFactory()
        conditionFactory = DummyConditionFactory()
    }

    @Test
    fun rulesForId() {
        tree = ruleTree(commentFactory) {
            child {
                id = 1
                comment { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 11
                    comment { "ConcB" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = 111
                        comment { "ConcC" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                        child {
                            id = 1111
                            comment { "ConcD" }
                            condition(conditionFactory) {
                                attribute = clinicalNotes
                                constant = "d"
                            }
                        }
                    }
                }
            }
        }.build()
        tree.ruleForId(tree.root.id) shouldBe tree.root
        tree.ruleForId(1).assignment shouldBe commentFactory.comment("ConcA")
        tree.ruleForId(11).assignment shouldBe commentFactory.comment("ConcB")
        tree.ruleForId(111).assignment shouldBe commentFactory.comment("ConcC")
        tree.ruleForId(1111).assignment shouldBe commentFactory.comment("ConcD")
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
        kase.interpretation.assignments() shouldBe setOf()
    }

    @Test
    fun root_rule_should_not_apply_to_a_case() {
        tree.apply(kase)
        kase.interpretation shouldBe Interpretation(CaseId(null, kase.name))
    }

    @Test
    fun add_to_empty_root() {
        tree = ruleTree(commentFactory) {
            child {
                +A
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()
        tree.apply(kase)
        val assignment1 = tree.root.childRules().first().assignment!!
        checkInterpretation(kase.interpretation, assignment1)
        (assignment1.expression as CommentTemplate).text shouldBe A
    }

    @Test
    fun add_to_root_that_has_one_child() {
        tree = ruleTree(commentFactory) {
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
        val assignment1 = tree.root.childRules().first().assignment!!
        val assignment2 = tree.root.childRules().last().assignment!!
        checkInterpretation(kase.interpretation, assignment1, assignment2)
    }

    @Test
    fun add_to_two_leaf_rules() {
        tree = ruleTree(commentFactory) {
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
        tree = ruleTree(commentFactory) {
            child {
                comment { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                comment { "ConcB" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
            child {
                comment { "ConcC" }
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
        tree = ruleTree(commentFactory) {
            child {
                comment { "ConcA" }
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
        tree = ruleTree(commentFactory) {
            child {
                comment { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                comment { "ConcA" }
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
        tree = ruleTree(commentFactory) {
            child {
                comment { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    comment { "ConcA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        comment { "ConcB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    comment { "ConcD" }
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
        tree = ruleTree(commentFactory) {
            child {
                id = 1
                comment { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 11
                    comment { "ConcA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        id = 111
                        comment { "ConcB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                    }
                }
                child {
                    id = 12
                    comment { "ConcD" }
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
        tree = ruleTree(commentFactory) {
            child {
                id = 1
                comment { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    comment { "ConcA" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "b"
                    }
                    child {
                        comment { "ConcB" }
                        condition(conditionFactory) {
                            attribute = clinicalNotes
                            constant = "c"
                        }
                        child {
                            comment { "ConcA" }
                            condition(conditionFactory) {
                                attribute = clinicalNotes
                                constant = "d"
                            }
                        }
                    }
                }
                child {
                    comment { "ConcD" }
                    condition(conditionFactory) {
                        attribute = clinicalNotes
                        constant = "d"
                    }
                }
            }
        }.build()
        val predicate: ((Rule) -> Boolean) = { r ->
            (r.assignment?.expression as? CommentTemplate)?.text == "ConcA"
        }
        val rulesMatching = tree.rulesMatching(predicate)
        rulesMatching.size shouldBe 3
        rulesMatching.forEach { (it.assignment?.expression as CommentTemplate).text shouldBe "ConcA" }
    }

    @Test
    fun add_child_under_child_under_root() {
        tree = ruleTree(commentFactory) {
            child {
                id = 1
                comment { "ConcA" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
                child {
                    id = 2
                    comment { "ConcB" }
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
        tree = ruleTree(commentFactory) {
        }.build()
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    @Test
    fun copy_tree_with_1_child() {
        tree = ruleTree(commentFactory) {
            child {
                comment { "ConcA" }
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
        tree = ruleTree(commentFactory) {
            child {
                + "ConcA"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                comment { "ConcB" }
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
        tree.copy() shouldBe tree
        (tree.copy() !== tree) shouldBe true
    }

    private fun checkInterpretationForCase(text: String, vararg conclusions: String) {
        val case = clinicalNotesCase(text)
        tree.apply(case)
        case.interpretation.assignments().map { (it.expression as CommentTemplate).text }
            .toSet() shouldBe conclusions.toSet()
    }
}