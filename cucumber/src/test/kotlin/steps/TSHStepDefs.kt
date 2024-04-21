package steps

import io.cucumber.java8.En
import io.rippledown.examples.vltsh.TSHCases
import io.rippledown.integration.RestClientAttributeFactory

class TSHStepDefs : En {
    init {
        When("the TSH sample KB has been loaded") {
            setupTSHSampleKB()
        }
    }

    private fun setupTSHSampleKB() {
        val attributeNamesInOrder = listOf(
            "Sex",
            "Age",
            "TSH",
            "Free T4",
            "Free T3",
            "TPO Antibodies",
            "Thyroglobulin",
            "Anti-Thyroglobulin",
            "Patient Location",
            "Tests",
            "Clinical Notes",
        )
        val attributesInOrder = attributeNamesInOrder.map {
            labProxy().restProxy.getOrCreateAttribute(it)
        }
        labProxy().restProxy.setAttributeOrder(attributesInOrder)
        val tshCases = TSHCases(RestClientAttributeFactory(restClient()))
        labProxy().provideCase(tshCases.TSH1)
        labProxy().provideCase(tshCases.TSH2)
        labProxy().provideCase(tshCases.TSH3)
        labProxy().provideCase(tshCases.TSH4)
        labProxy().provideCase(tshCases.TSH5)
        labProxy().provideCase(tshCases.TSH6)
        labProxy().provideCase(tshCases.TSH7)
        labProxy().provideCase(tshCases.TSH8)
        labProxy().provideCase(tshCases.TSH9)
        labProxy().provideCase(tshCases.TSH10)
        labProxy().provideCase(tshCases.TSH11)
        labProxy().provideCase(tshCases.TSH12)
        labProxy().provideCase(tshCases.TSH13)
        labProxy().provideCase(tshCases.TSH14)
        labProxy().provideCase(tshCases.TSH15)
        labProxy().provideCase(tshCases.TSH16)
        labProxy().provideCase(tshCases.TSH17)
        labProxy().provideCase(tshCases.TSH18)
        labProxy().provideCase(tshCases.TSH19)
        labProxy().provideCase(tshCases.TSH20)
        labProxy().provideCase(tshCases.TSH21)
        labProxy().provideCase(tshCases.TSH22)
        labProxy().provideCase(tshCases.TSH23)
        labProxy().provideCase(tshCases.TSH24)
        labProxy().provideCase(tshCases.TSH25)
        labProxy().provideCase(tshCases.TSH26)
        labProxy().provideCase(tshCases.TSH27)
        labProxy().provideCase(tshCases.TSH28)
        labProxy().provideCase(tshCases.TSH29)
        labProxy().provideCase(tshCases.TSH30)
        labProxy().provideCase(tshCases.TSH31)
        labProxy().provideCase(tshCases.TSH32)
        labProxy().provideCase(tshCases.TSH33)
        labProxy().provideCase(tshCases.TSH35)
    }
}
