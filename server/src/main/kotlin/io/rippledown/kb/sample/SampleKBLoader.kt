package io.rippledown.kb.sample

import io.rippledown.kb.sample.vltsh.TSHSampleBuilder
import io.rippledown.sample.SampleKB
import io.rippledown.server.KBEndpoint

fun loadSampleKB(kbEndpoint: KBEndpoint, sampleKB: SampleKB) {
    require(kbEndpoint.kb.ruleTree.size() == 1L) {
        "Cannot load a sample into a KB that already has rules."
    }
    when (sampleKB) {
        SampleKB.TSH -> TSHSampleBuilder(kbEndpoint).buildTSHRules()
        SampleKB.TSH_CASES -> TSHSampleBuilder(kbEndpoint).setupTSHSampleCases()
    }
}