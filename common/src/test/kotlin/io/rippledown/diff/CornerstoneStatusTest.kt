package io.rippledown.diff

import io.kotest.matchers.shouldBe
import io.rippledown.fromJsonString
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
                "pendingChange": null,
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
            pendingChange = Addition("Go to Bondi.", "C1", attributeId = 17)
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.commentDiff shouldBe Addition("Go to Bondi.", "C1", attributeId = 17)
    }

    @Test
    fun `should serialize and deserialize with a Removal diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            pendingChange = Removal("Go to Bondi.")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.commentDiff shouldBe Removal("Go to Bondi.")
    }

    @Test
    fun `should serialize and deserialize with a Replacement diff`() {
        //Given
        val replacement = Replacement(
            originalText = "Go to Bondi.",
            replacementText = "Go to Maroubra.",
            attributeName = "C2",
            replacedAttributeName = "C1"
        )
        val cornerstoneStatus = CornerstoneStatus(
            pendingChange = replacement
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.commentDiff shouldBe replacement
    }

    @Test
    fun `a Replacement from an older client defaults the replaced attribute name`() {
        // Given JSON written before Replacement carried the name of the attribute going out
        val legacyJson = """
            {
                "cornerstoneToReview": null,
                "indexOfCornerstoneToReview": -1,
                "numberOfCornerstones": 0,
                "pendingChange": {
                    "type": "io.rippledown.model.diff.Replacement",
                    "originalText": "Go to Bondi.",
                    "replacementText": "Go to Maroubra.",
                    "attributeName": "C2"
                },
                "ruleConditions": []
            }
        """.trimIndent()

        // When it is read by the current model
        val status = legacyJson.fromJsonString<CornerstoneStatus>()

        // Then the missing name takes its backward-compatible default
        status.commentDiff shouldBe Replacement("Go to Bondi.", "Go to Maroubra.", "C2")
    }

    @Test
    fun `should default pendingChange to null`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus()

        //Then
        cornerstoneStatus.pendingChange shouldBe null
        cornerstoneStatus.commentDiff shouldBe null
        cornerstoneStatus.derivedValueDiff shouldBe null
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
            pendingChange = Addition("Go to Bondi."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.commentDiff shouldBe Addition("Go to Bondi.")
        deserialized.ruleConditions shouldBe listOf("Sun is in case", "Wave is in case")
    }

    @Test
    fun `should serialize and deserialize with rule conditions and Removal diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            pendingChange = Removal("Go to Bondi."),
            ruleConditions = listOf("UV > 5.0")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.commentDiff shouldBe Removal("Go to Bondi.")
        deserialized.ruleConditions shouldBe listOf("UV > 5.0")
    }

    @Test
    fun `should serialize and deserialize with rule conditions and Replacement diff`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(
            pendingChange = Replacement("Go to Bondi.", "Go to Maroubra."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.commentDiff shouldBe Replacement("Go to Bondi.", "Go to Maroubra.")
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
            pendingChange = Addition("Go to Bondi."),
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
            pendingChange = Addition("Go to Bondi.", "C1"),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )

        cornerstoneStatus.toJsonString() shouldBe """
            {
                "cornerstoneToReview": null,
                "indexOfCornerstoneToReview": -1,
                "numberOfCornerstones": 0,
                "pendingChange": {
                    "type": "io.rippledown.model.diff.Addition",
                    "addedText": "Go to Bondi.",
                    "attributeName": "C1",
                    "attributeId": null
                },
                "ruleConditions": [
                    "Sun is in case",
                    "Wave is in case"
                ]
            }
        """.trimIndent()
    }

    @Test
    fun `should be thread safe with rule conditions`() {
        val cornerstoneStatus = CornerstoneStatus(
            pendingChange = Addition("Go to Bondi."),
            ruleConditions = listOf("Sun is in case", "Wave is in case")
        )
        checkSerializationIsThreadSafe(cornerstoneStatus)
    }

    @Test
    fun `should default derivedValueDiff to null`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus()

        //Then
        cornerstoneStatus.derivedValueDiff shouldBe null
    }

    @Test
    fun `should serialize and deserialize with a derived value addition`() {
        //Given
        val change = DerivedValueAddition("BMI", "weight / height ^ 2")
        val cornerstoneStatus = CornerstoneStatus(pendingChange = change)

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.derivedValueDiff shouldBe change
    }

    @Test
    fun `should serialize and deserialize with a derived value removal`() {
        //Given
        val cornerstoneStatus = CornerstoneStatus(pendingChange = DerivedValueRemoval("BMI"))

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.derivedValueDiff shouldBe DerivedValueRemoval("BMI")
    }

    @Test
    fun `should serialize and deserialize with a derived value replacement`() {
        //Given
        val change = DerivedValueReplacement("BMI", "weight / height ^ 3")
        val cornerstoneStatus = CornerstoneStatus(pendingChange = change)

        //When
        val deserialized = serializeDeserialize(cornerstoneStatus)

        //Then
        deserialized shouldBe cornerstoneStatus
        deserialized.derivedValueDiff shouldBe change
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
            pendingChange = DerivedValueAddition("BMI", "weight / height ^ 2")
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
            pendingChange = DerivedValueAddition("BMI", "weight / height ^ 2")
        )

        //Then the comment view of it is null, so the Comments panel previews nothing
        cornerstoneStatus.commentDiff shouldBe null
        serializeDeserialize(cornerstoneStatus).commentDiff shouldBe null
    }

    @Test
    fun `should be thread safe with a derived value change`() {
        val cornerstoneStatus = CornerstoneStatus(
            pendingChange = DerivedValueAddition("BMI", "weight / height ^ 2")
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
