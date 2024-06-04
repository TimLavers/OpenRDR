package io.rippledown.kb.sample

import io.rippledown.kb.sample.vltsh.TSHSampleBuilder
import io.rippledown.sample.SampleKB
import io.rippledown.server.KBEndpoint

fun loadSampleKB(kbEndpoint: KBEndpoint, sampleKB: SampleKB) {
    when (sampleKB) {
        SampleKB.TSH -> TSHSampleBuilder(kbEndpoint).buildTSHRules()
        SampleKB.TSH_CASES -> TSHSampleBuilder(kbEndpoint).setupTSHSampleCases()
    }
}