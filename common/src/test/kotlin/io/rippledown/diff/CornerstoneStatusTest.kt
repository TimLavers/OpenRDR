package io.rippledown.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import io.rippledown.utils.checkSerializationIsThreadSafe
import io.rippledown.utils.defaultDate
import io.rippledown.utils.serializeDeserialize
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
        val deserialized = serializeDeserialize(cornerstoneStatus)
        deserialized shouldBe cornerstoneStatus

        checkSerializationIsThreadSafe(cornerstoneStatus)
    }

    @Test
    fun checkJson() {
        val cornerstoneStatus = CornerstoneStatus()

        cornerstoneStatus.toJsonString() shouldBe """
            {
                "cornerstoneToReview": null,
                "indexOfCornerstoneToReview": -1,
                "numberOfCornerstones": 0,
                "diff": null,
                "ruleConditions": []
            }
        """.trimIndent()
    }

    @Test
    fun `should serialize and deserialize with an Addition diff`() {
        //Given
        val rdrCase = createCase("Case1")
        val viewableCase = ViewableCase(rdrCase, caseViewProperties())
        val cornerstoneStatus = CornerstoneStatus(
            cornerstoneToReview = viewableCase,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 1,
            diff = Addition("Go to Bondi.")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.diff shouldBe Addition("Go to Bondi.")
    }

    @Test
    fun `should serialize and deserialize with a Removal diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            diff = Removal("Go to Bondi.")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.diff shouldBe Removal("Go to Bondi.")
    }

    @Test
    fun `should serialize and deserialize with a Replacement diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            diff = Replacement("Go to Bondi.", "Go to Maroubra.")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.diff shouldBe Replacement("Go to Bondi.", "Go to Maroubra.")
    }

    @Test
    fun `should default diff to null`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus()

        //Then
        cornerstoneStatus.diff shouldBe null
    }

    private fun caseViewProperties() = CaseViewProperties(listOf(abc, tsh, xyz))

    private fun createCase(name: String): RDRCase {

        with(RDRCaseBuilder()) {
            addValue(tsh, defaultDate, "0.68")
            addValue(xyz, defaultDate, "0.66")
            addValue(abc, defaultDate, "0.67")
            return build(name)
        }
    }
}