package io.rippledown.scripts

//import io.kotlintest.specs.StringSpec
//
//class ReplacingConclusionsTest : StringSpec() {
//    init {
//
//        "Replace conclusion using a rule with no conditions"{
//            build {
//                case("1", "a")
//                requireInterpretation("1")
//                session {
//                    selectCase("1")
//                    addConclusion("A")
//                    commit()
//                }
//
//                requireInterpretation("1", "A")
//                session {
//                    selectCase("1")
//                    replaceConclusion("A", "B")
//                    commit()
//                }
//                requireInterpretation("1", "B")
//            }
//        }
//
//        "Replace conclusion with a rule with one condition"{
//            build {
//                case("1", "a")
//                case("2", "b")
//                case("3", "ab")
//
//                requireInterpretation("1")
//                requireInterpretation("2")
//                requireInterpretation("3")
//
//                session {
//                    selectCase("1")
//                    addConclusion("A")
//                    condition("a")
//                    commit()
//                }
//
//                session {
//                    selectCase("2")
//                    addConclusion("B")
//                    condition("b")
//                    commit()
//                }
//
//                requireInterpretation("1", "A")
//                requireInterpretation("2", "B")
//                requireInterpretation("3", "A", "B")
//
//                session {
//                    selectCase("3")
//                    replaceConclusion("B", "C")
//                    condition("ab")
//                    commit()
//                }
//                requireInterpretation("1", "A")
//                requireInterpretation("2", "B")
//                requireInterpretation("3", "A", "C")
//            }
//        }
//
//        "Replace a conclusion that is given by several rules"{
//            build {
//                (1..4).forEach {
//                    case(it)
//                    requireInterpretation(it.toString())
//                }
//
//                session {
//                    selectCase("4")
//                    addConclusion("A")
//                    condition(4)
//                    commit()
//                }
//
//                session {
//                    selectCase("3")
//                    addConclusion("A")
//                    condition(3)
//                    commit()
//                }
//
//                session {
//                    selectCase("1")
//                    addConclusion("A")
//                    condition(1)
//                    commit()
//                }
//
//                session {
//                    selectCase("2")
//                    replaceConclusion("A", "B")
//                    condition(2)
//                    commit()
//                }
//
//                requireInterpretation("1", "A", "B")
//                requireInterpretation("2", "B")
//                requireInterpretation("3", "A")
//                requireInterpretation("4", "A")
//            }
//        }
//    }
//
//}