package io.rippledown.kb.scripts

import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import kotlin.test.BeforeTest
import kotlin.test.Test

class AddingConclusionsTest {
    @Test
    fun Add_conclusion() {
        build {
            case("Case1", "a")
            requireInterpretation("Case1")
            session {
                selectCase("Case1")
                addConclusion("A")
                condition("a")
                    requireCornerstones()
                commit()
            }
            requireInterpretation("Case1", "A")
        }
    }

    @Test
    fun add_two_conclusions() {
        build {
            case("Case1", "a")
            session {
                selectCase("Case1")
                addConclusion("A")
                condition("a")
                commit()
            }
            requireInterpretation("Case1", "A")
            session {
                selectCase("Case1")
                addConclusion("B")
                condition("a")
                requireCornerstones()
                commit()
            }
            requireInterpretation("Case1", "A", "B")
        }
    }

//
//        "All cornerstones should be presented when adding a conclusion"{
//            build {
//                case("1", "a")
//                case("2", "b")
//                case("3", "c")
//                case("4", "abc")
//                session {
//                    selectCase("1")
//                    addConclusion("A")
//                    requireCornerstones("2", "3", "4") //the current case is not a cornerstone
//                    condition("a")
//                    requireCornerstones("4")
//                    commit()
//                }
//                requireInterpretation("1", "A")
//                requireInterpretation("2")
//                requireInterpretation("3")
//                requireInterpretation("4", "A")
//            }
//
//        }
//        "session action must be defined before adding a condition"{
//            val exception = shouldThrow<Exception> {
//                build {
//                    case("1", "a")
//                    session {
//                        selectCase("1")
//                        condition("a")
//                        addConclusion("A")
//                    }
//                }
//            }
//            exception.message shouldEqual addedConditionBeforeSessionStarted
//        }
//
//        "session current case must be defined before adding a condition"{
//            val exception = shouldThrow<Exception> {
//                build {
//                    case("1", "a")
//                    session {
//                        condition("a")
//                    }
//                }
//            }
//            exception.message shouldEqual addedConditionBeforeSessionStarted
//        }
//    }
//
}