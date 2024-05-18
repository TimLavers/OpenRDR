package steps

import io.cucumber.java8.En
import io.rippledown.examples.contact_lenses.ContactLensesCases
import io.rippledown.examples.vltsh.TSHCases
import io.rippledown.integration.RestClientAttributeFactory
import io.rippledown.integration.RestClientConclusionFactory
import io.rippledown.integration.RestClientConditionFactory
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsAbsentFromCase

class ContactLensesKBStepDefs : En {
    init {

        When("the Contact Lenses sample KB has been loaded") {
            val attributeFactory = RestClientAttributeFactory(labProxy().restProxy)
            val caseMaker = ContactLensesCases(attributeFactory)
            setAttributeOrder(caseMaker)
            provideCases(caseMaker)
//            buildTSHRules()
        }
    }

    private fun buildTSHRules() = TSHRulesBuilder(labProxy().restProxy).buildRules()

    private fun setAttributeOrder(cases: ContactLensesCases) {
        val attributesInOrder = listOf(cases.age, cases.prescription, cases.astigmatism, cases.tearProduction)
        labProxy().restProxy.setAttributeOrder(attributesInOrder)
    }

    private fun provideCases(cases: ContactLensesCases) {
        cases.allCases.forEach {
            labProxy().provideCase(it)
        }
    }
}
