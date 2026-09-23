package io.rippledown.kb.sample.cancergenes

import io.kotest.matchers.shouldBe
import io.rippledown.kb.sample.SampleBuilderTest
import io.rippledown.model.AttributeKind
import kotlin.test.Test

class CancerGenesSampleBuilderTest: SampleBuilderTest() {
    @Test
    fun `test cases kb`() {
        CancerGenesSampleBuilder(endpoint).buildCasesOnlyKb()
        checkAttributes()

    }

    private fun checkAttributes() {
        val attributesInOrder = endpoint.kb.caseViewManager.allInOrder()
            .filter { it.kind == AttributeKind.EXTERNAL }.map { it.name }
        attributesInOrder shouldBe listOf(
            "gene",
            "driverRole",
            "driverInterp",
            "hotspot",
            "biallelic",
            "effects",
            "codingEffect",
            "proteinImpact",
            "codingImpact",
            "codon",
            "hrdStatus",
            "isHrdDrivingGene"
        )

    }
}