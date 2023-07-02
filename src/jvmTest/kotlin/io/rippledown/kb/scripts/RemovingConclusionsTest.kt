package io.rippledown.kb.scripts

import kotlin.test.Test

class RemovingConclusionsTest {

    @Test
    fun remove_conclusion() {
        build {
            case("1", "a")
            requireInterpretation("1")
            session {
                selectCase("1")
                +"A"
                commit()
            }

            requireInterpretation("1", "A")
            session {
                selectCase("1")
                -"A"
                commit()
            }
            requireInterpretation("1")
        }
    }

    @Test
    fun remove_conclusion_with_condition() {
        build {
            case("1", "a")
            case("2", "b")
            case("3", "ab")

            cornerstoneCase("1", "a")
            cornerstoneCase("2", "b")
            cornerstoneCase("3", "ab")

            requireInterpretation("1")
            requireInterpretation("2")
            requireInterpretation("3")

            session {
                selectCase("1")
                +"A"
                requireCornerstones("2", "3")
                condition("a")
                requireCornerstones("3")
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2")
            requireInterpretation("3", "A")

            session {
                selectCase("2")
                +"B"
                requireCornerstones("1", "3")
                condition("b")
                requireCornerstones("3")
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2", "B")
            requireInterpretation("3", "A", "B")

            session {
                selectCase("3")
                -"B"
                requireCornerstones("2")
                condition("ab")
                requireCornerstones()
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2", "B")
            requireInterpretation("3", "A")
        }
    }

    @Test
    fun remove_a_conclusion_that_is_given_by_several_rules() {
        build {
            (1..4).forEach {
                case(it)
                cornerstoneCase(it)
                requireInterpretation(it.toString())
            }

            session {
                selectCase("4")
                +"A"
                requireCornerstones("1", "2", "3")
                condition(4)
                requireCornerstones()
                commit()
            }

            session {
                selectCase("3")
                +"A"
                requireCornerstones("1", "2")
                condition(3)
                requireCornerstones()
                commit()
            }

            session {
                selectCase("1")
                +"A"
                requireCornerstones("2")
                condition(1)
                requireCornerstones("2")
                commit()
            }

            requireInterpretation("1", "A")
            requireInterpretation("2", "A")
            requireInterpretation("3", "A")
            requireInterpretation("4", "A")
            session {
                selectCase("2")
                -"A"
                requireCornerstones("1")
                condition(2)
                requireCornerstones()
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2")
            requireInterpretation("3", "A")
            requireInterpretation("4", "A")
        }
    }

    @Test
    fun cornerstone_not_presented_as_conflict_if_not_all_instances_of_conclusion_removed() {
        build {
            case("1", "a")
            case("2", "b")
            case("3", "ab")

            cornerstoneCase("1", "a")
            cornerstoneCase("2", "b")
            cornerstoneCase("3", "ab")

            requireInterpretation("1")
            requireInterpretation("2")
            requireInterpretation("3")

            session {
                selectCase("1")
                +"A"
                requireCornerstones("2", "3")
                condition("a")
                requireCornerstones("3")
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2")
            requireInterpretation("3", "A")

            session {
                selectCase("2")
                +"A"
                requireCornerstones()
                condition("b")
                requireCornerstones()
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2", "A")
            requireInterpretation("3", "A")

            session {
                selectCase("1")
                -"A"
                // Case 1 is not a conflicting cornerstone, as it is the
                // case for which the rule is being built.
                // Case 2 is not a conflicting cornerstone because the
                // rule giving A for it is not being changed,
                // Case 3 is not a conflicting cornerstone because it gets
                // conclusion A from two rules, and one of these is not
                // affected by the rule being built.
                requireCornerstones()
                commit()
            }
            requireInterpretation("1")
            requireInterpretation("2", "A")
            requireInterpretation("3", "A")
        }
    }
}