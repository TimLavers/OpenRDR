package io.rippledown.kb.scripts

import kotlin.test.Test

class RemoveLastRuleTest {

    @Test
    fun `undo removes all rules added in a single session`() {
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

            // Now undo and check.
            undoLastRuleSession()
            requireInterpretation("1", "A")
            requireInterpretation("2", "A")
            requireInterpretation("3", "A")
            requireInterpretation("4", "A")

            undoLastRuleSession()
            requireInterpretation("1")
            requireInterpretation("2")
            requireInterpretation("3", "A")
            requireInterpretation("4", "A")

            undoLastRuleSession()
            requireInterpretation("1")
            requireInterpretation("2")
            requireInterpretation("3")
            requireInterpretation("4", "A")

            undoLastRuleSession()
            requireInterpretation("1")
            requireInterpretation("2")
            requireInterpretation("3")
            requireInterpretation("4")
        }
    }
}