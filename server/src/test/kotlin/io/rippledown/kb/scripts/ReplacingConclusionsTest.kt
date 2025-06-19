package io.rippledown.kb.scripts

import kotlin.test.Test

class ReplacingConclusionsTest {
    @Test
    fun `replace conclusion using a rule with no conditions`() {
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
    fun `replace conclusion with a rule with one condition`() {
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
                "C" replaces "B"
                requireCornerstones("2")
                condition("ab")
                requireCornerstones()
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2", "B")
            requireInterpretation("3", "A", "C")
        }
    }

    @Test
    fun `replace a conclusion that is given by several rules`() {
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

            session {
                selectCase("2")
                "B" replaces "A"
                requireCornerstones("1", "3", "4")
                condition(2)
                requireCornerstones("3", "4")
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2", "B")
            requireInterpretation("3", "A", "B")
            requireInterpretation("4", "A", "B")
        }
    }

    @Test
    fun `replace a conclusion that has been removed`() {
        build {
            case("1", "ac")
            case("2", "b")
            case("3", "ab")
            cornerstoneCase("1", "ac")
            cornerstoneCase("2", "b")
            cornerstoneCase("3", "ab")

            session {
                selectCase("1")
                +"X"
                requireCornerstones( "2", "3")
                condition("a")
                requireCornerstones("3")
                commit()
            }
            requireInterpretation("1", "X")
            requireInterpretation("2")
            requireInterpretation("1", "X")

            session {
                selectCase("2")
                +"X"
                requireCornerstones()
                condition("b")
                requireCornerstones()
                commit()
            }
            requireInterpretation("1", "X")
            requireInterpretation("2", "X")
            requireInterpretation("3", "X")

            session {
                selectCase("1")
                -"X"
                // Conflicting CCs:
                // Not 1: it's the session case.
                // Not 2: it gets X from a different rule from that from which 1 gets X.
                // Not 3: it gets X twice - once from the same context as 1,
                // and once from the same context as 2.
                requireCornerstones()
                condition("c")
                requireCornerstones()
                commit()
            }
            requireInterpretation("1")
            requireInterpretation("2", "X")
            requireInterpretation("3", "X")

            session {
                selectCase("3")
                "Y" replaces "X"
                requireCornerstones("1", "2")
                commit()
            }
            requireInterpretation("1", "Y")
            requireInterpretation("2", "Y")
            requireInterpretation("3", "Y")
        }
    }
}