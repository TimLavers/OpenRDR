@file:Suppress("EnumEntryName")

package io.rippledown.examples.contact_lenses

import io.rippledown.model.AttributeFactory
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.defaultDate

/**
 * Cases from the
 */
class ContactLensesCases(val attributeFactory: AttributeFactory) {
    private enum class Age {
        young, pre_presbyopic, presbyopic

    }
    private enum class Prescription {
        myope, hypermetrope
    }
    private enum class Astigmatism {
        not_astigmatic, astigmatic
    }
    private enum class TearProduction {
        reduced, normal
    }
    val age = attributeFactory.create("age")
    val prescription = attributeFactory.create("prescription")
    val astigmatism = attributeFactory.create("astigmatism")
    val tearProduction = attributeFactory.create("tear production")
    val Case1 = makeCase("Case1", Age.young, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.reduced)
    val Case2 = makeCase("Case2", Age.young, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.normal)                        // soft
    val Case3 = makeCase("Case3", Age.young, Prescription.myope, Astigmatism.astigmatic, TearProduction.reduced)                           // none
    val Case4 = makeCase("Case4", Age.young, Prescription.myope, Astigmatism.astigmatic, TearProduction.normal)                            // hard
    val Case5 = makeCase("Case5", Age.young, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.reduced)                // none
    val Case6 = makeCase("Case6", Age.young, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.normal)                 // soft
    val Case7 = makeCase("Case7", Age.young, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.reduced)                    // none
    val Case8 = makeCase("Case8", Age.young, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.normal)                     // hard
    val Case9 = makeCase("Case9", Age.pre_presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.reduced)              // none
    val Case10 = makeCase("Case10", Age.pre_presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.normal)              // soft
    val Case11 = makeCase("Case11", Age.pre_presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.reduced)                 // none
    val Case12 = makeCase("Case12", Age.pre_presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.normal)                  // hard
    val Case13 = makeCase("Case13", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.reduced)      // none
    val Case14 = makeCase("Case14", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.normal)       // soft
    val Case15 = makeCase("Case15", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.reduced)          // none
    val Case16 = makeCase("Case16", Age.pre_presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.normal)           // none
    val Case17 = makeCase("Case17", Age.presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.reduced)                 // none
    val Case18 = makeCase("Case18", Age.presbyopic, Prescription.myope, Astigmatism.not_astigmatic, TearProduction.normal)                  // none
    val Case19 = makeCase("Case19", Age.presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.reduced)                     // none
    val Case20 = makeCase("Case20", Age.presbyopic, Prescription.myope, Astigmatism.astigmatic, TearProduction.normal)                      // hard
    val Case21 = makeCase("Case21", Age.presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.reduced)          // none
    val Case22 = makeCase("Case22", Age.presbyopic, Prescription.hypermetrope, Astigmatism.not_astigmatic, TearProduction.normal)           // soft
    val Case23 = makeCase("Case23", Age.presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.reduced)              // none
    val Case24 = makeCase("Case24", Age.presbyopic, Prescription.hypermetrope, Astigmatism.astigmatic, TearProduction.normal)               // none
    val allCases = listOf(
        Case1, Case2, Case3, Case4, Case5, Case6, Case7, Case8, Case9, Case10,
        Case11, Case12, Case13, Case14, Case15, Case16, Case17, Case18, Case19, Case20,
        Case21, Case22, Case23, Case24
        )
    private fun makeCase(caseName: String, ageValue: Age, prescriptionValue: Prescription, astigmatismValue: Astigmatism, tearProductionValue: TearProduction): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(age, defaultDate, ageValue.name)
        builder.addValue(prescription, defaultDate, prescriptionValue.name)
        builder.addValue(astigmatism, defaultDate, astigmatismValue.name)
        builder.addValue(tearProduction, defaultDate, tearProductionValue.name)
        return builder.build(caseName)
    }

}