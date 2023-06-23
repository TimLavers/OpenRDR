package io.rippledown.model.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.defaultDate
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class CornerstoneStatusTest {
    val abc = Attribute(1, "ABC")
    val tsh = Attribute(2, "TSH")
    val xyz = Attribute(3, "XYZ")

    @Test
    fun checkSerialization() {
        val rdrCase = createCase("Case1")
        val viewableCase = ViewableCase(rdrCase, caseViewProperties())
        val cornerstoneStatus = CornerstoneStatus(
            cornerstoneToReview = viewableCase,
            indexOfCornerstoneToReview = 42,
            numberOfCornerstones = 99
        )
        val json = Json { allowStructuredMapKeys = true }
        val serialized = json.encodeToString(cornerstoneStatus)
        val deserialized = json.decodeFromString<CornerstoneStatus>(serialized)
        deserialized shouldBe cornerstoneStatus
    }

    private fun caseViewProperties() = CaseViewProperties(listOf(abc, tsh, xyz))

    private fun createCase(name: String): RDRCase {

        with( RDRCaseBuilder() ) {
            addValue(tsh, defaultDate, "0.68")
            addValue(xyz, defaultDate, "0.66")
            addValue(abc, defaultDate, "0.67")
            return build(name)
        }
    }
}