package io.rippledown.kb.scripts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AddingConclusionsTest {
    @Test
    fun `add conclusion`() {
        build {
            case("Case1", "a")
            requireInterpretation("Case1")
            session {
                selectCase("Case1")
                +"A"
                condition("a")
                requireCornerstones()
                commit()
            }
            requireInterpretation("Case1", "A")
        }
    }

    @Test
    fun `add two conclusions`() {
        build {
            case("Case1", "a")
            session {
                selectCase("Case1")
                +"A"
                condition("a")
                commit()
            }
            requireInterpretation("Case1", "A")
            session {
                selectCase("Case1")
                +"B"
                condition("a")
                requireCornerstones()
                commit()
            }
            requireInterpretation("Case1", "A", "B")
        }
    }

    @Test
    fun `all cornerstones should be presented when adding a conclusion`() {
        build {
            cornerstoneCase("1", "a")
            cornerstoneCase("2", "b")
            cornerstoneCase("3", "c")
            cornerstoneCase("4", "abc")
            case("1","a")
            case("2", "b")
            case("3", "c")
            case("4", "abc")
            session {
                selectCase("1")
                +"A"
                requireCornerstones("2", "3", "4") //the current case is not a cornerstone
                condition("a")
                requireCornerstones("4")
                commit()
            }
            requireInterpretation("1", "A")
            requireInterpretation("2")
            requireInterpretation("3")
            requireInterpretation("4", "A")
        }
    }

    @Test
    fun `session action must be defined before adding a condition`() {
        val exception = shouldThrow<Exception> {
            build {
                case("1", "a")
                session {
                    selectCase("1")
                    condition("a")
                    +"A"
                }
            }
        }
        exception.message shouldBe addedConditionBeforeSessionStarted
    }

    @Test
    fun session_current_case_must_be_defined_before_adding_a_condition() {
        val exception = shouldThrow<Exception> {
            build {
                case("1", "a")
                session {
                    condition("a")
                }
            }
        }
        exception.message shouldBe addedConditionBeforeSessionStarted
    }
}