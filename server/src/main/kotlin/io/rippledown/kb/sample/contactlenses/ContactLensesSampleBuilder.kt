package io.rippledown.kb.sample.contactlenses

import io.rippledown.kb.sample.SampleRuleBuilder
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.AgeName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.AstigmatismName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.PrescriptionName
import io.rippledown.kb.sample.contactlenses.ContactLensesRulesBuilder.Companion.TearProductionName
import io.rippledown.server.KBEndpoint

const val CONTACT_LENSES_DESCRIPTION = """
    # Contact Lenses KB
    This is a KB described in the course notes for a course in 
    Machine Learning and RDR at The University of New South Wales.
    See https://www.cse.unsw.edu.au/~cs9416/06s1/lectures/rdr/rdr_trace.html
    
    Used with permission.
    
    This is obviously for demonstration purposes only, and not to be used for advice or diagnosis.
    No liability of any kind is accepted.
"""

const val CONTACT_LENSES_CASES_DESCRIPTION = """
    # Contact Lenses KB
    This contains cases from a KB described in the course notes for a course in 
    Machine Learning and RDR at The University of New South Wales.
    See https://www.cse.unsw.edu.au/~cs9416/06s1/lectures/rdr/rdr_trace.html
    
    Used with permission.
    
    This is obviously for demonstration purposes only, and not to be used for advice or diagnosis.
    No liability of any kind is accepted.
"""


class ContactLensesSampleBuilder(private val kbe: KBEndpoint) {

    fun buildRules() {
        setupCases()
        kbe.setDescription(CONTACT_LENSES_DESCRIPTION)
        ContactLensesRulesBuilder(kbe).buildRules()
    }

    fun setupCases() {
        kbe.setDescription(CONTACT_LENSES_CASES_DESCRIPTION)
        createAttributes()
        Cases(kbe.kb.attributeManager).allCases.forEach {
            kbe.kb.addProcessedCase(it)
        }
    }

    private fun createAttributes() {
        // We create the attributes ahead of time and set their order
        // so that the order in the case view is well-defined.
        val attributeNamesInOrder = listOf(AgeName, PrescriptionName, AstigmatismName, TearProductionName)
        val attributesInOrder = attributeNamesInOrder.map {
            kbe.getOrCreateAttribute(it)
        }
        kbe.setAttributeOrder(attributesInOrder)
    }
}
internal class ContactLensesRulesBuilder(kbe: KBEndpoint) : SampleRuleBuilder(kbe) {
    private val age = kbe.getOrCreateAttribute(AgeName)
    private val prescription = kbe.getOrCreateAttribute(PrescriptionName)
    private val astigmatism = kbe.getOrCreateAttribute(AstigmatismName)
    private val tearProduction = kbe.getOrCreateAttribute(TearProductionName)
    private val isPrePresbyopic = kbe.getOrCreateCondition(isCondition(age, Age.pre_presbyopic.name))
    private val isPresbyopic = kbe.getOrCreateCondition(isCondition(age, Age.presbyopic.name))
    private val isNotAstigmatic = kbe.getOrCreateCondition(isCondition(astigmatism, Astigmatism.not_astigmatic.name))
    private val isAstigmatic = kbe.getOrCreateCondition(isCondition(astigmatism, Astigmatism.astigmatic.name))
    private val isNormalTearProduction = kbe.getOrCreateCondition(isCondition(tearProduction, TearProduction.normal.name))
    private val isMyope = kbe.getOrCreateCondition(isCondition(prescription, Prescription.myope.name))
    private val isHypermetrope = kbe.getOrCreateCondition(isCondition(prescription, Prescription.hypermetrope.name))
    private val soft = "soft"
    private val hard = "hard"

    fun buildRules() {
        addCommentForCase("Case2", soft, isNotAstigmatic, isNormalTearProduction)
        addCommentForCase("Case4", hard, isAstigmatic, isNormalTearProduction)
        removeCommentForCase("Case16", hard, isPrePresbyopic, isHypermetrope)
        removeCommentForCase("Case18", soft, isPresbyopic, isMyope)
        removeCommentForCase("Case24", hard, isPresbyopic, isHypermetrope)
    }

    companion object {
        const val AgeName = "age"
        const val PrescriptionName = "prescription"
        val AstigmatismName = "astigmatism"
        val TearProductionName = "tear production"
    }
}