package io.rippledown.kb.sample.cancergenes

import io.rippledown.kb.sample.SampleRuleBuilder
import io.rippledown.server.KBEndpoint

const val CANCER_GENES_DESCRIPTION =
    """
        # Cancer gene variant classification
        This KB is a simple example of the classification of genetic variants
        in some human genes.
        
        It is in no way a reliable source of information and should not
        be used for medical diagnosis or treatment.
    """

const val CANCER_GENES_CASES_DESCRIPTION =
    """
        # Cancer gene variant classification - cases only
        This KB contains cases that could be used as examples for building
        a simple gene variant classifier.
        
        It is in no way a reliable source of information and should not
        be used for medical diagnosis or treatment.
    """

class CancerGenesSampleBuilder(private val kbe: KBEndpoint) {
    fun buildCasesOnlyKb() {
        kbe.setDescription(CANCER_GENES_CASES_DESCRIPTION)
        val genesCases = GenesCases(kbe.kb.attributeManager)
        genesCases.setAttributeOrder(kbe)
        genesCases.cases().forEach { kbe.kb.addProcessedCase(it) }
    }

    fun buildKb() {
        buildCasesOnlyKb()
        kbe.setDescription(CANCER_GENES_DESCRIPTION)
        CancerGenesRulesBuilder(kbe).buildRules()
    }
}
class CancerGenesRulesBuilder(kbe: KBEndpoint) : SampleRuleBuilder(kbe) {
    val gc = GenesCases(kbe.kb.attributeManager)
    val gene = gc.gene
    val driverInterp = gc.driverInterp

    fun buildRules() {

        val alkActivating = "activating mutation, possible indication for ALK inhibitors"


        // ALK	ACTIVATING_MUTATION	ONLY_HIGH	activating mutation, possible indication for ALK inhibitors.
        addCommentForCase("ALK p.Lys1525del 1", alkActivating,geneIs("ALK"), driverInterpIs("HIGH") )
    }

    private fun geneIs(name: String) = kbe.getOrCreateCondition(isCondition(gene, name))
    private fun driverInterpIs(value: String) = kbe.getOrCreateCondition(isCondition(driverInterp, value))
}
