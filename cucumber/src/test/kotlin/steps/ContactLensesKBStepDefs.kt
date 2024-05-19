package steps

import io.cucumber.java8.En
import io.rippledown.examples.contact_lenses.*
import io.rippledown.examples.contact_lenses.Age.young
import io.rippledown.examples.contact_lenses.Astigmatism.astigmatic
import io.rippledown.examples.contact_lenses.Astigmatism.not_astigmatic
import io.rippledown.examples.contact_lenses.Prescription.myope
import io.rippledown.examples.contact_lenses.TearProduction.normal
import io.rippledown.examples.vltsh.TSHCases
import io.rippledown.integration.RestClientAttributeFactory
import io.rippledown.integration.RestClientConclusionFactory
import io.rippledown.integration.RestClientConditionFactory
import io.rippledown.integration.RestClientRuleBuilder
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
            ContactLensesKBRuleBuilder(labProxy().restProxy).buildRules()
        }
    }

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
class ContactLensesKBRuleBuilder(restClient: RESTClient): RestClientRuleBuilder(restClient) {
    private val age = attributeFactory.create("age")
    val prescription = attributeFactory.create("prescription")
    private val astigmatism = attributeFactory.create("astigmatism")
    private val tearProduction = attributeFactory.create("tear production")
    val young = conditionFactory.getOrCreate(isCondition(age, Age.young.name))
    private val notAstigmatic = conditionFactory.getOrCreate(isCondition(astigmatism, not_astigmatic.name))
    private val astigmatic = conditionFactory.getOrCreate(isCondition(astigmatism, Astigmatism.astigmatic.name))
    private val normalTearProduction = conditionFactory.getOrCreate(isCondition(tearProduction, normal.name))
    private val myope = conditionFactory.getOrCreate(isCondition(prescription, Prescription.myope.name))
    private val soft = "soft"
    private val hard = "hard"

    fun buildRules() {
        addCommentForCase("Case2", soft, notAstigmatic, normalTearProduction)
        addCommentForCase("Case4", hard, myope, astigmatic, normalTearProduction)
    }
}
