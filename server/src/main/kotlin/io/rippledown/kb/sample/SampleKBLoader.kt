package io.rippledown.kb.sample

import io.rippledown.kb.sample.contactlenses.ContactLensesSampleBuilder
import io.rippledown.kb.sample.vltsh.TSHSampleBuilder
import io.rippledown.sample.SampleKB
import io.rippledown.sample.SampleKB.*
import io.rippledown.server.KBEndpoint

const val defaultDate = 1659752689505

fun loadSampleKB(kbEndpoint: KBEndpoint, sampleKB: SampleKB) {
    require(kbEndpoint.kb.ruleTree.size() == 1L) {
        "Cannot load a sample into a KB that already has rules."
    }
    when (sampleKB) {
        TSH -> TSHSampleBuilder(kbEndpoint).buildTSHRules()
        TSH_CASES -> TSHSampleBuilder(kbEndpoint).setupTSHSampleCases()
        CONTACT_LENSES -> ContactLensesSampleBuilder(kbEndpoint).buildRules()
        CONTACT_LENSES_CASES -> ContactLensesSampleBuilder(kbEndpoint).setupCases()
    }
}