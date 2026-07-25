package io.rippledown.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.*
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
                "ruleConditions": [],
                "derivedValueChange": null
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

    @Test
    fun `should default ruleConditions to empty list`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus()

        //Then
        cornerstoneStatus.ruleConditions shouldBe emptyList()
    }

    @Test
    fun `should serialize and deserialize with a single rule condition`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            ruleConditions = listOf("Sun is in case")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.ruleConditions shouldBe listOf("Sun is in case")
    }

    @Test
    fun `should serialize and deserialize with multiple rule conditions`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            ruleConditions = listOf("Sun is in case", "Wave is in case", "UV > 5.0")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.ruleConditions shouldBe listOf("Sun is in case", "Wave is in case", "UV > 5.0")
    }

    @Test
    fun `should serialize and deserialize with rule conditions and Addition diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            diff = Addition("Go to Bondi."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.diff shouldBe Addition("Go to Bondi.")
        deserialized.ruleConditions shouldBe listOf("Sun is in case", "Wave is in case")
    }

    @Test
    fun `should serialize and deserialize with rule conditions and Removal diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            diff = Removal("Go to Bondi."),
            ruleConditions = listOf("UV > 5.0")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.diff shouldBe Removal("Go to Bondi.")
        deserialized.ruleConditions shouldBe listOf("UV > 5.0")
    }

    @Test
    fun `should serialize and deserialize with rule conditions and Replacement diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            diff = Replacement("Go to Bondi.", "Go to Maroubra."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.diff shouldBe Replacement("Go to Bondi.", "Go to Maroubra.")
        deserialized.ruleConditions shouldBe listOf("Sun is in case", "Wave is in case")
    }

    @Test
    fun `should serialize and deserialize with rule conditions and a cornerstone case`() {
        //Given
        val rdrCase = createCase("Case1")
        val viewableCase = ViewableCase(rdrCase, caseViewProperties())
        val cornerstoneStatus = CornerstoneStatus(
            cornerstoneToReview = viewableCase,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 1,
            diff = Addition("Go to Bondi."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.ruleConditions shouldBe listOf("Sun is in case", "Wave is in case")
    }

    @Test
    fun checkJsonWithRuleConditions() {
        val cornerstoneStatus = CornerstoneStatus(
            diff = Addition("Go to Bondi."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )

        cornerstoneStatus.toJsonString() shouldBe """
            {
                "cornerstoneToReview": null,
                "indexOfCornerstoneToReview": -1,
                "numberOfCornerstones": 0,
                "diff": {
                    "type": "io.rippledown.model.diff.Addition",
                    "addedText": "Go to Bondi."
                },
                "ruleConditions": [
                    "Sun is in case",
                    "Wave is in case"
                ],
                "derivedValueChange": null
            }
        """.trimIndent()
    }

    @Test
    fun `should be thread safe with rule conditions`() {
        val cornerstoneStatus = CornerstoneStatus(
            diff = Addition("Go to Bondi."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )
        checkSerializationIsThreadSafe(cornerstoneStatus)
    }

    @Test
    fun `should default derivedValueChange to null`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus()

        //Then
        cornerstoneStatus.derivedValueChange shouldBe null
    }

    @Test
    fun `should serialize and deserialize with a derived value addition`() {
        //Given
        val change = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
        val cornerstoneStatus = CornerstoneStatus(derivedValueChange = change)

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.derivedValueChange shouldBe change
    }

    @Test
    fun `should serialize and deserialize with a derived value removal`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(derivedValueChange = DerivedValueRemoval("BMI"))

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.derivedValueChange shouldBe DerivedValueRemoval("BMI")
    }

    @Test
    fun `should serialize and deserialize with a derived value replacement`() {
        //Given
        val change = DerivedValueReplacement("BMI", "15.47", "weight / height ^ 3")
        val cornerstoneStatus = CornerstoneStatus(derivedValueChange = change)

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.derivedValueChange shouldBe change
    }

    @Test
    fun `should serialize and deserialize a derived value change with a cornerstone and conditions`() {
        //Given
        val rdrCase = createCase("Case1")
        val viewableCase = ViewableCase(rdrCase, caseViewProperties())
        val cornerstoneStatus = CornerstoneStatus(
            cornerstoneToReview = viewableCase,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 1,
            ruleConditions = listOf("Weight is high"),
            derivedValueChange = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
    }

    @Test
    fun `a derived value change and a comment diff are independent`() {
        //Given a status carrying only a derived value change
        val cornerstoneStatus = CornerstoneStatus(
            derivedValueChange = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
        )

        //Then the comment diff is unaffected, so the Comments panel previews nothing
        cornerstoneStatus.diff shouldBe null
        serializeDeserialize(cornerstoneStatus).diff shouldBe null
    }

    @Test
    fun `should be thread safe with a derived value change`() {
        val cornerstoneStatus = CornerstoneStatus(
            derivedValueChange = DerivedValueAddition("BMI", "30.93", "weight / height ^ 2")
        )
        checkSerializationIsThreadSafe(cornerstoneStatus)
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