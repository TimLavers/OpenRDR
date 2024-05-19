package steps

import io.cucumber.java8.En
import io.rippledown.examples.contact_lenses.*
import io.rippledown.examples.contact_lenses.Age.pre_presbyopic
import io.rippledown.examples.contact_lenses.Astigmatism.astigmatic
import io.rippledown.examples.contact_lenses.Astigmatism.not_astigmatic
import io.rippledown.examples.contact_lenses.Prescription.hypermetrope
import io.rippledown.examples.contact_lenses.Prescription.myope
import io.rippledown.examples.contact_lenses.TearProduction.normal
import io.rippledown.integration.RestClientAttributeFactory
import io.rippledown.integration.RestClientRuleBuilder
import io.rippledown.integration.restclient.RESTClient

@Suppress("unused") // Used in cucumber file.
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
    private val prescription = attributeFactory.create("prescription")
    private val astigmatism = attributeFactory.create("astigmatism")
    private val tearProduction = attributeFactory.create("tear production")
    private val isPrePresbyopic = conditionFactory.getOrCreate(isCondition(age, pre_presbyopic.name))
    private val isPresbyopic = conditionFactory.getOrCreate(isCondition(age, Age.presbyopic.name))
    private val isNotAstigmatic = conditionFactory.getOrCreate(isCondition(astigmatism, not_astigmatic.name))
    private val isAstigmatic = conditionFactory.getOrCreate(isCondition(astigmatism, astigmatic.name))
    private val isNormalTearProduction = conditionFactory.getOrCreate(isCondition(tearProduction, normal.name))
    private val isMyope = conditionFactory.getOrCreate(isCondition(prescription, myope.name))
    private val isHypermetrope = conditionFactory.getOrCreate(isCondition(prescription, hypermetrope.name))
    private val soft = "soft"
    private val hard = "hard"

    fun buildRules() {
        addCommentForCase("Case2", soft, isNotAstigmatic, isNormalTearProduction)
        addCommentForCase("Case4", hard, isAstigmatic, isNormalTearProduction)
        removeCommentForCase("Case16", hard, isPrePresbyopic, isHypermetrope)
        removeCommentForCase("Case18", soft, isPresbyopic, isMyope)
        removeCommentForCase("Case24", hard, isPresbyopic, isHypermetrope)
    }
}
