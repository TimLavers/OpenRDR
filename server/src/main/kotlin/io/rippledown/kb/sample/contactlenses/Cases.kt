@file:Suppress("EnumEntryName")
package io.rippledown.kb.sample.contactlenses

import io.rippledown.kb.AttributeManager
import io.rippledown.kb.sample.defaultDate
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder

enum class Age {
    young, pre_presbyopic, presbyopic
}
enum class Prescription {
    myope, hypermetrope
}
enum class Astigmatism {
    not_astigmatic, astigmatic
}
enum class TearProduction {
    reduced, normal
}

class Cases(attributeFactory: AttributeManager) {
    val age = attributeFactory.getOrCreate("age")
    val prescription = attributeFactory.getOrCreate("prescription")
    val astigmatism = attributeFactory.getOrCreate("astigmatism")
    val tearProduction = attributeFactory.getOrCreate("tear production")
    private val case1 = makeCase("Case1", Age.young, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.reduced)
    private val case2 = makeCase("Case2", Age.young, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.normal)                        // soft
    private val case3 = makeCase("Case3", Age.young, Prescription.myope, Astigmatism.astigmatic, TearProduction.reduced)                           // none
    private val case4 = makeCase("Case4", Age.young, Prescription.myope, Astigmatism.astigmatic, TearProduction.normal)                            // hard
    private val case5 = makeCase("Case5", Age.young, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.reduced)                // none
    private val case6 = makeCase("Case6", Age.young, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.normal)                 // soft
    private val case7 = makeCase("Case7", Age.young, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.reduced)                    // none
    private val case8 = makeCase("Case8", Age.young, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.normal)                     // hard
    private val case9 = makeCase("Case9", Age.pre_presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.reduced)              // none
    private val case10 = makeCase("Case10", Age.pre_presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.normal)              // soft
    private val case11 = makeCase("Case11", Age.pre_presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.reduced)                 // none
    private val case12 = makeCase("Case12", Age.pre_presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.normal)                  // hard
    private val case13 = makeCase("Case13", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.reduced)      // none
    private val case14 = makeCase("Case14", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.normal)       // soft
    private val case15 = makeCase("Case15", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.reduced)          // none
    private val case16 = makeCase("Case16", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.normal)           // none
    private val case17 = makeCase("Case17", Age.presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.reduced)                 // none
    private val case18 = makeCase("Case18", Age.presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.normal)                  // none
    private val case19 = makeCase("Case19", Age.presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.reduced)                     // none
    private val case20 = makeCase("Case20", Age.presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.normal)                      // hard
    private val case21 = makeCase("Case21", Age.presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.reduced)          // none
    private val case22 = makeCase("Case22", Age.presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.normal)           // soft
    private val case23 = makeCase("Case23", Age.presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.reduced)              // none
    private val case24 = makeCase("Case24", Age.presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.normal)               // none
    val allCases = listOf(
        case1, case2, case3, case4, case5, case6, case7, case8, case9, case10,
        case11, case12, case13, case14, case15, case16, case17, case18, case19, case20,
        case21, case22, case23, case24)

    private fun makeCase(caseName: String, ageValue: Age, prescriptionValue: Prescription, astigmatismValue: Astigmatism, tearProductionValue: TearProduction): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(age, defaultDate, ageValue.name)
        builder.addValue(prescription, defaultDate, prescriptionValue.name)
        builder.addValue(astigmatism, defaultDate, astigmatismValue.name)
        builder.addValue(tearProduction, defaultDate, tearProductionValue.name)
        return builder.build(caseName)
    }
}