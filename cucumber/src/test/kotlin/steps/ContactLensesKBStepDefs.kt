package steps

import io.cucumber.java8.En
import io.rippledown.examples.contact_lenses.*
import io.rippledown.examples.contact_lenses.Age.pre_presbyopic
import io.rippledown.examples.contact_lenses.Astigmatism.not_astigmatic
import io.rippledown.examples.contact_lenses.Prescription.hypermetrope
import io.rippledown.examples.contact_lenses.TearProduction.normal
import io.rippledown.integration.RestClientAttributeFactory
import io.rippledown.integration.RestClientRuleBuilder
import io.rippledown.integration.restclient.RESTClient

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
    private val prePresbyopic = conditionFactory.getOrCreate(isCondition(age, pre_presbyopic.name))
    private val notAstigmatic = conditionFactory.getOrCreate(isCondition(astigmatism, not_astigmatic.name))
    private val astigmatic = conditionFactory.getOrCreate(isCondition(astigmatism, Astigmatism.astigmatic.name))
    private val normalTearProduction = conditionFactory.getOrCreate(isCondition(tearProduction, normal.name))
    private val myope = conditionFactory.getOrCreate(isCondition(prescription, Prescription.myope.name))
    private val isHypermetrope = conditionFactory.getOrCreate(isCondition(prescription, hypermetrope.name))
    private val soft = "soft"
    private val hard = "hard"

    fun buildRules() {
        addCommentForCase("Case2", soft, notAstigmatic, normalTearProduction)
        addCommentForCase("Case4", hard, astigmatic, normalTearProduction)
        removeCommentForCase("Case16", hard, prePresbyopic, isHypermetrope)
    }
}
