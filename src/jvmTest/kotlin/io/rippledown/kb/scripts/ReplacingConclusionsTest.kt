package io.rippledown.kb.scripts

import kotlin.test.Test

class ReplacingConclusionsTest {
    @Test
    fun replace_conclusion_using_a_rule_with_no_conditions() {
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
                "B" replaces "A"
                commit()
            }
            requireInterpretation("1", "B")
        }
    }

    @Test
    fun replace_conclusion_with_a_rule_with_one_condition() {
        build {
            case("1", "a")
            case("2", "b")
            case("3", "ab")

            requireInterpretation("1")
            requireInterpretation("2")
            requireInterpretation("3")

            session {
                selectCase("1")
                +"A"
                condition("a")
                commit()
            }

            session {
                selectCase("2")
                +"B"
                condition("b")
                commit()
            }

            requireInterpretation("1", "A")
            requireInterpretation("2", "B")
            requireInterpretation("3", "A", "B")

            session {
                selectCase("3")
                "C" replaces "B"
                condition("ab")
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2", "B")
            requireInterpretation("3", "A", "C")
        }
    }

    @Test
    fun replace_a_conclusion_that_is_given_by_several_rules() {
        build {
            (1..4).forEach {
                case(it)
                requireInterpretation(it.toString())
            }

            session {
                selectCase("4")
                +"A"
                condition(4)
                commit()
            }

            session {
                selectCase("3")
                +"A"
                condition(3)
                commit()
            }

            session {
                selectCase("1")
                +"A"
                condition(1)
                commit()
            }

            session {
                selectCase("2")
                "B" replaces "A"
                condition(2)
                commit()
            }

            requireInterpretation("1", "A")
            requireInterpretation("2", "B")
            requireInterpretation("3", "A", "B")
            requireInterpretation("4", "A", "B")
        }
    }
}